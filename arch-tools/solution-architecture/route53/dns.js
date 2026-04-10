const http = require("http");

const hostedZone = {
  "simple.myapp.com": {
    type: "simple",
    url: "http://localhost:5000",
  },
  "api.myapp.com": {
    type: "failover",
    primary: "http://localhost:3001",
    secondary: "http://localhost:3002",
    primaryHealthy: true, // Simulates Route 53 Health Checks
  },
  "web.myapp.com": {
    type: "weighted",
    targets: [
      { url: "http://localhost:4001", weight: 80 },
      { url: "http://localhost:4002", weight: 20 },
    ],
  },
};

const server = http.createServer((req, res) => {
  const url = new URL(req.url, `http://${req.headers.host}`);

  if (url.pathname === "/resolve") {
    const domain = url.searchParams.get("domain");
    const record = hostedZone[domain];

    if (!record) {
      res.writeHead(404, { "Content-Type": "application/json" });
      res.end(JSON.stringify({ error: "NXDOMAIN: Domain not found" }));
      return;
    }

    let resolvedUrl = null;

    if (record.type === "simple") {
      resolvedUrl = record.url;
    } else if (record.type === "failover") {
      resolvedUrl = record.primaryHealthy ? record.primary : record.secondary;
    } else if (record.type === "weighted") {
      const totalWeight = record.targets.reduce((sum, t) => sum + t.weight, 0);
      let random = Math.random() * totalWeight;
      for (const target of record.targets) {
        if (random < target.weight) {
          resolvedUrl = target.url;
          break;
        }
        random -= target.weight;
      }
    }

    res.writeHead(200, { "Content-Type": "application/json" });
    res.end(JSON.stringify({ domain, resolvedUrl, policy: record.type }));
  } else if (url.pathname === "/toggle-health") {
    const domain = url.searchParams.get("domain");
    if (hostedZone[domain] && hostedZone[domain].type === "failover") {
      hostedZone[domain].primaryHealthy = !hostedZone[domain].primaryHealthy;
      res.writeHead(200, { "Content-Type": "application/json" });
      res.end(
        JSON.stringify({
          domain,
          primaryHealthy: hostedZone[domain].primaryHealthy,
        }),
      );
    } else {
      res.writeHead(400, { "Content-Type": "application/json" });
      res.end(
        JSON.stringify({
          error: "Domain is not configured for failover routing",
        }),
      );
    }
  } else {
    res.writeHead(404);
    res.end();
  }
});

const PORT = process.env.PORT || 5053;
server.listen(PORT, () => {
  console.log(`Route 53 Simulator listening on port ${PORT}`);
  console.log(
    `Try resolving: http://localhost:${PORT}/resolve?domain=api.myapp.com`,
  );
  console.log(
    `Toggle health: http://localhost:${PORT}/toggle-health?domain=api.myapp.com`,
  );
});
