const http = require("http");

const port = Number(process.env.ORIGIN_PORT || 3000);
const delayMs = Number(process.env.ORIGIN_DELAY_MS || 200);

function respondAfterDelay(res, handler) {
  setTimeout(() => handler(res), Math.max(0, delayMs));
}

const server = http.createServer((req, res) => {
  const url = new URL(req.url, `http://${req.headers.host}`);

  if (url.pathname === "/") {
    respondAfterDelay(res, () => {
      res.statusCode = 200;
      res.setHeader("Content-Type", "text/html; charset=utf-8");
      res.setHeader("Cache-Control", "public, max-age=10");
      res.end(
        `<h1>Origin</h1><p>Generated at: ${new Date().toISOString()}</p>`,
      );
    });
    return;
  }

  if (url.pathname === "/static/app.js") {
    respondAfterDelay(res, () => {
      res.statusCode = 200;
      res.setHeader("Content-Type", "application/javascript; charset=utf-8");
      res.setHeader("Cache-Control", "public, max-age=30");
      res.end(`console.log("origin build:", "${new Date().toISOString()}");`);
    });
    return;
  }

  if (url.pathname === "/api/time") {
    respondAfterDelay(res, () => {
      res.statusCode = 200;
      res.setHeader("Content-Type", "application/json; charset=utf-8");
      res.setHeader("Cache-Control", "public, max-age=2");
      res.end(JSON.stringify({ now: new Date().toISOString() }));
    });
    return;
  }

  if (url.pathname === "/nocache") {
    respondAfterDelay(res, () => {
      res.statusCode = 200;
      res.setHeader("Content-Type", "text/plain; charset=utf-8");
      res.setHeader("Cache-Control", "no-store");
      res.end(`nocache: ${new Date().toISOString()}`);
    });
    return;
  }

  respondAfterDelay(res, () => {
    res.statusCode = 404;
    res.setHeader("Content-Type", "text/plain; charset=utf-8");
    res.end("Not found");
  });
});

server.listen(port, () => {
  console.log(`[origin] listening on ${port}`);
  console.log(`[origin] try: http://localhost:${port}/`);
});
