package com.example.demo.it213_session11_bai1;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "management.tracing.enabled=false",
        "otel.exporter.otlp.headers.authorization=Basic test-only"
})
@EnableConfigurationProperties(OtelBatchProperties.class)
class OtelBatchPropertiesTests {
    @Autowired
    private OtelBatchProperties properties;

    @Test
    void bindsBoundedNonBlockingBatchSettings() {
        assertThat(properties.getBsp().getMaxQueueSize()).isEqualTo(2048);
        assertThat(properties.getBsp().getMaxExportBatchSize()).isEqualTo(512);
        assertThat(properties.getBsp().getScheduleDelay()).isEqualTo(Duration.ofSeconds(2));
        assertThat(properties.getBsp().getExportTimeout()).isEqualTo(Duration.ofSeconds(3));
    }
}
