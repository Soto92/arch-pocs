const http = require("http");
const https = require("https");

const listenPort = Number(process.env.CDN_PORT || 4000);
const origin = new URL(process.env.ORIGIN_URL || "http://localhost:3000");
const defaultTtlSeconds = Number(process.env.CDN_TTL_SECONDS || 10);

function pickTtlSeconds(cacheControlHeader) {
  if (!cacheControlHeader) return defaultTtlSeconds;
  const cacheControl = String(cacheControlHeader).toLowerCase();
  if (cacheControl.includes("no-store")) return 0;

  const maxAgeMatch = cacheControl.match(/(?:^|,)\\s*s-maxage=(\\d+)/);
  if (maxAgeMatch) return Number(maxAgeMatch[1]);

  const fallbackMatch = cacheControl.match(/(?:^|,)\\s*max-age=(\\d+)/);
  if (fallbackMatch) return Number(fallbackMatch[1]);

  return defaultTtlSeconds;
}

function shouldCache(req, statusCode, cacheControlHeader) {
  if (req.method !== "GET" && req.method !== "HEAD") return false;
  if ((statusCode || 0) !== 200) return false;
  if (pickTtlSeconds(cacheControlHeader) <= 0) return false;
  return true;
}

const cache = new Map();

function cacheKey(req) {
  return `${req.method} ${req.url}`;
}

function nowMs() {
  return Date.now();
}

function proxyRequest(req, res) {
  const upstreamModule = origin.protocol === "https:" ? https : http;
  const options = {
    protocol: origin.protocol,
    hostname: origin.hostname,
    port: origin.port,
    method: req.method,
    path: req.url,
    headers: {
      ...req.headers,
      host: origin.host,
      "x-forwarded-host": req.headers.host || "",
      "x-forwarded-proto": "http",
    },
  };

  const upstreamReq = upstreamModule.request(options, (upstreamRes) => {
    const chunks = [];

    res.statusCode = upstreamRes.statusCode || 502;
    for (const [k, v] of Object.entries(upstreamRes.headers)) {
      if (typeof v !== "undefined") res.setHeader(k, v);
    }
    res.setHeader("X-Cache", "Miss from CDN");

    upstreamRes.on("data", (chunk) => {
      chunks.push(chunk);
      res.write(chunk);
    });

    upstreamRes.on("end", () => {
      res.end();

      const body = Buffer.concat(chunks);
      const cacheControl = upstreamRes.headers["cache-control"];

      if (shouldCache(req, res.statusCode, cacheControl)) {
        const ttlSeconds = pickTtlSeconds(cacheControl);
        cache.set(cacheKey(req), {
          expiresAtMs: nowMs() + ttlSeconds * 1000,
          statusCode: res.statusCode,
          headers: upstreamRes.headers,
          body,
        });
      }
    });
  });

  upstreamReq.on("error", (err) => {
    res.statusCode = 502;
    res.setHeader("Content-Type", "text/plain; charset=utf-8");
    res.setHeader("X-Cache", "Error from CDN");
    res.end(`Origin error: ${err.message}`);
  });

  req.pipe(upstreamReq);
}

const server = http.createServer((req, res) => {
  const key = cacheKey(req);
  const entry = cache.get(key);

  if (entry && entry.expiresAtMs > nowMs()) {
    res.statusCode = entry.statusCode;
    for (const [k, v] of Object.entries(entry.headers)) {
      if (typeof v !== "undefined") res.setHeader(k, v);
    }
    res.setHeader("X-Cache", "Hit from CDN");

    if (req.method === "HEAD") {
      res.end();
      return;
    }

    res.end(entry.body);
    return;
  }

  if (entry) cache.delete(key);
  proxyRequest(req, res);
});

server.listen(listenPort, () => {
  console.log(`[cdn] listening on ${listenPort}`);
  console.log(`[cdn] origin: ${origin.href}`);
  console.log(`[cdn] default ttl seconds: ${defaultTtlSeconds}`);
});
