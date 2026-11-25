package com.gateway.ratelimiter.limiter;

/**
 * Outcome of a rate-limit check.
 *
 * @param allowed      whether the request may proceed
 * @param remaining    tokens left in the bucket after this call
 * @param limit        bucket capacity (max burst)
 * @param retryAfterMs how long the caller should wait before retrying (0 when allowed)
 */
public record RateLimitResult(boolean allowed, long remaining, int limit, long retryAfterMs) {

    public long retryAfterSeconds() {
        return (long) Math.ceil(retryAfterMs / 1000.0);
    }
}
