# When And Where To Place Tools In Solution Architecture Diagrams

This guide explains where to place tools or services in solution architecture diagrams (cloud services and infrastructure), plus why they exist.

## What Belongs In A Solution Architecture Diagram

Goal: show the major runtime services, data flows, and boundaries.

Typical elements:
- Public entry points (DNS, CDN, load balancer, API gateway)
- Compute (containers, serverless, VMs)
- Data stores (relational, NoSQL, sharded clusters)
- Integration (queues, event buses, ETL)
- External systems (credit bureau, payment gateways)

## Where To Place Common Tools

- Load balancer: edge entry, in front of services
- AWS API Gateway: edge entry, in front of APIs
- Loan balance service: core compute layer
- Sharded database: data layer with shard boundaries
- Cache (Redis): between compute and data layers
- Messaging (Kafka/SNS/SQS): integration layer

## How Many Tools To Show

Focus on clarity, not completeness.

Guidelines:
- 8 to 15 major services per diagram is typically readable
- Group minor tools into shared boxes ("Shared Services")
- Use callouts for key choices (sharding, consistency, failover)

## Why And How To Choose

Include a tool if:
- It is a runtime dependency
- It owns critical data
- It is a critical scalability or reliability choice
- It is a security boundary

Exclude or collapse tools if:
- It is a utility without architecture impact
- It is an internal library
- It clutters the diagram without changing decisions

## Most Used Tools (What, When, Why)

## Load Balancer

What it is:
- A network service that distributes incoming traffic across multiple servers or instances.

When to use:
- You have more than one instance of a service.
- You need high availability or zero-downtime deploys.
- You want to absorb traffic spikes.

Why use it:
- Prevents a single server from being a bottleneck.
- Allows rolling deployments and failover.
- Improves reliability and performance.

Problem it solves:
- Single point of failure and uneven traffic distribution.

## Route 53 (DNS)

What it is:
- Managed DNS that maps domains to endpoints (load balancers, CloudFront, APIs).

When to use:
- You host public or private domains.
- You need health checks and DNS-based failover.

Why use it:
- Reliable global DNS with health-aware routing.

Problem it solves:
- Unreliable DNS, manual routing, and slow failovers.

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

## API Gateway

What it is:
- A managed entry point for APIs that handles routing, auth, throttling, and monitoring.

When to use:
- Multiple backend services need a single entry.
- You need consistent auth, rate limiting, or request validation.
- You need to expose APIs to external clients.

Why use it:
- Centralizes cross cutting concerns.
- Simplifies client integration.

Problem it solves:
- Duplicate gateway logic in every service and inconsistent security.

## EKS (Kubernetes)

What it is:
- Managed Kubernetes for running containerized services.

When to use:
- You need container orchestration, autoscaling, and service discovery.
- You run many microservices.

Why use it:
- Standardized deployment and scaling across services.

Problem it solves:
- Manual container management and inconsistent deployments.

## Cache (Redis or Memcached)

What it is:
- An in-memory store for fast reads.

When to use:
- Repeated reads of the same data.
- Expensive queries or computations.
- Need to reduce database load.

Why use it:
- Improves latency and throughput.

Problem it solves:
- Slow reads and database overload.

## Message Queue or Event Bus

What it is:
- A system that moves work asynchronously (SQS, Kafka, RabbitMQ).

When to use:
- Work can be done later or in parallel.
- You need reliable retries.
- You need to decouple producers from consumers.

Why use it:
- Smooths traffic spikes and increases resilience.

Problem it solves:
- Tight coupling and cascading failures during traffic spikes.

## MSK (Managed Kafka)

What it is:
- Managed Apache Kafka service for high-throughput event streaming.

When to use:
- You need durable, ordered streams with many producers and consumers.
- You want Kafka without managing brokers.

Why use it:
- Reliable event pipelines with less operational overhead.

Problem it solves:
- Running and scaling Kafka yourself.

## Database (Relational or NoSQL)

What it is:
- The system of record for persistent data.

When to use:
- You need strong data consistency or complex queries.
- You need durable storage.

Why use it:
- Ensures data durability and integrity.

Problem it solves:
- Data loss and inconsistent state.

## Sharded Database

What it is:
- A database split across multiple nodes by a shard key.

When to use:
- Data size or traffic exceeds single database limits.
- You need horizontal scale.

Why use it:
- Allows growth by adding nodes.

Problem it solves:
- Storage and performance limits of a single database.

## DynamoDB

What it is:
- Managed NoSQL key value and document database.

When to use:
- You need massive scale, low latency, and flexible schema.
- Access patterns are simple and predictable by keys.

Why use it:
- Automatic scaling and high availability without managing servers.

Problem it solves:
- Scaling relational databases for simple key based workloads.

## ElastiCache (Redis or Memcached)

What it is:
- Managed in-memory cache service.

When to use:
- You want caching without running your own Redis or Memcached servers.

Why use it:
- Fast reads with minimal ops effort.

Problem it solves:
- Operational burden of managing cache clusters.

## Service Discovery

What it is:
- A registry of active service instances.

When to use:
- Many dynamic services or containers.
- Autoscaling with frequent instance changes.

Why use it:
- Enables routing without hardcoding endpoints.

Problem it solves:
- Manual configuration and stale endpoints.

## Secrets Manager

What it is:
- Secure storage for credentials and keys.

When to use:
- You store API keys or database passwords.
- You need rotation and auditing.

Why use it:
- Prevents secrets leakage.

Problem it solves:
- Hardcoded secrets and weak access control.

## Observability Stack

What it is:
- Logs, metrics, and traces with alerting.

When to use:
- You need to troubleshoot production issues.
- You need uptime and performance visibility.

Why use it:
- Shortens incident response times.

Problem it solves:
- Blind spots in production systems.

## CloudWatch

What it is:
- AWS monitoring service for metrics, logs, alarms, and dashboards.

When to use:
- You run workloads on AWS and need native observability.

Why use it:
- Centralized monitoring and alerting for AWS resources.

Problem it solves:
- Fragmented monitoring and slow incident response.

## Example Placement

- Load balancer: edge entry
- AWS API Gateway: edge entry
- Sharded Postgres cluster: data layer with shard boundaries
- Third-party KYC: external integration
