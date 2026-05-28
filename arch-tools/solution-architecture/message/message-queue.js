const http = require("http");

const port = Number(process.env.PORT || 7000);
const messages = [];
let nextId = 1;

function sendJson(res, statusCode, body) {
  res.writeHead(statusCode, { "Content-Type": "application/json" });
  res.end(JSON.stringify(body));
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

function createMessage(payload) {
  const message = {
    id: nextId,
    payload,
    status: "queued",
    attempts: 0,
    createdAt: new Date().toISOString(),
  };

  nextId += 1;
  messages.push(message);
  return message;
}

function takeNextMessage() {
  const message = messages.find((item) => item.status === "queued");

  if (!message) {
    return null;
  }

  message.status = "processing";
  message.attempts += 1;
  message.processingAt = new Date().toISOString();
  return message;
}

function ackMessage(id) {
  const index = messages.findIndex((message) => message.id === id);

  if (index === -1) {
    return null;
  }

  const [message] = messages.splice(index, 1);
  message.status = "acknowledged";
  message.acknowledgedAt = new Date().toISOString();
  return message;
}

function retryMessage(id) {
  const message = messages.find((item) => item.id === id);

  if (!message) {
    return null;
  }

  message.status = "queued";
  delete message.processingAt;
  return message;
}

const server = http.createServer(async (req, res) => {
  const url = new URL(req.url, `http://${req.headers.host}`);

  try {
    if (req.method === "GET" && url.pathname === "/health") {
      sendJson(res, 200, { status: "ok" });
      return;
    }

    if (req.method === "GET" && url.pathname === "/messages") {
      sendJson(res, 200, { messages });
      return;
    }

    if (req.method === "POST" && url.pathname === "/messages") {
      const body = await readBody(req);

      if (!Object.hasOwn(body, "payload")) {
        sendJson(res, 400, { error: "Missing payload" });
        return;
      }

      sendJson(res, 201, { message: createMessage(body.payload) });
      return;
    }

    if (req.method === "POST" && url.pathname === "/messages/next") {
      const message = takeNextMessage();

      if (!message) {
        sendJson(res, 204, {});
        return;
      }

      sendJson(res, 200, { message });
      return;
    }

    const ackMatch = url.pathname.match(/^\/messages\/(\d+)\/ack$/);
    if (req.method === "POST" && ackMatch) {
      const message = ackMessage(Number(ackMatch[1]));

      if (!message) {
        sendJson(res, 404, { error: "Message not found" });
        return;
      }

      sendJson(res, 200, { message });
      return;
    }

    const retryMatch = url.pathname.match(/^\/messages\/(\d+)\/retry$/);
    if (req.method === "POST" && retryMatch) {
      const message = retryMessage(Number(retryMatch[1]));

      if (!message) {
        sendJson(res, 404, { error: "Message not found" });
        return;
      }

      sendJson(res, 200, { message });
      return;
    }

    sendJson(res, 404, { error: "Not found" });
  } catch (err) {
    sendJson(res, 400, { error: err.message });
  }
});

server.listen(port, () => {
  console.log(`[message-queue] listening on ${port}`);
});
