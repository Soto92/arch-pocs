const http = require("http");

const port = Number(process.env.PORT || 7071);
const defaultTtlSeconds = Number(process.env.CACHE_TTL_SECONDS || 20);

const productsDb = new Map([
  ["sku-100", { id: "sku-100", name: "Architecture Notebook", price: 24.9, stock: 42 }],
  ["sku-200", { id: "sku-200", name: "Diagram Marker Set", price: 12.5, stock: 18 }],
  ["sku-300", { id: "sku-300", name: "Latency Sticker Pack", price: 4.99, stock: 120 }],
]);

const cache = new Map();
const stats = {
  hits: 0,
  misses: 0,
  writes: 0,
  invalidations: 0,
};

function nowMs() {
  return Date.now();
}

function clone(value) {
  return JSON.parse(JSON.stringify(value));
}

function cacheKeyForProduct(id) {
  return `products:${id}`;
}

function getFromCache(key) {
  const entry = cache.get(key);

  if (!entry) {
    stats.misses += 1;
    return null;
  }

  if (entry.expiresAtMs <= nowMs()) {
    cache.delete(key);
    stats.misses += 1;
    return null;
  }

  stats.hits += 1;
  return {
    value: clone(entry.value),
    expiresAt: new Date(entry.expiresAtMs).toISOString(),
  };
}

function setInCache(key, value, ttlSeconds = defaultTtlSeconds) {
  const expiresAtMs = nowMs() + ttlSeconds * 1000;

  cache.set(key, {
    value: clone(value),
    expiresAtMs,
  });

  stats.writes += 1;

  return {
    key,
    value: clone(value),
    ttlSeconds,
    expiresAt: new Date(expiresAtMs).toISOString(),
  };
}

function deleteFromCache(key) {
  const deleted = cache.delete(key);

  if (deleted) {
    stats.invalidations += 1;
  }

  return deleted;
}

async function readProductFromDatabase(id) {
  await new Promise((resolve) => setTimeout(resolve, 300));

  const product = productsDb.get(id);
  return product ? clone(product) : null;
}

async function writeProductToDatabase(id, patch) {
  await new Promise((resolve) => setTimeout(resolve, 300));

  const current = productsDb.get(id) || { id };
  const product = {
    ...current,
    ...patch,
    id,
    updatedAt: new Date().toISOString(),
  };

  productsDb.set(id, product);
  return clone(product);
}

async function readProductThroughCache(id) {
  const key = cacheKeyForProduct(id);
  const cached = getFromCache(key);

  if (cached) {
    return {
      source: "cache",
      key,
      product: cached.value,
      expiresAt: cached.expiresAt,
    };
  }

  const product = await readProductFromDatabase(id);

  if (!product) {
    return {
      source: "database",
      key,
      product: null,
    };
  }

  const cachedWrite = setInCache(key, product);

  return {
    source: "database",
    key,
    product,
    expiresAt: cachedWrite.expiresAt,
  };
}

function cacheSnapshot() {
  const entries = [];

  for (const [key, entry] of cache.entries()) {
    if (entry.expiresAtMs <= nowMs()) {
      cache.delete(key);
      continue;
    }

    entries.push({
      key,
      value: clone(entry.value),
      expiresAt: new Date(entry.expiresAtMs).toISOString(),
    });
  }

  const totalReads = stats.hits + stats.misses;

  return {
    entries,
    stats: {
      ...stats,
      hitRatio: totalReads === 0 ? 0 : Number((stats.hits / totalReads).toFixed(3)),
    },
  };
}

function sendJson(res, statusCode, body) {
  res.writeHead(statusCode, { "Content-Type": "application/json" });
  res.end(JSON.stringify(body, null, 2));
}

function readBody(req) {
  return new Promise((resolve, reject) => {
    let body = "";

    req.on("data", (chunk) => {
      body += chunk;
    });

    req.on("end", () => {
      if (!body) {
        resolve({});
        return;
      }

      try {
        resolve(JSON.parse(body));
      } catch (err) {
        reject(err);
      }
    });

    req.on("error", reject);
  });
}

const server = http.createServer(async (req, res) => {
  const url = new URL(req.url, `http://${req.headers.host}`);

  try {
    if (req.method === "GET" && url.pathname === "/health") {
      sendJson(res, 200, { status: "ok" });
      return;
    }

    if (req.method === "GET" && url.pathname === "/cache") {
      sendJson(res, 200, cacheSnapshot());
      return;
    }

    if (req.method === "DELETE" && url.pathname === "/cache") {
      const deleted = cache.size;
      cache.clear();
      stats.invalidations += deleted;
      sendJson(res, 200, { deleted });
      return;
    }

    const cacheMatch = url.pathname.match(/^\/cache\/([^/]+)$/);
    if (cacheMatch) {
      const key = decodeURIComponent(cacheMatch[1]);

      if (req.method === "GET") {
        const entry = getFromCache(key);
        sendJson(res, entry ? 200 : 404, { key, entry });
        return;
      }

      if (req.method === "DELETE") {
        sendJson(res, 200, { key, deleted: deleteFromCache(key) });
        return;
      }
    }

    const productMatch = url.pathname.match(/^\/products\/([^/]+)$/);
    if (productMatch) {
      const id = decodeURIComponent(productMatch[1]);

      if (req.method === "GET") {
        const result = await readProductThroughCache(id);

        if (!result.product) {
          sendJson(res, 404, { error: "Product not found", source: result.source });
          return;
        }

        sendJson(res, 200, {
          ...result,
          ttlSeconds: defaultTtlSeconds,
        });
        return;
      }

      if (req.method === "PUT") {
        const body = await readBody(req);
        const product = await writeProductToDatabase(id, body);
        const key = cacheKeyForProduct(id);
        const invalidated = deleteFromCache(key);

        sendJson(res, 200, {
          source: "database",
          key,
          invalidated,
          product,
        });
        return;
      }
    }

    sendJson(res, 404, { error: "Not found" });
  } catch (err) {
    sendJson(res, 400, { error: err.message });
  }
});

server.listen(port, () => {
  console.log(`[cache-tier] listening on ${port}`);
  console.log(`[cache-tier] default ttl seconds: ${defaultTtlSeconds}`);
  console.log(`[cache-tier] try: http://localhost:${port}/products/sku-100`);
});
