# Simple Load Balancer

```mermaid
flowchart LR
  U[Client] --> LB[Load Balancer]
  LB --> S1[App Server 1]
  LB --> S2[App Server 2]
```