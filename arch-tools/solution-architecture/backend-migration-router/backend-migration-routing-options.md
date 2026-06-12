# Backend Migration Routing Options

## Context

During a backend migration, the frontend may need to call different backend implementations depending on feature readiness, user segment, route, tenant, region, or rollout stage.

One current option is a frontend middleware controlled by a feature flag. This works, but it is not the only pattern. The best choice depends on how much control, observability, reversibility, and operational ownership the migration requires.

## Option 1: Frontend Middleware With Feature Flags

The frontend contains routing logic that decides whether a request should go to the legacy backend or the new backend. The decision can be controlled by a feature flag service, environment configuration, user attributes, tenant metadata, or route-level rules.

### Advantages

- Fast to implement when the frontend already owns the request orchestration.
- Good for UI-specific migrations where backend choice depends on visible product behavior.
- Easy to test with selected users, internal accounts, or specific browser sessions.
- Rollback can be quick if the feature flag provider is reliable.
- Useful when the frontend must adapt request or response shapes during the migration.

### Tradeoffs

- Routing logic is distributed into the client-facing layer, which can increase frontend complexity.
- Browser clients may expose hints about backend topology unless carefully abstracted.
- Harder to guarantee consistent behavior across web, mobile, API consumers, and batch integrations.
- Observability can be fragmented unless the frontend adds clear request metadata and tracing.
- Feature flag evaluation can become business-critical; failures must have safe defaults.
- Caching, retries, authentication, and CORS behavior may become harder to reason about.

### Best Fit

Use this when the migration is closely tied to frontend behavior, the number of clients is small, and the rollout needs product-level targeting.

## Option 2: Backend For Frontend Gateway

A Backend for Frontend, or BFF, sits between the frontend and backend services. The frontend always calls the BFF, and the BFF decides whether to route each request to the legacy backend or the new backend.

### Advantages

- Keeps backend routing out of the browser-facing code.
- Provides one stable API contract for the frontend during migration.
- Centralizes authentication, request shaping, response mapping, logging, and tracing.
- Easier to apply server-side feature flags, tenant rules, allowlists, and percentage rollouts.
- Better suited for complex migrations where request and response contracts differ.

### Tradeoffs

- Requires an additional backend layer to build, operate, and scale.
- Can become a long-term coupling point if it accumulates too much business logic.
- Adds network latency compared with direct frontend-to-backend calls.
- Needs strong ownership boundaries so it does not become a generic proxy with unclear responsibilities.

### Best Fit

Use this when the frontend needs a stable contract while backend systems evolve underneath it.

## Option 3: API Gateway Routing

An API gateway routes requests to either the legacy backend or the new backend based on path, header, cookie, tenant, JWT claims, percentage rollout, or other request metadata.

### Advantages

- Centralized routing outside application code.
- Works across multiple clients, not only the web frontend.
- Usually integrates well with rate limiting, authentication, logging, WAF rules, and traffic policies.
- Enables path-based, header-based, weighted, or canary routing.
- Rollbacks can be operationally simple if gateway configuration is versioned and automated.

### Tradeoffs

- Less suitable when requests or responses need complex transformation.
- Gateway rules can become difficult to understand if too many business conditions are encoded there.
- Debugging can be harder if routing decisions are not logged clearly.
- Requires strong DevOps or platform ownership.
- Some advanced rollout rules may depend on gateway product capabilities.

### Best Fit

Use this when both backends expose compatible or nearly compatible APIs and routing can be decided from request metadata.

## Option 4: Service Mesh Traffic Splitting

A service mesh such as Istio, Linkerd, or Consul can split traffic between backend versions at the infrastructure layer.

### Advantages

- Strong support for canary releases, weighted traffic, retries, circuit breaking, mTLS, and observability.
- Application code can remain mostly unaware of the migration.
- Useful for progressive delivery and controlled backend replacement.
- Works well in Kubernetes-heavy environments.

### Tradeoffs

- Operational complexity is higher than application-level routing.
- Usually routes service-to-service traffic, not browser-to-service decisions directly.
- Business-level targeting, such as user or tenant based routing, can be harder unless metadata is propagated.
- Requires mature platform practices and clear ownership.

### Best Fit

Use this when the migration is mostly infrastructure or service-version based and the organization already operates a service mesh.

## Option 5: Reverse Proxy Or Edge Routing

A reverse proxy or edge platform, such as NGINX, Envoy, CloudFront, Cloudflare, Fastly, or Azure Front Door, routes requests before they reach the application.

### Advantages

- Keeps routing close to the network edge.
- Can reduce latency when rules are simple and globally distributed.
- Useful for path-based, host-based, cookie-based, or header-based routing.
- Can support quick rollback by changing proxy configuration.
- Works without requiring frontend redeploys.

### Tradeoffs

- Complex business logic at the edge can become hard to test and maintain.
- Local development and staging parity may be weaker.
- Observability must be deliberately designed.
- Request and response transformation capabilities vary by provider.

### Best Fit

Use this when routing rules are simple, performance matters, and the routing decision can be made before reaching the application.

## Option 6: Strangler Fig Pattern

The new backend gradually replaces parts of the legacy backend. Routing is usually done by capability, endpoint, domain area, or bounded context.

### Advantages

- Reduces migration risk by moving one capability at a time.
- Enables incremental delivery without a large cutover.
- Encourages cleaner domain boundaries.
- Can be combined with a gateway, BFF, proxy, or service mesh.

### Tradeoffs

- Requires careful contract management between old and new systems.
- Data consistency can become challenging if both systems write to overlapping data.
- The transitional state can last longer than expected.
- Requires strong observability to understand which system handled each request.

