const http = require("http");
const { URL } = require("url");

const port = Number(process.env.PORT || 8080);

const legacyBackend = new URL(
  process.env.LEGACY_BACKEND_URL || "http://localhost:3001",
);
const newBackend = new URL(
  process.env.NEW_BACKEND_URL || "http://localhost:3002",
);

const migrationFlag = String(process.env.USE_NEW_BACKEND || "false") === "true";
const rolloutPercentage = Number(process.env.NEW_BACKEND_ROLLOUT || 0);
const allowHeaderOverride =
  String(process.env.ALLOW_BACKEND_OVERRIDE || "true") === "true";

let requestSequence = 0;

function hashValue(value) {
  let hash = 0;

  for (let i = 0; i < value.length; i += 1) {
    hash = (hash * 31 + value.charCodeAt(i)) >>> 0;
  }

  return hash;
}

function parseCookies(cookieHeader = "") {
  return cookieHeader
    .split(";")
    .map((item) => item.trim())
    .filter(Boolean)
    .reduce((cookies, item) => {
      const separatorIndex = item.indexOf("=");

      if (separatorIndex === -1) {
        return cookies;
      }

      const key = item.slice(0, separatorIndex);
      const value = item.slice(separatorIndex + 1);

      return { ...cookies, [key]: decodeURIComponent(value) };
    }, {});
}

function shouldUseNewBackend(req) {
  const url = new URL(req.url, `http://${req.headers.host || "localhost"}`);
  const cookies = parseCookies(req.headers.cookie);
  const subject =
    req.headers["x-user-id"] ||
    cookies.userId ||
    url.searchParams.get("userId") ||
    req.socket.remoteAddress ||
    "anonymous";

  if (allowHeaderOverride) {
    const requestedBackend = req.headers["x-backend-target"] || cookies.backend;

    if (requestedBackend === "new") {
      return { useNewBackend: true, reason: "explicit override" };
    }

    if (requestedBackend === "legacy") {
      return { useNewBackend: false, reason: "explicit override" };
    }
  }

  if (migrationFlag) {
    return { useNewBackend: true, reason: "feature flag" };
  }

  if (rolloutPercentage > 0) {
    const bucket = hashValue(String(subject)) % 100;

    if (bucket < rolloutPercentage) {
      return {
        useNewBackend: true,
        reason: `percentage rollout ${rolloutPercentage}%`,
      };
    }
  }

  return { useNewBackend: false, reason: "default legacy route" };
}

function proxyRequest(req, res, target, routeDecision, requestId) {
  const options = {
    protocol: target.protocol,
    hostname: target.hostname,
    port: target.port,
    method: req.method,
    path: req.url,
    headers: {
      ...req.headers,
      host: target.host,
      "x-request-id": String(requestId),
      "x-migration-target": routeDecision.useNewBackend ? "new" : "legacy",
      "x-migration-reason": routeDecision.reason,
    },
  };

  const proxyReq = http.request(options, (proxyRes) => {
    res.setHeader("x-request-id", String(requestId));
    res.setHeader(
      "x-migration-target",
      routeDecision.useNewBackend ? "new" : "legacy",
    );
    res.setHeader("x-migration-reason", routeDecision.reason);
    res.writeHead(proxyRes.statusCode || 502, proxyRes.headers);
    proxyRes.pipe(res);
  });

  proxyReq.on("error", (err) => {
    res.statusCode = 502;
    res.setHeader("content-type", "application/json");
    res.end(
      JSON.stringify(
        {
          error: "Backend proxy failed",
          target: target.href,
          message: err.message,
        },
        null,
        2,
      ),
    );
  });

  req.pipe(proxyReq);
}

const server = http.createServer((req, res) => {
  requestSequence += 1;

  const routeDecision = shouldUseNewBackend(req);
  const target = routeDecision.useNewBackend ? newBackend : legacyBackend;

  console.log(
    `[middleware] ${requestSequence} ${req.method} ${req.url} -> ${
      target.href
    } (${routeDecision.reason})`,
  );

  proxyRequest(req, res, target, routeDecision, requestSequence);
});

server.listen(port, () => {
  console.log(`[middleware] listening on ${port}`);
  console.log(`[middleware] legacy backend: ${legacyBackend.href}`);
  console.log(`[middleware] new backend: ${newBackend.href}`);
  console.log(`[middleware] USE_NEW_BACKEND=${migrationFlag}`);
  console.log(`[middleware] NEW_BACKEND_ROLLOUT=${rolloutPercentage}`);
});
