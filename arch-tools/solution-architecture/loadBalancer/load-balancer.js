const http = require("http");
const { URL } = require("url");

const listenPort = Number(process.env.LB_PORT || 3000);
const targetsEnv =
  process.env.TARGETS || "http://localhost:3001,http://localhost:3002";
const targets = targetsEnv.split(",").map((t) => new URL(t.trim()));
const verbose = String(process.env.VERBOSE || "1") === "1";

if (targets.length === 0) {
  console.error("No targets configured. Set TARGETS env var.");
  process.exit(1);
}

let rrIndex = 0;
let reqId = 0;

function pickTarget() {
  // round‑robin tehnique here
  const target = targets[rrIndex % targets.length];
  rrIndex += 1;
  return target;
}

const server = http.createServer((req, res) => {
  reqId += 1;
  const id = reqId;

  const target = pickTarget();
  const options = {
    protocol: target.protocol,
    hostname: target.hostname,
    port: target.port,
    method: req.method,
    path: req.url,
    headers: {
      ...req.headers,
      host: target.host,
    },
  };

  const proxyReq = http.request(options, (proxyRes) => {
    res.setHeader("X-Target", target.href);
    res.setHeader("X-Request-Id", String(id));
    res.writeHead(proxyRes.statusCode || 502, proxyRes.headers);
    proxyRes.pipe(res);
  });

  proxyReq.on("error", (err) => {
    res.statusCode = 502;
    res.setHeader("Content-Type", "text/plain");
    res.end(`Upstream error: ${err.message}`);
  });

  if (verbose)
    console.log(`[lb] ${id} ${req.method} ${req.url} -> ${target.href}`);
  req.pipe(proxyReq);
});

server.listen(listenPort, () => {
  console.log(`[lb] listening on ${listenPort}`);
  console.log(`[lb] targets: ${targets.map((t) => t.href).join(", ")}`);
});
