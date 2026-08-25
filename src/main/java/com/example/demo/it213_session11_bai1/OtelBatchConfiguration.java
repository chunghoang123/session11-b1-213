package com.example.demo.it213_session11_bai1;

import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(OtelBatchProperties.class)
@ConditionalOnProperty(name = "management.tracing.enabled", matchIfMissing = true)
public class OtelBatchConfiguration {

    @Bean
    SpanProcessor langfuseBatchSpanProcessor(OtelBatchProperties properties) {
        var otlp = properties.getExporter().getOtlp();
        var bsp = properties.getBsp();
        SpanExporter delegate = OtlpHttpSpanExporter.builder()
                .setEndpoint(otlp.getEndpoint())
                .addHeader("Authorization", otlp.getHeaders().getAuthorization())
                .addHeader("x-langfuse-ingestion-version", otlp.getHeaders().getXLangfuseIngestionVersion())
                .setTimeout(otlp.getTimeout())
                .build();

        return BatchSpanProcessor.builder(new ConfirmingSpanExporter(delegate, otlp.getEndpoint()))
                .setMaxQueueSize(bsp.getMaxQueueSize())
                .setScheduleDelay(bsp.getScheduleDelay().toMillis(), TimeUnit.MILLISECONDS)
                .setMaxExportBatchSize(bsp.getMaxExportBatchSize())
                .setExporterTimeout(bsp.getExportTimeout().toMillis(), TimeUnit.MILLISECONDS)
                .build();
    }

    private static final class ConfirmingSpanExporter implements SpanExporter {
        private static final Logger log = LoggerFactory.getLogger(ConfirmingSpanExporter.class);
        private final SpanExporter delegate;
        private final String endpoint;
        private final AtomicLong exportedSpans = new AtomicLong();

        private ConfirmingSpanExporter(SpanExporter delegate, String endpoint) {
            this.delegate = delegate;
            this.endpoint = endpoint;
        }

        @Override
        public CompletableResultCode export(Collection<SpanData> spans) {
            CompletableResultCode result = delegate.export(spans);
            result.whenComplete(() -> {
                if (result.isSuccess()) {
                    long total = exportedSpans.addAndGet(spans.size());
                    log.info("OTLP export succeeded: batchSize={}, totalExported={}, endpoint={}",
                            spans.size(), total, endpoint);
                }
                else {
                    log.warn("OTLP export failed or timed out: batchSize={}, endpoint={}", spans.size(), endpoint);
                }
            });
            return result;
        }

        @Override
        public CompletableResultCode flush() { return delegate.flush(); }

        @Override
        public CompletableResultCode shutdown() { return delegate.shutdown(); }
    }
}
