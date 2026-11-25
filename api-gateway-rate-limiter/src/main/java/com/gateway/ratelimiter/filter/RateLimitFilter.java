package com.gateway.ratelimiter.filter;

import com.gateway.ratelimiter.config.RateLimitProperties;
import com.gateway.ratelimiter.config.RateLimitProperties.Tier;
import com.gateway.ratelimiter.limiter.ClientResolver;
import com.gateway.ratelimiter.limiter.RateLimitResult;
import com.gateway.ratelimiter.limiter.TokenBucketRateLimiter;
import com.gateway.ratelimiter.stats.StatsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Throttles every request that goes through the gateway (/gateway/**).
 * Over-limit callers get 429 + Retry-After before any upstream work happens,
 * which is what protects the backend from traffic spikes.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final TokenBucketRateLimiter limiter;
    private final ClientResolver resolver;
    private final StatsService stats;
    private final RateLimitProperties props;

    public RateLimitFilter(TokenBucketRateLimiter limiter, ClientResolver resolver,
                           StatsService stats, RateLimitProperties props) {
        this.limiter = limiter;
        this.resolver = resolver;
        this.stats = stats;
        this.props = props;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/gateway");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String clientId = resolver.clientId(request);
        Tier tier = resolver.tierFor(request);

        RateLimitResult result;
        try {
            result = limiter.tryAcquire(clientId, tier);
        } catch (Exception e) {
            log.error("Rate limiter unavailable: {}", e.getMessage());
            if (props.isFailOpen()) {
                chain.doFilter(request, response);
            } else {
                writeJson(response, HttpStatus.SERVICE_UNAVAILABLE,
                        "{\"error\":\"rate_limiter_unavailable\"}");
            }
            return;
        }

        stats.record(clientId, result.allowed());
        response.setHeader("X-RateLimit-Limit", String.valueOf(result.limit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(result.remaining()));

        if (result.allowed()) {
            chain.doFilter(request, response);
            return;
        }

        response.setHeader("Retry-After", String.valueOf(result.retryAfterSeconds()));
        writeJson(response, HttpStatus.TOO_MANY_REQUESTS,
                "{\"error\":\"rate_limit_exceeded\",\"retryAfterMs\":" + result.retryAfterMs() + "}");
    }

    private void writeJson(HttpServletResponse response, HttpStatus status, String body) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(body);
    }
}
