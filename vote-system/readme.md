# Ensuring Users Vote Only Once

This document defines a **simple, strict, and consistent model** to ensure that every verified person can cast **exactly one vote per election**, backed by **database-level guarantees**.

---

## Key Principle

Each real user has:

- **One global `voter_id`**, permanent across all elections
- May cast **one vote per election**, enforced by a unique constraint

---

# 1. Identity Model

## Global `voter_id`

Each participant, after verification (email, WebAuthn, passport, gov ID, etc.), receives a **single permanent `voter_id`**.

Characteristics:

- Stable across all elections
- Pseudonymized (e.g., UUID or salted hash of identity)
- Never changes, even if the user votes in many elections
- Not tied to personal data in the voting tables

The real personal information (email, phone) stays only in the **USERS** table.

---

# 2. Tokenized Authorization to Vote

Before voting, the backend issues a **short-lived vote token**, containing:

- `voter_id`
- `election_id`
- expiration timestamp

This prevents API abuse and avoids passing raw voter_id directly.

---

# 3. Database Constraint — The Core Guarantee

At the database level:

```sql
UNIQUE (election_id, voter_id)
```

This **absolutely prevents double-voting**, even under concurrency or race conditions.

A vote insert looks like:

```sql
INSERT INTO votes (election_id, voter_id, vote_payload, receipt_hash)
VALUES ($1, $2, $3, $4);
```

If the user already voted:

- The DB throws an error
- The transaction rolls back
- The backend replies `{ "error": "already_voted" }`

This is the strongest possible guarantee.

---

# 4. Minimal Required Tables

- `users`
- `elections`
- `voter_identities`
- `votes`

# 🗳️ Database Model

## 1. ERD (Updated)

```mermaid
erDiagram

    USERS {
        UUID user_id PK
        TEXT auth_provider
        TEXT auth_provider_id
        TEXT email
        TEXT phone
        TIMESTAMPTZ created_at
    }

    VOTER_IDENTITIES {
        UUID voter_id PK
        UUID user_id FK
        TEXT identity_hash
        TIMESTAMPTZ created_at
    }

    ELECTIONS {
        UUID election_id PK
        TEXT name
        TEXT status
        TIMESTAMPTZ starts_at
        TIMESTAMPTZ ends_at
    }

    VOTES {
        UUID vote_id PK
        UUID election_id FK
        UUID voter_id FK
        JSONB vote_payload
        TIMESTAMPTZ timestamp
        TEXT receipt_hash
    }

    USERS ||--|| VOTER_IDENTITIES : "has global voter_id"
    VOTER_IDENTITIES ||--|{ VOTES : "can vote in elections"
    ELECTIONS ||--|{ VOTES : "receives votes"
```

---

# 2. Tables

## USERS

```sql
CREATE TABLE users (
  user_id UUID PRIMARY KEY,
  auth_provider TEXT NOT NULL,        -- Google, Apple, etc.
  auth_provider_id TEXT NOT NULL,     -- unique per provider user
  email TEXT,
  phone TEXT,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  UNIQUE(auth_provider, auth_provider_id)
);
```

---

## VOTER_IDENTITIES

Global, permanent voting identity.

```sql
CREATE TABLE voter_identities (
  voter_id UUID PRIMARY KEY,
  user_id UUID REFERENCES users(user_id),
  identity_hash TEXT NOT NULL,         -- hashed identity (for privacy)
  created_at TIMESTAMPTZ DEFAULT NOW(),
  UNIQUE (identity_hash)               -- ensures no duplicate humans
);
```

---

## ELECTIONS

```sql
CREATE TABLE elections (
  election_id UUID PRIMARY KEY,
  name TEXT NOT NULL,
  status TEXT NOT NULL,        -- draft, open, closed
  starts_at TIMESTAMPTZ,
  ends_at TIMESTAMPTZ
);
```

---

## VOTES

**Critical rule:** one vote per election per voter.

```sql
CREATE TABLE votes (
  vote_id UUID PRIMARY KEY,
  election_id UUID REFERENCES elections(election_id),
  voter_id UUID REFERENCES voter_identities(voter_id),
  vote_payload JSONB NOT NULL,          -- encrypted or anonymized ballot
  timestamp TIMESTAMPTZ DEFAULT NOW(),
  receipt_hash TEXT UNIQUE,
  UNIQUE (election_id, voter_id)
);
```

---

# 3. Interaction Flow

```mermaid
sequenceDiagram
  participant U as User
  participant AUTH as Auth Provider
  participant SYS as Voting System
  participant DBU as USERS
  participant DBVI as VOTER_IDENTITIES
  participant DBV as VOTES

  U->>AUTH: Authenticate (Google/WebAuthn/etc.)
  AUTH-->>SYS: identity confirmed

  SYS->>DBU: get or create user record
  DBU-->>SYS: user_id

  SYS->>DBVI: fetch or create global voter_id
  DBVI-->>SYS: voter_id

  U->>SYS: submit vote (via vote token)
  SYS->>DBV: INSERT vote

  alt first vote
      DBV-->>SYS: success
      SYS-->>U: vote accepted (receipt)
  else duplicate
      DBV-->>SYS: UNIQUE constraint violation
      SYS-->>U: { "error": "already_voted" }
  end
```

---

# Final Summary

### **1. One real person → One global voter_id**

Permanent for all elections.

### **2. One vote per election**

Enforced by `UNIQUE(election_id, voter_id)`.

### **3. Minimal tables**

Only 4 tables remain.

### **4. Clean privacy boundaries**

Personal data stays only in USERS.
Voting data uses only `voter_id`.
