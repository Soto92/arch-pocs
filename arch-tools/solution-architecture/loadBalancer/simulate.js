const http = require('http');

const target = process.env.TARGET || 'http://localhost:3000';
const rate = Number(process.env.RATE || 200); // requests per second
const durationSec = Number(process.env.DURATION || 5);
const concurrency = Number(process.env.CONCURRENCY || 50);

const agent = new http.Agent({ keepAlive: true, maxSockets: concurrency });

let sent = 0;
let ok = 0;
let limited = 0;
let other = 0;
let errors = 0;
let inFlight = 0;

let tokens = 0;
const tickMs = 100;
const tokensPerTick = rate / (1000 / tickMs);

function fireOnce() {
  sent += 1;
  inFlight += 1;
  const req = http.request(target, { agent }, (res) => {
    if (res.statusCode === 200) ok += 1;
    else if (res.statusCode === 429) limited += 1;
    else other += 1;
    res.resume();
    inFlight -= 1;
  });
  req.on('error', () => { errors += 1; inFlight -= 1; });
  req.end();
}

const interval = setInterval(() => {
  tokens += tokensPerTick;
  while (tokens >= 1 && inFlight < concurrency) {
    tokens -= 1;
    fireOnce();
  }
}, tickMs);

setTimeout(() => {
  clearInterval(interval);
  setTimeout(() => {
    console.log('Results');
    console.log(`Target: ${target}`);
    console.log(`Sent: ${sent}`);
    console.log(`OK: ${ok}`);
    console.log(`Limited(429): ${limited}`);
    console.log(`Other: ${other}`);
    console.log(`Errors: ${errors}`);
    process.exit(0);
  }, 500);
}, durationSec * 1000);
