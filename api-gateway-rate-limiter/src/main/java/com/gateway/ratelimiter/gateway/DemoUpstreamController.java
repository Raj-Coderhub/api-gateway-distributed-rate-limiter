package com.gateway.ratelimiter.gateway;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Stand-in backend service so the project works out of the box.
 * Point ratelimit.upstream-url at a real service to replace it.
 */
@RestController
@RequestMapping("/demo")
public class DemoUpstreamController {

    @GetMapping("/hello")
    public Map<String, Object> hello() {
        return Map.of("message", "Hello from the upstream service", "time", Instant.now().toString());
    }

    @GetMapping("/orders")
    public Map<String, Object> orders() {
        return Map.of("orders", java.util.List.of(
                Map.of("id", 1, "item", "Keyboard"),
                Map.of("id", 2, "item", "Monitor")));
    }
}
