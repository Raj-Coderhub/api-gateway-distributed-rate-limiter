package com.gateway.ratelimiter.limiter;

import com.gateway.ratelimiter.config.RateLimitProperties.Tier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Distributed Token Bucket limiter. All bucket state lives in Redis and is
 * updated by a single Lua script, so concurrent requests hitting different
 * gateway instances can never over-spend tokens (no read-modify-write race).
 */
@Service
public class TokenBucketRateLimiter {

    private static final String KEY_PREFIX = "rl:bucket:";

    private final StringRedisTemplate redis;
    @SuppressWarnings("rawtypes")
    private final DefaultRedisScript<List> script;

    @SuppressWarnings("rawtypes")
    public TokenBucketRateLimiter(StringRedisTemplate redis, DefaultRedisScript<List> tokenBucketScript) {
        this.redis = redis;
        this.script = tokenBucketScript;
    }

    public RateLimitResult tryAcquire(String clientId, Tier tier) {
        return tryAcquire(clientId, tier, 1);
    }

    @SuppressWarnings("unchecked")
    public RateLimitResult tryAcquire(String clientId, Tier tier, int tokens) {
        List<Long> result = redis.execute(
                script,
                List.of(KEY_PREFIX + clientId),
                String.valueOf(tier.getCapacity()),
                String.valueOf(tier.getRefillRate()),
                String.valueOf(tokens));

        if (result == null || result.size() < 3) {
            throw new IllegalStateException("Unexpected response from rate limit script");
        }
        return new RateLimitResult(
                result.get(0) == 1L,
                result.get(1),
                tier.getCapacity(),
                result.get(2));
    }
}
