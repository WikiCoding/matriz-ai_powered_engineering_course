package com.wikicoding.matrizbackend;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.sun.net.httpserver.HttpServer;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
import io.opentelemetry.sdk.resources.Resource;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class OpenTelemetryLoggingTests {

    @Test
    void initializesSdkWithCompatibleIncubatorApi() {
        // Arrange
        var builder = OpenTelemetrySdk.builder();

        // Act: initializes SdkConfigProvider, which failed with incubator API 1.55.0-alpha.
        try (var telemetry = builder.build()) {
            // Assert
            assertThat(telemetry.getLogsBridge().get("startup-regression")).isNotNull();
        }
    }

    @Test
    void exportsLogMessageServiceNameAndRequestIdOverOtlpHttp() throws Exception {
        // Arrange: a disposable OTLP receiver, without Docker or application data.
        var received = new LinkedBlockingQueue<String>();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/otlp/v1/logs", exchange -> {
            try (exchange) {
                received.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                exchange.getResponseHeaders().set("Content-Type", "application/x-protobuf");
                exchange.sendResponseHeaders(200, -1);
            }
        });
        server.start();
        var logger = (Logger) LoggerFactory.getLogger("otlp-delivery-test");
        var previousLevel = logger.getLevel();
        var previousAdditive = logger.isAdditive();
        var appender = new OpenTelemetryAppender();
        appender.setName("OTLP_TEST");
        appender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        appender.setCaptureMdcAttributes("requestId");
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.INFO);
        logger.setAdditive(false);

        try (var exporter = OtlpHttpLogRecordExporter.builder()
                .setEndpoint("http://127.0.0.1:" + server.getAddress().getPort() + "/otlp/v1/logs")
                .setTimeout(Duration.ofSeconds(2)).build();
             var provider = SdkLoggerProvider.builder()
                     .setResource(Resource.create(Attributes.of(
                             AttributeKey.stringKey("service.name"), "matriz-backend")))
                     .addLogRecordProcessor(BatchLogRecordProcessor.builder(exporter).build())
                     .build()) {
            var telemetry = OpenTelemetrySdk.builder().setLoggerProvider(provider).build();
            new OpenTelemetryAppenderInitializer(telemetry).afterPropertiesSet();

            // Act
            try (var ignored = MDC.putCloseable("requestId", "delivery-test-request")) {
                logger.info("otlp-delivery-marker");
            }
            var flush = provider.forceFlush().join(5, TimeUnit.SECONDS);

            // Assert: protobuf retains these UTF-8 fields on the actual HTTP wire.
            assertThat(flush.isSuccess()).isTrue();
            assertThat(received.poll(5, TimeUnit.SECONDS))
                    .contains("otlp-delivery-marker", "matriz-backend", "delivery-test-request");
        } finally {
            logger.detachAppender(appender);
            appender.stop();
            logger.setLevel(previousLevel);
            logger.setAdditive(previousAdditive);
            OpenTelemetryAppender.install(OpenTelemetry.noop());
            server.stop(0);
        }
    }
}
