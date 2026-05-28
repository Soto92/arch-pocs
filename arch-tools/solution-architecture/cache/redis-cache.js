const http = require("http");
const net = require("net");

const port = Number(process.env.PORT || 7070);
const redisHost = process.env.REDIS_HOST || "127.0.0.1";
const redisPort = Number(process.env.REDIS_PORT || 6379);
const cacheTtlSeconds = Number(process.env.CACHE_TTL_SECONDS || 30);

const usersDb = {
  "1": { id: "1", name: "Ana Silva", role: "Architect" },
  "2": { id: "2", name: "Bruno Costa", role: "Developer" },
  "3": { id: "3", name: "Carla Mendes", role: "Product Owner" },
};

function encodeRedisCommand(args) {
  const parts = [`*${args.length}\r\n`];

  for (const arg of args) {
    const value = String(arg);
    parts.push(`$${Buffer.byteLength(value)}\r\n${value}\r\n`);
  }

  return parts.join("");
}

function parseRedisReply(buffer) {
  const text = buffer.toString();
  const type = text[0];

  if (type === "+") {
    if (!text.includes("\r\n")) {
      return undefined;
    }

    return text.slice(1, text.indexOf("\r\n"));
  }

  if (type === ":") {
    if (!text.includes("\r\n")) {
      return undefined;
    }

    return Number(text.slice(1, text.indexOf("\r\n")));
  }

  if (type === "$") {
    const firstLineEnd = text.indexOf("\r\n");
    if (firstLineEnd === -1) {
      return undefined;
    }

    const length = Number(text.slice(1, firstLineEnd));

    if (length === -1) {
      return null;
    }

    const start = firstLineEnd + 2;
    if (buffer.length < start + length + 2) {
      return undefined;
    }

    return text.slice(start, start + length);
  }

  if (type === "-") {
    if (!text.includes("\r\n")) {
      return undefined;
    }

    throw new Error(text.slice(1, text.indexOf("\r\n")));
  }

  throw new Error(`Unsupported Redis reply: ${text}`);
}

function redisCommand(...args) {
  return new Promise((resolve, reject) => {
    const socket = net.createConnection({ host: redisHost, port: redisPort });
    const chunks = [];
    let settled = false;

    function finish(err, value) {
      if (settled) {
        return;
      }

      settled = true;
      socket.destroy();

      if (err) {
        reject(err);
        return;
      }

      resolve(value);
    }

    socket.setTimeout(2000);

    socket.on("connect", () => {
      socket.write(encodeRedisCommand(args));
    });

    socket.on("data", (chunk) => {
      chunks.push(chunk);

      try {
        const reply = parseRedisReply(Buffer.concat(chunks));

        if (reply !== undefined) {
          finish(null, reply);
        }
      } catch (err) {
        finish(err);
      }
    });

    socket.on("end", () => {
      try {
        finish(null, parseRedisReply(Buffer.concat(chunks)));
      } catch (err) {
        finish(err);
      }
    });

    socket.on("timeout", () => {
      finish(new Error("Redis request timed out"));
    });

    socket.on("error", finish);
  });
}

async function getCache(key) {
  const value = await redisCommand("GET", key);
  return value ? JSON.parse(value) : null;
}

async function setCache(key, value, ttlSeconds = cacheTtlSeconds) {
  await redisCommand("SETEX", key, ttlSeconds, JSON.stringify(value));
}

async function deleteCache(key) {
  return redisCommand("DEL", key);
}

async function readUserFromDatabase(id) {
  await new Promise((resolve) => setTimeout(resolve, 250));
  return usersDb[id] || null;
}

function sendJson(res, statusCode, body) {
  res.writeHead(statusCode, { "Content-Type": "application/json" });
  res.end(JSON.stringify(body, null, 2));
}

async function readBody(req) {
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

async function getUserWithCache(id) {
  const key = `users:${id}`;
  const cachedUser = await getCache(key);

  if (cachedUser) {
    return { source: "cache", user: cachedUser };
  }

  const user = await readUserFromDatabase(id);

  if (!user) {
    return { source: "database", user: null };
  }

  await setCache(key, user);
  return { source: "database", user };
}

const server = http.createServer(async (req, res) => {
  const url = new URL(req.url, `http://${req.headers.host}`);

  try {
    if (req.method === "GET" && url.pathname === "/health") {
      await redisCommand("PING");
      sendJson(res, 200, { status: "ok", redis: `${redisHost}:${redisPort}` });
      return;
    }

    const userMatch = url.pathname.match(/^\/users\/([^/]+)$/);
    if (req.method === "GET" && userMatch) {
      const result = await getUserWithCache(userMatch[1]);

      if (!result.user) {
        sendJson(res, 404, { error: "User not found", source: result.source });
        return;
      }

      sendJson(res, 200, {
        ...result,
        ttlSeconds: cacheTtlSeconds,
      });
      return;
    }

    const cacheMatch = url.pathname.match(/^\/cache\/([^/]+)$/);
    if (cacheMatch) {
      const key = decodeURIComponent(cacheMatch[1]);

      if (req.method === "GET") {
        sendJson(res, 200, { key, value: await getCache(key) });
        return;
      }

      if (req.method === "PUT") {
        const body = await readBody(req);

        if (!Object.hasOwn(body, "value")) {
          sendJson(res, 400, { error: "Missing value" });
          return;
        }

        await setCache(key, body.value, body.ttlSeconds || cacheTtlSeconds);
        sendJson(res, 200, { key, value: body.value });
        return;
      }

      if (req.method === "DELETE") {
        sendJson(res, 200, { key, deleted: await deleteCache(key) });
        return;
      }
    }

    sendJson(res, 404, { error: "Not found" });
  } catch (err) {
    sendJson(res, 503, {
      error: err.message,
      hint: "Check that Redis is running and reachable.",
    });
  }
});

server.listen(port, () => {
  console.log(`[redis-cache] listening on ${port}`);
  console.log(`[redis-cache] redis: ${redisHost}:${redisPort}`);
  console.log(`[redis-cache] try: http://localhost:${port}/users/1`);
});
