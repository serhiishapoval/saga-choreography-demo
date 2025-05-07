package com.saga.log.kafka;

import com.saga.dto.trace.BaseTracing;
import com.saga.log.service.impl.KafkaLogServiceImpl;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaLogListener {

    private final KafkaLogServiceImpl kafkaLogServiceImpl;

    @Value("${spring.application.name}")
    private String serviceName;

    private final Tracer tracer;

    @KafkaListener(
            topics = {"${kafka.topic.name.order.topic}"},
            groupId = "${spring.kafka.consumer.group-id}")
    public void listenOrderTopic(final ConsumerRecord<String, Object> record) {
        this.processMessageWithTracing("order-topic", record);
    }

    @KafkaListener(
            topics = {"${kafka.topic.name.payment.topic}"},
            groupId = "${spring.kafka.consumer.group-id}")
    public void listenPaymentTopic(final ConsumerRecord<String, Object> record) {
        this.processMessageWithTracing("payment-topic", record);
    }

    @KafkaListener(
            topics = {"${kafka.topic.name.product.topic}"},
            groupId = "${spring.kafka.consumer.group-id}")
    public void listenProductTopic(final ConsumerRecord<String, Object> record) {
        this.processMessageWithTracing("product-topic", record);
    }

    private void processMessageWithTracing(final String topic, final ConsumerRecord<String, Object> record) {
        String operationId;
        String eventClassName;
        String eventJson;

        if (record.value() instanceof BaseTracing event) {
            operationId = (event.getOperationId() != null) ? event.getOperationId().toString() : UUID.randomUUID().toString();
            eventClassName = event.getClass().getSimpleName();
            eventJson = record.value().toString();
        } else {
            operationId = UUID.randomUUID().toString();
            eventClassName = (record.value() != null) ? record.value().getClass().getSimpleName() : "UnknownEvent";
            eventJson = (record.value() != null) ? record.value().toString() : "null";
        }

        final Span span = tracer.spanBuilder("Buy something flow. OperationId: " + operationId)
                .setAttribute("event.class.name", eventClassName)
                .setAttribute("event.operation_id", operationId)
                .setAttribute("event.topic", topic)
                .setAttribute("event.json", eventJson)
                .startSpan();

        try (final Scope scope = span.makeCurrent()) {
            log.debug("Consumed message from topic {}: {}", topic, record.value());
            this.kafkaLogServiceImpl.logKafkaMessage(record.topic(), record.value(), serviceName);
        } catch (Exception e) {
            span.recordException(e);
            log.error("Error processing message from topic {}: {}", topic, e.getMessage(), e);
        } finally {
            span.end();
        }
    }


}