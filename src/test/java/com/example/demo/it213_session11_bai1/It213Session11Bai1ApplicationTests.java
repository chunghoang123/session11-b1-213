package com.example.demo.it213_session11_bai1;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "management.tracing.enabled=false",
        "otel.exporter.otlp.headers.authorization=Basic test-only"
})
class It213Session11Bai1ApplicationTests {

    @Test
    void contextLoads() {
    }

}
