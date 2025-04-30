package com.saga.log.kafka;

import com.saga.log.service.impl.KafkaLogServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaLogListener {

  private final KafkaLogServiceImpl kafkaLogServiceImpl;

  @Value("${spring.application.name}")
  private String serviceName;

  @KafkaListener(
      topics = {"${kafka.topic.name.order.topic}"},
      groupId = "${spring.kafka.consumer.group-id}")
  public void listenOrderTopic(ConsumerRecord<String, Object> record) {
    log.debug("Received message from order topic: {}", record.value());
    this.kafkaLogServiceImpl.logKafkaMessage(record.topic(), record.value(), this.serviceName);
  }

  @KafkaListener(
      topics = {"${kafka.topic.name.payment.topic}"},
      groupId = "${spring.kafka.consumer.group-id}")
  public void listenPaymentTopic(ConsumerRecord<String, Object> record) {
    log.debug("Received message from payment topic: {}", record.value());
    this.kafkaLogServiceImpl.logKafkaMessage(record.topic(), record.value(), this.serviceName);
  }

  @KafkaListener(
      topics = {"${kafka.topic.name.product.topic}"},
      groupId = "${spring.kafka.consumer.group-id}")
  public void listenProductTopic(ConsumerRecord<String, Object> record) {
    log.debug("Received message from product topic: {}", record.value());
    this.kafkaLogServiceImpl.logKafkaMessage(record.topic(), record.value(), this.serviceName);
  }
}