### Best Fit

Use this for large migrations where replacing the backend all at once is risky or unrealistic.

## Option 7: Backend Adapter Or Anti-Corruption Layer

An adapter layer normalizes differences between the legacy backend and the new backend. The frontend or gateway calls a stable interface, while the adapter maps requests and responses to the appropriate backend.

### Advantages

- Protects clients from backend contract differences.
- Keeps migration-specific mapping logic in one place.
- Useful when the legacy and new systems have different data models.
- Can reduce frontend conditional logic.

### Tradeoffs

- Adds another layer to own and operate.
- Mapping logic can become complicated if the two systems diverge significantly.
- May hide domain inconsistencies that should eventually be resolved.
- Adds latency and another possible failure point.

### Best Fit

Use this when the new backend cannot match the legacy API contract immediately.

## Option 8: Dual Write And Read Switching

The application writes to both legacy and new systems, then gradually switches reads from the legacy backend to the new backend.

### Advantages

- Allows the new backend to be validated with production-like data before serving reads.
- Enables comparison between old and new behavior.
- Supports safer cutover when data correctness is the main risk.

### Tradeoffs

- Dual writes are difficult to make reliable.
- Partial failures can create data divergence.
- Requires reconciliation, audit tooling, and clear source-of-truth rules.
- Rollback semantics can be complex if users have already interacted with new-system data.

### Best Fit

Use this when the migration includes data ownership changes and correctness must be proven before switching user traffic.

## Option 9: Shadow Traffic

Production requests continue to be served by the legacy backend, while a copy of selected traffic is sent to the new backend for validation. The new backend response is not returned to the user.

### Advantages

- Tests the new backend under realistic traffic without user impact.
- Helps detect correctness, performance, and scaling issues before cutover.
- Useful for comparing responses, latency, error rates, and side effects.

### Tradeoffs

- The new backend must avoid unsafe side effects for shadowed requests.
- Requires careful handling of writes, idempotency, and external integrations.
- Response comparison can be non-trivial if systems are not deterministic.
- Does not fully validate user-facing behavior because users still see legacy responses.

### Best Fit

Use this before switching traffic when confidence in the new backend is still low.

## Option 10: Consumer-Driven Contract Testing

Instead of routing traffic dynamically, both backends are tested against the same client contracts. The migration proceeds only when the new backend satisfies the expected API behavior.

### Advantages

- Reduces runtime surprises before traffic is shifted.
- Clarifies which behaviors the frontend and other clients depend on.
- Useful with CI/CD gates and automated release validation.
- Complements gateway, BFF, and strangler migration strategies.

### Tradeoffs

- Does not solve runtime routing by itself.
- Requires discipline to maintain contracts as clients evolve.
- May miss behavior that is not captured in tests, such as performance, authorization edge cases, or data-dependent flows.

### Best Fit

Use this as a safety mechanism alongside any routing strategy.

## Comparison Summary

| Option | Main Control Point | Best For | Main Risk |
| --- | --- | --- | --- |
| Frontend middleware with feature flags | Frontend | Product-targeted rollout | Client complexity and fragmented behavior |
| BFF gateway | Application backend layer | Stable frontend contract | Extra service ownership |
| API gateway routing | API platform | Cross-client routing | Gateway rule complexity |
| Service mesh | Infrastructure | Service-version rollout | Platform complexity |
| Reverse proxy or edge routing | Edge/network | Simple high-performance routing | Limited business logic |
| Strangler fig | Architecture pattern | Incremental replacement | Long transitional complexity |
| Adapter layer | Application boundary | Contract normalization | Mapping complexity |
| Dual write and read switching | Data/application layer | Data migration | Data divergence |
| Shadow traffic | Traffic validation | Pre-cutover confidence | Unsafe side effects |
| Contract testing | CI/CD validation | Regression prevention | Not a runtime routing solution |

## Recommended Direction

For most backend migrations, avoid putting all migration responsibility in the frontend unless the migration is small and truly UI-specific.

A stronger default architecture is:

1. Use a BFF or API gateway as the stable routing point.
2. Use server-side feature flags or gateway rules for controlled rollout.
3. Apply the strangler fig pattern by moving one capability at a time.
4. Add contract tests to protect client expectations.
5. Use shadow traffic or dual reads when correctness and performance need production validation.

The existing frontend middleware can still be useful as a temporary tactical layer, especially for quick experiments or UI-specific behavior. However, for a durable migration strategy, routing should usually move closer to the backend boundary or platform layer so that all clients receive consistent behavior and the migration can be observed, rolled back, and governed centrally.

## Practical Decision Guide

Choose frontend middleware when:

- Only the web frontend is affected.
- The migration logic is temporary and simple.
- The backend contracts differ in ways the frontend already handles.
- Product teams need fast user-level experimentation.

Choose a BFF when:

- The frontend needs one stable API.
- The old and new backends have different contracts.
- Request and response transformation is required.
- You want server-side observability and safer feature flags.

Choose an API gateway or edge proxy when:

- Routing rules are mostly based on path, header, cookie, tenant, or percentage.
- Multiple clients need the same migration behavior.
- The backend APIs are mostly compatible.
- You want centralized operational rollback.

Choose service mesh when:

- The organization already uses Kubernetes and mesh tooling.
- The migration is mainly about backend service versions.
- Traffic splitting, retries, and circuit breaking are important.

Choose dual write, shadow traffic, and contract tests when:

- Data correctness is the main migration risk.
- You need evidence before moving real users.
- The new backend must be validated against production traffic patterns.
