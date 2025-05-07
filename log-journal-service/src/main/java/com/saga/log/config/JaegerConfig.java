package com.saga.log.config;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.exporter.jaeger.JaegerGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JaegerConfig {

    @Value("${otel.exporter.jaeger.endpoint}")
    private String jaegerEndpoint;

    @Value("${spring.application.name}")
    private String serviceName;

    @Bean
    public Tracer tracer(OpenTelemetry openTelemetry) {
        return openTelemetry.getTracer(this.serviceName);
    }

    @Bean
    public OpenTelemetry openTelemetry() {
        final JaegerGrpcSpanExporter jaegerExporter = JaegerGrpcSpanExporter.builder()
                .setEndpoint(this.jaegerEndpoint)
                .build();

        final BatchSpanProcessor spanProcessor = BatchSpanProcessor.builder(jaegerExporter).build();

        final Resource serviceResource = Resource.getDefault()
                .merge(Resource.create(Attributes.builder()
                        .put("service.name", this.serviceName)
                        .build()));

        final SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(spanProcessor)
                .setResource(serviceResource)
                .build();

        return OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .buildAndRegisterGlobal();
    }
}
