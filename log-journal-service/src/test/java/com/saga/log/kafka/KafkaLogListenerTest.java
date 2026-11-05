package com.saga.log.kafka;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.saga.dto.order.CreateOrderAction;
import com.saga.log.service.impl.KafkaLogServiceImpl;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class KafkaLogListenerTest {

  private static final String SERVICE_NAME = "log-journal-service";

  @Mock private KafkaLogServiceImpl kafkaLogServiceImpl;

  @Mock private Tracer tracer;

  @Mock(answer = Answers.RETURNS_SELF)
  private SpanBuilder spanBuilder;

  @Mock private Span span;

  @Mock private Scope scope;

  @InjectMocks private KafkaLogListener kafkaLogListener;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(this.kafkaLogListener, "serviceName", SERVICE_NAME);
    when(this.tracer.spanBuilder(anyString())).thenReturn(this.spanBuilder);
    when(this.spanBuilder.startSpan()).thenReturn(this.span);
    when(this.span.makeCurrent()).thenReturn(this.scope);
  }

  static Stream<Arguments> topicListeners() {
    return Stream.of(
        Arguments.of(
            (BiConsumer<KafkaLogListener, ConsumerRecord<String, Object>>)
                KafkaLogListener::listenOrderTopic,
            "order-topic"),
        Arguments.of(
            (BiConsumer<KafkaLogListener, ConsumerRecord<String, Object>>)
                KafkaLogListener::listenPaymentTopic,
            "payment-topic"),
        Arguments.of(
            (BiConsumer<KafkaLogListener, ConsumerRecord<String, Object>>)
                KafkaLogListener::listenProductTopic,
            "product-topic"));
  }

  @ParameterizedTest(name = "listener for {1} logs the message and ends the span")
  @MethodSource("topicListeners")
  void givenTracedRecord_whenListenerInvoked_thenLogsMessageAndEndsSpan(
      final BiConsumer<KafkaLogListener, ConsumerRecord<String, Object>> listener,
      final String topic) {
    // given
    final UUID operationId = UUID.randomUUID();
    final CreateOrderAction value =
        CreateOrderAction.builder()
            .operationId(operationId)
            .productId(UUID.randomUUID())
            .quantity(1)
            .build();
    final ConsumerRecord<String, Object> record = new ConsumerRecord<>(topic, 0, 0L, "key", value);

    // when
    listener.accept(this.kafkaLogListener, record);

    // then
    verify(this.tracer).spanBuilder("Buy something flow. OperationId: " + operationId);
    verify(this.spanBuilder).setAttribute("event.class.name", "CreateOrderAction");
    verify(this.spanBuilder).setAttribute("event.operation_id", operationId.toString());
    verify(this.spanBuilder).setAttribute("event.topic", topic);
    verify(this.kafkaLogServiceImpl).logKafkaMessage(topic, value, SERVICE_NAME);
    verify(this.span).end();
  }

  @Test
  void givenNullValue_whenListenOrderTopic_thenUsesUnknownEventAndLogsNullMessage() {
    // given
    final ConsumerRecord<String, Object> record =
        new ConsumerRecord<>("order-topic", 0, 0L, "key", null);

    // when
    this.kafkaLogListener.listenOrderTopic(record);

    // then
    verify(this.spanBuilder).setAttribute("event.class.name", "UnknownEvent");
    verify(this.spanBuilder).setAttribute("event.json", "null");
    verify(this.kafkaLogServiceImpl).logKafkaMessage("order-topic", null, SERVICE_NAME);
    verify(this.span).end();
  }

  @Test
  void givenLoggingFails_whenListenOrderTopic_thenRecordsExceptionAndEndsSpan() {
    // given
    final UUID operationId = UUID.randomUUID();
    final CreateOrderAction value =
        CreateOrderAction.builder()
            .operationId(operationId)
            .productId(UUID.randomUUID())
            .quantity(1)
            .build();
    final ConsumerRecord<String, Object> record =
        new ConsumerRecord<>("order-topic", 0, 0L, "key", value);
    final RuntimeException failure = new RuntimeException("database unavailable");
    doThrow(failure)
        .when(this.kafkaLogServiceImpl)
        .logKafkaMessage(eq("order-topic"), eq(value), eq(SERVICE_NAME));

    // when
    this.kafkaLogListener.listenOrderTopic(record);

    // then
    verify(this.span).recordException(failure);
    verify(this.span).end();
  }
}
