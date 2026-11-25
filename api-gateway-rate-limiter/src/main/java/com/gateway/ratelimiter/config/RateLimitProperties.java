package com.gateway.ratelimiter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "ratelimit")
public class RateLimitProperties {

    private String upstreamUrl = "http://localhost:8080/demo";
    private boolean failOpen = true;
    private Tier defaultTier = new Tier(10, 5);
    private Map<String, Tier> tiers = new HashMap<>();

    public static class Tier {
        private int capacity;
        private double refillRate;

        public Tier() {}

        public Tier(int capacity, double refillRate) {
            this.capacity = capacity;
            this.refillRate = refillRate;
        }

        public int getCapacity() { return capacity; }
        public void setCapacity(int capacity) { this.capacity = capacity; }
        public double getRefillRate() { return refillRate; }
        public void setRefillRate(double refillRate) { this.refillRate = refillRate; }
    }

    public String getUpstreamUrl() { return upstreamUrl; }
    public void setUpstreamUrl(String upstreamUrl) { this.upstreamUrl = upstreamUrl; }
    public boolean isFailOpen() { return failOpen; }
    public void setFailOpen(boolean failOpen) { this.failOpen = failOpen; }
    public Tier getDefaultTier() { return defaultTier; }
    public void setDefaultTier(Tier defaultTier) { this.defaultTier = defaultTier; }
    public Map<String, Tier> getTiers() { return tiers; }
    public void setTiers(Map<String, Tier> tiers) { this.tiers = tiers; }
}
