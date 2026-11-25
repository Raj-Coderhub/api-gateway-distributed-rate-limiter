package com.gateway.ratelimiter.limiter;

import com.gateway.ratelimiter.config.RateLimitProperties;
import com.gateway.ratelimiter.config.RateLimitProperties.Tier;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 * Decides who the caller is (API key or IP) and which bucket settings apply.
 */
@Component
public class ClientResolver {

    public static final String API_KEY_HEADER = "X-API-Key";

    private final RateLimitProperties props;

    public ClientResolver(RateLimitProperties props) {
        this.props = props;
    }

    /** API key if present, otherwise the client IP (honours X-Forwarded-For). */
    public String clientId(HttpServletRequest request) {
        String apiKey = request.getHeader(API_KEY_HEADER);
        if (apiKey != null && !apiKey.isBlank()) {
            return "key:" + apiKey.trim();
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return "ip:" + forwarded.split(",")[0].trim();
        }
        return "ip:" + request.getRemoteAddr();
    }

    /** Tier configured for the API key, or the default tier for everyone else. */
    public Tier tierFor(HttpServletRequest request) {
        String apiKey = request.getHeader(API_KEY_HEADER);
        if (apiKey != null) {
            Tier tier = props.getTiers().get(apiKey.trim());
            if (tier != null) {
                return tier;
            }
        }
        return props.getDefaultTier();
    }
}
