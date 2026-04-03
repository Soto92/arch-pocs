const http = require("http");
const { URL } = require("url");

const listenPort = Number(process.env.GW_PORT || 4000);
const upstream = new URL(process.env.GW_UPSTREAM || "http://localhost:3000");
const apiKey = process.env.API_KEY || "demo-key";

function isAuthorized(req) {
  const key = req.headers["x-api-key"];
  return key && key === apiKey;
}

const server = http.createServer((req, res) => {
  if (!req.url.startsWith("/api/")) {
    res.statusCode = 404;
    res.setHeader("Content-Type", "text/plain");
    res.end("Not found");
    return;
  }

  if (!isAuthorized(req)) {
    res.statusCode = 401;
    res.setHeader("Content-Type", "text/plain");
    res.end("Unauthorized");
    return;
  }

  const options = {
    protocol: upstream.protocol,
    hostname: upstream.hostname,
    port: upstream.port,
    method: req.method,
    path: req.url.replace("/api", ""),
    headers: {
      ...req.headers,
      host: upstream.host,
      "x-forwarded-host": req.headers.host || "",
      "x-forwarded-proto": "http",
    },
  };

  const proxyReq = http.request(options, (proxyRes) => {
    res.writeHead(proxyRes.statusCode || 502, proxyRes.headers);
    proxyRes.pipe(res);
  });

  proxyReq.on("error", (err) => {
    res.statusCode = 502;
    res.setHeader("Content-Type", "text/plain");
    res.end(`Upstream error: ${err.message}`);
  });

  req.pipe(proxyReq);
});

server.listen(listenPort, () => {
  console.log(`[gateway] listening on ${listenPort}`);
  console.log(`[gateway] upstream: ${upstream.href}`);
  console.log(`[gateway] api key header: X-API-Key`);
});