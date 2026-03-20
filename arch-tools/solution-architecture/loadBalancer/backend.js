const http = require("http");

const port = Number(process.argv[2] || 3001);
const name = process.argv[3] || `server-${port}`;
const maxRps = Number(process.env.BACKEND_RPS || 100);

let windowStart = Date.now();
let windowCount = 0;

function rateLimitOk() {
  const now = Date.now();
  if (now - windowStart >= 1000) {
    windowStart = now;
    windowCount = 0;
  }
  if (windowCount >= maxRps) return false;
  windowCount += 1;
  return true;
}

const server = http.createServer((req, res) => {
  if (!rateLimitOk()) {
    res.statusCode = 429;
    res.setHeader("Content-Type", "text/plain");
    res.setHeader("Retry-After", "1");
    res.end("Backend rate limit exceeded");
    return;
  }

  const body = JSON.stringify({
    server: name,
    port,
    method: req.method,
    url: req.url,
    time: new Date().toISOString(),
  });
  res.writeHead(200, {
    "Content-Type": "application/json",
    "Content-Length": Buffer.byteLength(body),
  });
  res.end(body);
});

server.listen(port, () => {
  console.log(`[backend] ${name} listening on ${port} (limit ${maxRps} req/s)`);
});
