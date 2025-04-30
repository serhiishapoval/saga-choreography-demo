package com.saga.payment.kafka;

import com.saga.dto.order.CancelOrderAction;
import com.saga.dto.order.UpdateOrderToPaidStatusAction;
import com.saga.dto.payment.ProcessPaymentAction;
import com.saga.payment.service.PaymentService;
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
@KafkaListener(topics = {"${kafka.topic.name.payment.topic}"})
@Slf4j
public class PaymentHandler {

  @Value("${kafka.topic.name.order.topic}")
  private String orderTopic;

  private final PaymentService paymentService;
  private final KafkaTemplate<String, Object> kafkaTemplate;

  @KafkaHandler
  public void handlePaymentAction(@Payload final ProcessPaymentAction action) {
    log.info("handlePaymentAction. Received ProcessPaymentAction: {} ", action);
    try {
      this.paymentService.pay(action.getOrderId(), action.getProductId(),
          action.getAmountToBePaid(), action.getCreditCardNumber());
      final UpdateOrderToPaidStatusAction updateOrderToPaidStatusAction = UpdateOrderToPaidStatusAction.builder()
          .operationId(action.getOperationId())
          .orderId(action.getOrderId())
          .build();

      this.kafkaTemplate.send(this.orderTopic, updateOrderToPaidStatusAction);

      log.info("handlePaymentAction. Sent UpdateOrderToPaidStatusAction: {} ",
          updateOrderToPaidStatusAction);
    } catch (Exception e) {
      final CancelOrderAction cancelOrderAction = CancelOrderAction.builder()
          .operationId(action.getOperationId())
          .productId(action.getProductId())
          .orderId(action.getOrderId())
          .quantity(action.getQuantity())
          .build();
      this.kafkaTemplate.send(this.orderTopic, cancelOrderAction);

      log.info("handlePaymentAction. Sent CancelOrderAction: {} ", cancelOrderAction);
    }

  }
}
