package com.admintem.web.security;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SecurityMetricsTest {

    @Test
    void recordsSecurityEventsWithoutSensitiveTags() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        SecurityMetrics metrics = new SecurityMetrics(registry);

        metrics.unauthorized();
        metrics.accessDenied("method");
        metrics.login("failure");

        assertEquals(1.0, registry.get("admintem_security_requests_total")
                .tag("result", "unauthorized").counter().count());
        assertEquals(1.0, registry.get("admintem_security_requests_total")
                .tag("result", "forbidden").tag("layer", "method").counter().count());
        assertEquals(1.0, registry.get("admintem_login_total")
                .tag("result", "failure").counter().count());
    }
}
