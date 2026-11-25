package com.gateway.ratelimiter.stats;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Cluster-wide allowed/blocked counters kept in Redis so every gateway
 * instance contributes to (and the dashboard reads) the same numbers.
 */
@Service
public class StatsService {

    private static final Logger log = LoggerFactory.getLogger(StatsService.class);
    private static final String TOTALS = "rl:stats:totals";
    private static final String CLIENTS = "rl:stats:clients";

    private final StringRedisTemplate redis;

    public StatsService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void record(String clientId, boolean allowed) {
        String field = allowed ? "allowed" : "blocked";
        try {
            redis.opsForHash().increment(TOTALS, field, 1);
            redis.opsForHash().increment(CLIENTS, clientId + "|" + field, 1);
        } catch (Exception e) {
            log.debug("Could not record stats: {}", e.getMessage());
        }
    }

    public Map<String, Object> snapshot() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("allowed", readLong(TOTALS, "allowed"));
        out.put("blocked", readLong(TOTALS, "blocked"));

        Map<String, Map<String, Long>> clients = new LinkedHashMap<>();
        redis.opsForHash().entries(CLIENTS).forEach((k, v) -> {
            String[] parts = k.toString().split("\\|");
            if (parts.length == 2) {
                clients.computeIfAbsent(parts[0], c -> new LinkedHashMap<>())
                        .put(parts[1], Long.parseLong(v.toString()));
            }
        });
        out.put("clients", clients);
        return out;
    }

    public void reset() {
        redis.delete(TOTALS);
        redis.delete(CLIENTS);
    }

    private long readLong(String key, String field) {
        Object v = redis.opsForHash().get(key, field);
        return v == null ? 0 : Long.parseLong(v.toString());
    }
}
