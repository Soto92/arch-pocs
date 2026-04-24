# CloudFront (CDN)

```mermaid
flowchart LR
  U[Client] --> CDN[CloudFront (CDN)]
  CDN --> O[Origin Server]
```

## CloudFront (CDN)

What it is:

- Content Delivery Network that caches and serves content from edge locations.

When to use:

- You serve static assets or APIs to users in many regions.
- You want lower latency and DDoS protection at the edge.

Why use it:

- Faster responses and reduced origin load.

Problem it solves:

- High latency and origin overload for global traffic.

## Demo (local simulator)

Start the origin:

- `node origin.js`

Start the CDN proxy/cache:

- `node cdn.js`

Try a few requests and watch `X-Cache`:

- `curl -i http://localhost:4000/`
- `curl -i http://localhost:4000/api/time`
- `curl -i http://localhost:4000/nocache`
