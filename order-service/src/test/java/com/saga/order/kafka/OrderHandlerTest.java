package com.saga.order.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.saga.dto.order.CancelOrderAction;
import com.saga.dto.order.CreateOrderAction;
import com.saga.dto.order.UpdateOrderToPaidStatusAction;
import com.saga.dto.payment.ProcessPaymentAction;
import com.saga.dto.product.ProductRollbackAction;
import com.saga.order.entity.OrderEntity;
import com.saga.order.entity.OrderStatus;
import com.saga.order.service.OrderService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OrderHandlerTest {

  private static final String PAYMENT_TOPIC = "payment-topic";
  private static final String PRODUCT_TOPIC = "product-topic";

  @Mock private OrderService orderService;

  @Mock private KafkaTemplate<String, Object> kafkaTemplate;

  @InjectMocks private OrderHandler orderHandler;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(this.orderHandler, "paymentTopic", PAYMENT_TOPIC);
    ReflectionTestUtils.setField(this.orderHandler, "productTopic", PRODUCT_TOPIC);
  }

  @Test
  void givenCreateOrderAction_whenOrderCreated_thenSendsProcessPaymentActionToPaymentTopic() {
    // given
    final UUID operationId = UUID.randomUUID();
    final UUID productId = UUID.randomUUID();
    final UUID orderId = UUID.randomUUID();
    final CreateOrderAction action =
        CreateOrderAction.builder()
            .operationId(operationId)
            .productId(productId)
            .quantity(2)
            .build();
    when(this.orderService.createOrder(productId, 2))
        .thenReturn(new OrderEntity(orderId, productId, 2, OrderStatus.NEW.getStatus()));
    final ArgumentCaptor<ProcessPaymentAction> captor =
        ArgumentCaptor.forClass(ProcessPaymentAction.class);

    // when
    this.orderHandler.handleOrderAction(action);

    // then
    verify(this.kafkaTemplate).send(eq(PAYMENT_TOPIC), captor.capture());
    final ProcessPaymentAction sent = captor.getValue();
    assertThat(sent.getCreditCardNumber()).matches("\\d{4} \\d{4} \\d{4} \\d{4}");
    final ProcessPaymentAction expected =
        ProcessPaymentAction.builder()
            .operationId(operationId)
            .orderId(orderId)
            .productId(productId)
            .quantity(2)
            .amountToBePaid(3.0D)
            .creditCardNumber(sent.getCreditCardNumber())
            .build();
    assertThat(sent).isEqualTo(expected);
  }

  @Test
  void givenCreateOrderActionThatFails_whenHandled_thenSendsProductRollbackActionToProductTopic() {
    // given
    final UUID operationId = UUID.randomUUID();
    final UUID productId = UUID.randomUUID();
    final CreateOrderAction action =
        CreateOrderAction.builder()
            .operationId(operationId)
            .productId(productId)
            .quantity(2)
            .build();
    when(this.orderService.createOrder(productId, 2)).thenThrow(new RuntimeException("db down"));

    // when
    this.orderHandler.handleOrderAction(action);

    // then
    final ProductRollbackAction expected =
        ProductRollbackAction.builder()
            .operationId(operationId)
            .productId(productId)
            .quantity(2)
            .build();
    verify(this.kafkaTemplate).send(PRODUCT_TOPIC, expected);
    verify(this.kafkaTemplate, never()).send(eq(PAYMENT_TOPIC), any());
  }

  @Test
  void givenCancelOrderAction_whenOrderCancelled_thenSendsProductRollbackAction() {
    // given
    final UUID operationId = UUID.randomUUID();
    final UUID orderId = UUID.randomUUID();
    final UUID productId = UUID.randomUUID();
    final CancelOrderAction action =
        CancelOrderAction.builder()
            .operationId(operationId)
            .orderId(orderId)
            .productId(productId)
            .quantity(4)
            .build();

    // when
    this.orderHandler.handleOrderAction(action);

    // then
    verify(this.orderService).cancelOrder(orderId);
    final ProductRollbackAction expected =
        ProductRollbackAction.builder()
            .operationId(operationId)
            .productId(productId)
            .quantity(4)
            .build();
    verify(this.kafkaTemplate).send(PRODUCT_TOPIC, expected);
  }

  @Test
  void givenCancelOrderActionThatFails_whenHandled_thenStillSendsProductRollbackAction() {
    // given
    final UUID operationId = UUID.randomUUID();
    final UUID orderId = UUID.randomUUID();
    final UUID productId = UUID.randomUUID();
    final CancelOrderAction action =
        CancelOrderAction.builder()
            .operationId(operationId)
            .orderId(orderId)
            .productId(productId)
            .quantity(4)
            .build();
    doThrow(new RuntimeException("cancel failed")).when(this.orderService).cancelOrder(orderId);

    // when
    this.orderHandler.handleOrderAction(action);

    // then
    final ProductRollbackAction expected =
        ProductRollbackAction.builder()
            .operationId(operationId)
            .productId(productId)
            .quantity(4)
            .build();
    verify(this.kafkaTemplate).send(PRODUCT_TOPIC, expected);
  }

  @Test
  void givenUpdateOrderToPaidStatusAction_whenHandled_thenPaysOrderWithoutSendingMessages() {
    // given
    final UUID orderId = UUID.randomUUID();
    final UpdateOrderToPaidStatusAction action =
        UpdateOrderToPaidStatusAction.builder().orderId(orderId).build();

    // when
    this.orderHandler.handleOrderAction(action);

    // then
    verify(this.orderService).payOrder(orderId);
    verifyNoInteractions(this.kafkaTemplate);
  }

  @Test
  void givenUpdateOrderToPaidStatusActionThatFails_whenHandled_thenDoesNotPropagateException() {
    // given
    final UUID orderId = UUID.randomUUID();
    final UpdateOrderToPaidStatusAction action =
        UpdateOrderToPaidStatusAction.builder().orderId(orderId).build();
    doThrow(new RuntimeException("pay failed")).when(this.orderService).payOrder(orderId);

    // when
    this.orderHandler.handleOrderAction(action);

    // then
    verifyNoInteractions(this.kafkaTemplate);
  }
}
