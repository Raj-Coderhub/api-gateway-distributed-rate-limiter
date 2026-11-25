package com.gateway.ratelimiter.stats;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/admin/stats")
public class StatsController {

    private final StatsService stats;

    public StatsController(StatsService stats) {
        this.stats = stats;
    }

    @GetMapping
    public Map<String, Object> get() {
        return stats.snapshot();
    }

    @DeleteMapping
    public ResponseEntity<Void> reset() {
        stats.reset();
        return ResponseEntity.noContent().build();
    }
}
