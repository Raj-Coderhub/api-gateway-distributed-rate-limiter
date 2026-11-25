package com.gateway.ratelimiter.gateway;

import com.gateway.ratelimiter.config.RateLimitProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.Enumeration;
import java.util.Set;

/**
 * Minimal reverse proxy: /gateway/{path} -> {upstream-url}/{path}.
 * Rate limiting already happened in RateLimitFilter by the time we get here.
 */
@RestController
public class GatewayController {

    private static final Set<String> HOP_BY_HOP = Set.of(
            "host", "connection", "content-length", "transfer-encoding", "keep-alive",
            "upgrade", "te", "trailer", "proxy-authorization", "proxy-authenticate");

    private final RestClient client = RestClient.create();
    private final RateLimitProperties props;

    public GatewayController(RateLimitProperties props) {
        this.props = props;
    }

    @RequestMapping("/gateway/**")
    public ResponseEntity<byte[]> proxy(HttpServletRequest request,
                                        @RequestBody(required = false) byte[] body) {
        String path = request.getRequestURI().substring("/gateway".length());
        String query = request.getQueryString() == null ? "" : "?" + request.getQueryString();
        URI target = URI.create(props.getUpstreamUrl() + path + query);

        try {
            return client.method(HttpMethod.valueOf(request.getMethod()))
                    .uri(target)
                    .headers(h -> copyRequestHeaders(request, h))
                    .body(body == null ? new byte[0] : body)
                    .retrieve()
                    .onStatus(status -> true, (req, res) -> { /* pass status through */ })
                    .toEntity(byte[].class)
                    .mutate()
                    .headers(h -> HOP_BY_HOP.forEach(h::remove))
                    .build();
        } catch (ResourceAccessException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("{\"error\":\"upstream_unreachable\"}".getBytes());
        }
    }

    private void copyRequestHeaders(HttpServletRequest request, HttpHeaders out) {
        Enumeration<String> names = request.getHeaderNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            if (!HOP_BY_HOP.contains(name.toLowerCase())) {
                out.addAll(name, java.util.Collections.list(request.getHeaders(name)));
            }
        }
    }
}
