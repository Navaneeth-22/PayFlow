# PayFlow

A distributed payment platform I built to learn event-driven
architecture and understand how systems like Razorpay work internally.

## What it does

Processes payments across 5 microservices using Kafka. When you
initiate a payment, it goes through fraud checks, ledger debit,
and webhook notification — all via async events. If any step fails
or gets stuck, the system compensates automatically.

## Why I built this

I wanted to understand how payment systems handle the hard problems:
- What happens if a service crashes mid-payment?
- How do you prevent duplicate charges from retry storms?
- How do you maintain correct balances under concurrent load?

## Tech

Java 21, Spring Boot 4.0, Kafka, PostgreSQL (one per service), Redis

## Running locally

Requires Docker Desktop.

    docker compose up -d

Then start each service:

    cd payment-service && mvn spring-boot:run

Services run on ports 8081-8084. API Gateway on 8080.

## The interesting parts

**Idempotency** — Two-tier approach using Redis + PostgreSQL.
Spent a day debugging a race condition where two concurrent requests
both got a Redis miss and both created payments. Fixed it with a
UNIQUE constraint as the durable fallback.

**No balance column** — Balance is computed from journal entries
every time. Sounds slow but it means the math is always correct
under concurrent load. A stored balance column kept giving wrong
results in my concurrency tests.

**Stuck payment detection** — Added this late after realizing
payments could hang forever if fraud-service was down. A scheduler
checks every 60 seconds and auto-fails or reverses stuck sagas.

## What I'd do differently

- The API gateway grew messier than I wanted. Would build it
  cleaner as a standalone project (working on that next — Aegis).
- No real auth. Used a test token endpoint. Production would
  use API keys like Razorpay does.
- Integration tests only cover payment-service. Would add
  cross-service tests with all 5 services running.

## Tests

    cd payment-service && mvn test -Dtest="*IT"

16 integration tests. Testcontainers spins up real Postgres,
Kafka, and Redis. Slowest test is the concurrency one —
100 threads hitting the same idempotency key simultaneously.

## What I Learned Building This
## Keeping DB and Kafka in sync

My first instinct was to save the payment to PostgreSQL and publish to Kafka in the same transaction. That breaks immediately — Kafka isn't transactional. If the app crashes after the DB commit but before the Kafka publish, the event is gone and money is in limbo.

I implemented the Transactional Outbox pattern. The payment and the event both go into PostgreSQL in one transaction. A background poller reads unpublished events and pushes to Kafka separately.

Took me a while to figure out the poller problem though — when I ran multiple instances, they kept publishing the same event twice. Fixed it with `SELECT ... FOR UPDATE SKIP LOCKED` so each poller instance grabs its own batch without stepping on others.

---

## Preventing double-spend under concurrent load

Wrote a test with 10 concurrent debits from the same account and the balance went negative. The bug was obvious in hindsight — two threads both read `balance = 1000`, both pass the check, both debit 900.

Two things fixed it:

- Route Kafka messages using `fromAccountId` as the partition key, so all debits from the same account hit the same consumer thread in order.
- Add `pg_advisory_xact_lock` on the account ID before every debit. It locks in Postgres memory rather than locking rows, so no deadlocks.

After both changes, the concurrent test passed every time.

---

## Idempotency under retry storms

A client that doesn't get an ACK will retry. I needed the same payment request to be safe to send multiple times.

I built an idempotency layer using `X-Idempotency-Key` — Redis checks if the key was already processed and returns the cached response. But Redis alone isn't enough. Two requests arriving at the exact same millisecond can both get a cache miss simultaneously.

The real safety net is a UNIQUE constraint on the `idempotency_keys` table. If Redis misses, Postgres catches the duplicate. I catch the constraint violation and return the stored response instead of letting it bubble up as a 500.

Proved it with a test — 100 concurrent threads, same idempotency key, exactly 1 payment in the database every time.

---

## Gateway thread exhaustion

I didn't want to use WebFlux — mixing reactive Mono/Flux debugging with standard servlet services sounded painful. But running a blocking proxy on Tomcat means threads pile up waiting for downstream responses and the pool exhausts under load.

Java 21 virtual threads solved this. When a thread blocks waiting for a downstream response, the JVM parks it and the CPU moves on. I get the concurrency of a reactive gateway with normal blocking code. The config is one line:

```yaml
spring:
  threads:
    virtual:
      enabled: true
```

---

## Rate limiter with no TTL

First version of the rate limiter used Redis `INCR` then `EXPIRE` as two separate commands. If the pod crashed between those two commands, the key lived in Redis forever with no expiry — permanently locking that user out.

Rewrote it as a Lua script so the increment, refill calculation, and TTL update happen atomically in one Redis operation. Redis executes Lua scripts as a single unbreakable unit — no partial state possible.


## License
This project is licensed under the MIT License.
