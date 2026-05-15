const http = require("http");
const crypto = require("crypto");

const port = Number(process.env.PORT || 7010);
const apiKey = process.env.API_KEY || "demo-admin-key";
const encryptionKey = crypto
  .createHash("sha256")
  .update(process.env.SECRETS_MASTER_KEY || "local-demo-master-key")
  .digest();

const secrets = new Map();
const auditLog = [];

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

function isAuthorized(req) {
  return req.headers["x-api-key"] === apiKey;
}

function recordAudit(action, secretName, status, req) {
  auditLog.unshift({
    action,
    secretName,
    status,
    principal: req.headers["x-principal"] || "anonymous",
    at: new Date().toISOString(),
  });

  if (auditLog.length > 100) {
    auditLog.pop();
  }
}

function encryptSecret(value) {
  const iv = crypto.randomBytes(12);
  const cipher = crypto.createCipheriv("aes-256-gcm", encryptionKey, iv);
  const encrypted = Buffer.concat([
    cipher.update(String(value), "utf8"),
    cipher.final(),
  ]);
  const authTag = cipher.getAuthTag();

  return {
    iv: iv.toString("base64"),
    value: encrypted.toString("base64"),
    authTag: authTag.toString("base64"),
  };
}

function decryptSecret(encryptedSecret) {
  const decipher = crypto.createDecipheriv(
    "aes-256-gcm",
    encryptionKey,
    Buffer.from(encryptedSecret.iv, "base64"),
  );
  decipher.setAuthTag(Buffer.from(encryptedSecret.authTag, "base64"));

  return Buffer.concat([
    decipher.update(Buffer.from(encryptedSecret.value, "base64")),
    decipher.final(),
  ]).toString("utf8");
}

function publicSecret(secret) {
  const currentVersion = secret.versions[secret.versions.length - 1];

  return {
    name: secret.name,
    description: secret.description,
    currentVersion: currentVersion.version,
    rotationEnabled: secret.rotationEnabled,
    createdAt: secret.createdAt,
    updatedAt: secret.updatedAt,
  };
}

function createSecret(name, value, description = "") {
  const now = new Date().toISOString();
  const secret = {
    name,
    description,
    rotationEnabled: false,
    createdAt: now,
    updatedAt: now,
    versions: [
      {
        version: 1,
        encryptedSecret: encryptSecret(value),
        createdAt: now,
      },
    ],
  };

  secrets.set(name, secret);
  return secret;
}

function rotateSecret(secret, value) {
  const now = new Date().toISOString();
  const version = secret.versions.length + 1;

  secret.versions.push({
    version,
    encryptedSecret: encryptSecret(value),
    createdAt: now,
  });
  secret.rotationEnabled = true;
  secret.updatedAt = now;

  return secret;
}

const server = http.createServer(async (req, res) => {
  const url = new URL(req.url, `http://${req.headers.host}`);

  try {
    if (req.method === "GET" && url.pathname === "/health") {
      sendJson(res, 200, { status: "ok" });
      return;
    }

    if (!isAuthorized(req)) {
      recordAudit(req.method, url.pathname, "denied", req);
      sendJson(res, 401, { error: "Unauthorized" });
      return;
    }

    if (req.method === "GET" && url.pathname === "/secrets") {
      sendJson(res, 200, { secrets: Array.from(secrets.values()).map(publicSecret) });
      return;
    }

    if (req.method === "POST" && url.pathname === "/secrets") {
      const body = await readBody(req);

      if (!body.name || !Object.hasOwn(body, "value")) {
        sendJson(res, 400, { error: "Missing name or value" });
        return;
      }

      if (secrets.has(body.name)) {
        sendJson(res, 409, { error: "Secret already exists" });
        return;
      }

      const secret = createSecret(body.name, body.value, body.description);
      recordAudit("create", body.name, "success", req);
      sendJson(res, 201, { secret: publicSecret(secret) });
      return;
    }

    const secretMatch = url.pathname.match(/^\/secrets\/([^/]+)$/);
    if (secretMatch) {
      const name = decodeURIComponent(secretMatch[1]);
      const secret = secrets.get(name);

      if (!secret) {
        recordAudit(req.method, name, "not_found", req);
        sendJson(res, 404, { error: "Secret not found" });
        return;
      }

      if (req.method === "GET") {
        const versionParam = url.searchParams.get("version");
        const version = versionParam ? Number(versionParam) : secret.versions.length;
        const secretVersion = secret.versions.find((item) => item.version === version);

        if (!secretVersion) {
          sendJson(res, 404, { error: "Secret version not found" });
          return;
        }

        recordAudit("read", name, "success", req);
        sendJson(res, 200, {
          name,
          version,
          value: decryptSecret(secretVersion.encryptedSecret),
        });
        return;
      }

      if (req.method === "DELETE") {
        secrets.delete(name);
        recordAudit("delete", name, "success", req);
        sendJson(res, 200, { deleted: name });
        return;
      }
    }

    const rotationMatch = url.pathname.match(/^\/secrets\/([^/]+)\/rotate$/);
    if (req.method === "POST" && rotationMatch) {
      const name = decodeURIComponent(rotationMatch[1]);
      const secret = secrets.get(name);

      if (!secret) {
        recordAudit("rotate", name, "not_found", req);
        sendJson(res, 404, { error: "Secret not found" });
        return;
      }

      const body = await readBody(req);

      if (!Object.hasOwn(body, "value")) {
        sendJson(res, 400, { error: "Missing value" });
        return;
      }

      rotateSecret(secret, body.value);
      recordAudit("rotate", name, "success", req);
      sendJson(res, 200, { secret: publicSecret(secret) });
      return;
    }

    if (req.method === "GET" && url.pathname === "/audit") {
      sendJson(res, 200, { events: auditLog });
      return;
    }

    sendJson(res, 404, { error: "Not found" });
  } catch (err) {
    sendJson(res, 400, { error: err.message });
  }
});

server.listen(port, () => {
  console.log(`[secrets-manager] listening on ${port}`);
  console.log(`[secrets-manager] api key header: X-API-Key`);
  console.log(`[secrets-manager] default api key: ${apiKey}`);
});
