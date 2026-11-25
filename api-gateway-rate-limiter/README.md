# API Gateway & Distributed Rate Limiting System

A Spring Boot API gateway that throttles traffic with a **Redis-backed Token Bucket**, shared across
any number of gateway instances. Includes a small web dashboard to generate load and watch throttling live.

**Stack:** Java 17 · Spring Boot 3 · Redis (Lua scripting) · Maven · Docker · HTML/JS dashboard

## How it works

```
Client ──> RateLimitFilter ──> GatewayController ──> Upstream service
               │  (429 if empty)
               ▼
        TokenBucketRateLimiter ──> Redis (Lua script, atomic)
```

1. Every request to `/gateway/**` passes through `RateLimitFilter`.
2. The caller is identified by the `X-API-Key` header, or by IP when no key is sent.
3. A Lua script runs **atomically inside Redis**: it refills the bucket based on elapsed time
   (`tokens = min(capacity, tokens + elapsed * refillRate)`), then takes 1 token if available.
4. Allowed → forwarded to the upstream service. Empty bucket → `429 Too Many Requests` with a
   `Retry-After` header, and the upstream is never touched.

### Why Lua + Redis?
- **Atomic:** read-refill-consume happens in one step, so two gateway instances can't both spend the same token.
- **Shared state:** all instances use the same bucket per client, so limits hold across the whole cluster.
- **One clock:** the script uses Redis `TIME`, avoiding clock skew between app servers.
- **Self-cleaning:** idle buckets expire automatically (`PEXPIRE`).

## Run it

### Option A: Docker (2 gateway instances + Redis)
```bash
docker compose up --build
```
Dashboard: http://localhost:8081 (second instance on :8082 shares the same limits).

### Option B: Local
```bash
docker run -d -p 6379:6379 redis:7-alpine
mvn spring-boot:run
```
Dashboard: http://localhost:8080

## Try it

```bash
./scripts/burst.sh 20                # anonymous: ~10 pass, rest get 429
./scripts/burst.sh 20 free-key       # burst 5, then 1/sec
./scripts/burst.sh 60 pro-key        # burst 50, 20/sec

curl -i http://localhost:8080/gateway/hello   # see X-RateLimit-* headers
curl http://localhost:8080/admin/stats        # cluster-wide counters
```

Distributed proof: alternate requests between `:8081` and `:8082` with the same key; the bucket drains as one.

## Configuration (`application.yml`)

| Property | Meaning |
|---|---|
| `ratelimit.upstream-url` | Backend that `/gateway/**` forwards to |
| `ratelimit.default-tier.capacity` | Max burst for anonymous clients |
| `ratelimit.default-tier.refill-rate` | Sustained tokens/second |
| `ratelimit.tiers.<api-key>` | Per-API-key limits |
| `ratelimit.fail-open` | If Redis is down: `true` lets traffic through, `false` returns 503 |

## Endpoints

| Path | Description |
|---|---|
| `/gateway/**` | Rate-limited proxy to the upstream |
| `/demo/hello`, `/demo/orders` | Built-in sample upstream |
| `/admin/stats` | `GET` totals and per-client counts, `DELETE` resets |
| `/actuator/health` | Health check |
| `/` | Dashboard |

## Project structure

```
src/main/java/com/gateway/ratelimiter
├── config/    RateLimitProperties, RedisConfig (loads the Lua script)
├── limiter/   TokenBucketRateLimiter, ClientResolver, RateLimitResult
├── filter/    RateLimitFilter (429 + Retry-After + X-RateLimit-* headers)
├── gateway/   GatewayController (reverse proxy), DemoUpstreamController
└── stats/     StatsService (Redis counters), StatsController
src/main/resources/scripts/token_bucket.lua   atomic bucket logic
src/main/resources/static/index.html          dashboard
```

## Ideas to extend
- Per-route limits and a sliding-window variant for comparison
- Micrometer + Prometheus/Grafana metrics
- Redis Cluster / Sentinel for high availability
- JWT auth instead of static API keys
- Integration tests with Testcontainers
