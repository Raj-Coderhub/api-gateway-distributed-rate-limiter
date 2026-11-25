package com.gateway.ratelimiter.limiter;

import com.gateway.ratelimiter.config.RateLimitProperties;
import com.gateway.ratelimiter.config.RateLimitProperties.Tier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientResolverTest {

    private ClientResolver resolver;

    @BeforeEach
    void setUp() {
        RateLimitProperties props = new RateLimitProperties();
        props.setDefaultTier(new Tier(10, 5));
        props.getTiers().put("pro-key", new Tier(50, 20));
        resolver = new ClientResolver(props);
    }

    @Test
    void usesApiKeyWhenPresent() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("X-API-Key", "pro-key");
        assertEquals("key:pro-key", resolver.clientId(req));
        assertEquals(50, resolver.tierFor(req).getCapacity());
    }

    @Test
    void fallsBackToIpAndDefaultTier() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("10.1.2.3");
        assertEquals("ip:10.1.2.3", resolver.clientId(req));
        assertEquals(10, resolver.tierFor(req).getCapacity());
    }

    @Test
    void honoursForwardedForHeader() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("X-Forwarded-For", "203.0.113.9, 10.0.0.1");
        assertEquals("ip:203.0.113.9", resolver.clientId(req));
    }

    @Test
    void unknownApiKeyGetsDefaultTier() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("X-API-Key", "not-configured");
        assertEquals(10, resolver.tierFor(req).getCapacity());
    }
}
