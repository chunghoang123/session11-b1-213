package com.example.demo.it213_session11_bai1;

import io.micrometer.tracing.Tracer;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
class TracingProbe {
    private final Tracer tracer;

    TracingProbe(Tracer tracer) { this.tracer = tracer; }

    @EventListener(ApplicationReadyEvent.class)
    void sendStartupProbe() {
        var span = tracer.nextSpan().name("langfuse-startup-probe").start();
        try (var ignored = tracer.withSpan(span)) {
            span.tag("service", "rikkeipay-assistant");
            span.event("application-ready");
        }
        finally {
            span.end();
        }
    }
}
