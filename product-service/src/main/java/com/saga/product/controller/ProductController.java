package com.saga.product.controller;

import com.saga.dto.product.ProductReserveAction;
import com.saga.product.dto.BuySomethingRequest;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ProductController {

  @Value("${kafka.topic.name.product.topic}")
  private String productTopic;

  private final KafkaTemplate<String, Object> kafkaTemplate;

  @RequestMapping(method = RequestMethod.POST, path = "/product/buy")
  public ResponseEntity<Void> getProducts(@RequestBody final BuySomethingRequest request) {

    final ProductReserveAction productReserveAction = ProductReserveAction.builder()
        .operationId(UUID.randomUUID())
        .quantity(request.getQuantity())
        .productId(request.getProductId())
        .build();

    this.kafkaTemplate.send(this.productTopic, productReserveAction);

    return ResponseEntity.ok().build();
  }
}
