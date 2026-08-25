package com.example.demo.it213_session11_bai1;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "otel")
public class OtelBatchProperties {
    private final Exporter exporter = new Exporter();
    private final Bsp bsp = new Bsp();

    public Exporter getExporter() { return exporter; }
    public Bsp getBsp() { return bsp; }

    public static class Exporter {
        private final Otlp otlp = new Otlp();
        public Otlp getOtlp() { return otlp; }
    }

    public static class Otlp {
        private String endpoint;
        private Duration timeout = Duration.ofSeconds(3);
        private final Headers headers = new Headers();

        public String getEndpoint() { return endpoint; }
        public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
        public Duration getTimeout() { return timeout; }
        public void setTimeout(Duration timeout) { this.timeout = timeout; }
        public Headers getHeaders() { return headers; }
    }

    public static class Headers {
        private String authorization;
        private String xLangfuseIngestionVersion = "4";

        public String getAuthorization() { return authorization; }
        public void setAuthorization(String authorization) { this.authorization = authorization; }
        public String getXLangfuseIngestionVersion() { return xLangfuseIngestionVersion; }
        public void setXLangfuseIngestionVersion(String value) { this.xLangfuseIngestionVersion = value; }
    }

    public static class Bsp {
        private int maxQueueSize = 2048;
        private Duration scheduleDelay = Duration.ofSeconds(2);
        private int maxExportBatchSize = 512;
        private Duration exportTimeout = Duration.ofSeconds(3);

        public int getMaxQueueSize() { return maxQueueSize; }
        public void setMaxQueueSize(int maxQueueSize) { this.maxQueueSize = maxQueueSize; }
        public Duration getScheduleDelay() { return scheduleDelay; }
        public void setScheduleDelay(Duration scheduleDelay) { this.scheduleDelay = scheduleDelay; }
        public int getMaxExportBatchSize() { return maxExportBatchSize; }
        public void setMaxExportBatchSize(int maxExportBatchSize) { this.maxExportBatchSize = maxExportBatchSize; }
        public Duration getExportTimeout() { return exportTimeout; }
        public void setExportTimeout(Duration exportTimeout) { this.exportTimeout = exportTimeout; }
    }
}
