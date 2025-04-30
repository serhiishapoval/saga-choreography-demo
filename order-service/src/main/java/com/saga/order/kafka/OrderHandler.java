package com.saga.order.kafka;

import com.saga.dto.order.CancelOrderAction;
import com.saga.dto.order.CreateOrderAction;
import com.saga.dto.order.UpdateOrderToPaidStatusAction;
import com.saga.dto.payment.ProcessPaymentAction;
import com.saga.dto.product.ProductRollbackAction;
import com.saga.order.entity.OrderEntity;
import com.saga.order.service.OrderService;
import com.saga.order.util.CreditCardUtils;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@KafkaListener(topics = {"${kafka.topic.name.order.topic}"})
@Slf4j
public class OrderHandler {

  @Value("${kafka.topic.name.payment.topic}")
  private String paymentTopic;

  @Value("${kafka.topic.name.product.topic}")
  private String productTopic;

  private final OrderService orderService;

  private final KafkaTemplate<String, Object> kafkaTemplate;

  @KafkaHandler
  public void handleOrderAction(@Payload final CreateOrderAction action) {
    log.info("handleOrderAction. Received CreateOrderAction: {} ", action);

    try {
      final OrderEntity createdOrder = this.orderService.createOrder(action.getProductId(),
          action.getQuantity());

      final ProcessPaymentAction processPaymentAction = ProcessPaymentAction.builder()
          .operationId(action.getOperationId())
          .amountToBePaid(action.getQuantity().doubleValue() * 1.5D)
          .orderId(createdOrder.getOrderId())
          .productId(action.getProductId())
          .creditCardNumber(CreditCardUtils.getCreditCardNumber())
          .quantity(action.getQuantity())
          .build();

      this.kafkaTemplate.send(this.paymentTopic, processPaymentAction);

      log.info("handleOrderAction. Sent ProcessPaymentAction: {} ", processPaymentAction);
    } catch (Exception e) {
      this.rollbackProduct(action.getOperationId(), action.getProductId(), action.getQuantity());
    }
  }

  @KafkaHandler
  public void handleOrderAction(@Payload final CancelOrderAction action) {
    log.info("handleOrderAction. Received CancelOrderAction: {} ", action);
    try {
      this.orderService.cancelOrder(action.getOrderId());
    } catch (Exception e) {
      //HERE WE SHOULD THROW A RETRYABLE EXCEPTION
    }
    this.rollbackProduct(action.getOperationId(), action.getProductId(), action.getQuantity());
  }

  @KafkaHandler
  public void handleOrderAction(@Payload final UpdateOrderToPaidStatusAction action) {
    log.info("handleOrderAction. Received UpdateOrderToPaidStatusAction: {} ", action);
    try {
      this.orderService.payOrder(action.getOrderId());
    } catch (Exception e) {
      //HERE WE SHOULD THROW A RETRYABLE EXCEPTION
    }
  }

  private void rollbackProduct(final UUID operationId, final UUID productId,
      final Integer quantity) {
    final ProductRollbackAction productRollbackAction = ProductRollbackAction.builder()
        .operationId(operationId)
        .productId(productId)
        .quantity(quantity)
        .build();

    this.kafkaTemplate.send(this.productTopic, productRollbackAction);

    log.info("handleOrderAction. Sent ProductRollbackAction: {} ", productRollbackAction);
  }
}
