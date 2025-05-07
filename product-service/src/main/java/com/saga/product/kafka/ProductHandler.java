package com.saga.product.kafka;

import com.saga.dto.order.CreateOrderAction;
import com.saga.dto.product.ProductReserveAction;
import com.saga.dto.product.ProductRollbackAction;
import com.saga.product.service.ProductService;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@KafkaListener(topics = {"${kafka.topic.name.product.topic}"})
@Slf4j
public class ProductHandler {

    @Value("${kafka.topic.name.order.topic}")
    private String orderTopic;

    private final ProductService productService;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaHandler
    public void handleProductAction(@Payload final ProductReserveAction action) {
        log.info("handleProductEvent. Received ProductReserveAction: {} ", action);
        try {

            this.productService.buyProduct(action.getProductId(), action.getQuantity());

      final CreateOrderAction createOrderAction = CreateOrderAction.builder()
          .operationId(action.getOperationId())
          .productId(action.getProductId())
          .quantity(action.getQuantity())
          .build();
      this.kafkaTemplate.send(this.orderTopic, createOrderAction);

            log.info("handleProductEvent. Sent CreateOrderAction: {} ", createOrderAction);
        } catch (Exception e) {
            log.error("handleProductEvent. Error while buying product: {}", e.getMessage());
        }
    }

    @KafkaHandler
    public void handleProductAction(@Payload final ProductRollbackAction action) {
        log.info("handleProductEvent. Received ProductRollbackAction: {} ", action);
        try {
            this.productService.rollbackProduct(action.getProductId(), action.getQuantity());
        } catch (Exception e) {
            //HERE WE SHOULD THROW A RETRYABLE EXCEPTION
        }
    }

}
