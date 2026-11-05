package com.saga.payment.kafka;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.saga.dto.order.CancelOrderAction;
import com.saga.dto.order.UpdateOrderToPaidStatusAction;
import com.saga.dto.payment.ProcessPaymentAction;
import com.saga.payment.service.PaymentService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PaymentHandlerTest {

  private static final String ORDER_TOPIC = "order-topic";

  @Mock private PaymentService paymentService;

  @Mock private KafkaTemplate<String, Object> kafkaTemplate;

  @InjectMocks private PaymentHandler paymentHandler;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(this.paymentHandler, "orderTopic", ORDER_TOPIC);
  }

  @Test
  void givenProcessPaymentAction_whenPaymentSucceeds_thenSendsUpdateOrderToPaidStatusAction() {
    // given
    final UUID operationId = UUID.randomUUID();
    final UUID orderId = UUID.randomUUID();
    final ProcessPaymentAction action = this.paymentAction(operationId, orderId);
    when(this.paymentService.pay(orderId, action.getProductId(), 3.0D, "1234 5678 9012 3456"))
        .thenReturn(UUID.randomUUID());

    // when
    this.paymentHandler.handlePaymentAction(action);

    // then
    final UpdateOrderToPaidStatusAction expected =
        UpdateOrderToPaidStatusAction.builder().operationId(operationId).orderId(orderId).build();
    verify(this.kafkaTemplate).send(ORDER_TOPIC, expected);
  }

  @Test
  void givenProcessPaymentAction_whenPaymentFails_thenSendsCancelOrderAction() {
    // given
    final UUID operationId = UUID.randomUUID();
    final UUID orderId = UUID.randomUUID();
    final ProcessPaymentAction action = this.paymentAction(operationId, orderId);
    doThrow(new RuntimeException("Payment failed"))
        .when(this.paymentService)
        .pay(any(), any(), anyDouble(), anyString());

    // when
    this.paymentHandler.handlePaymentAction(action);

    // then
    final CancelOrderAction expected =
        CancelOrderAction.builder()
            .operationId(operationId)
            .orderId(orderId)
            .productId(action.getProductId())
            .quantity(action.getQuantity())
            .build();
    verify(this.kafkaTemplate).send(ORDER_TOPIC, expected);
  }

  private ProcessPaymentAction paymentAction(final UUID operationId, final UUID orderId) {
    return ProcessPaymentAction.builder()
        .operationId(operationId)
        .orderId(orderId)
        .productId(UUID.randomUUID())
        .quantity(2)
        .amountToBePaid(3.0D)
        .creditCardNumber("1234 5678 9012 3456")
        .build();
  }
}
