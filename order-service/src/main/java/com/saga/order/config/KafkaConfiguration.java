package com.saga.order.config;

import static com.saga.dto.constant.KafkaConstants.PARTITIONS;
import static com.saga.dto.constant.KafkaConstants.REPLICAS;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

@Configuration
public class KafkaConfiguration {

  @Value("${kafka.topic.name.order.topic}")
  private String orderTopic;

  @Bean
  public KafkaTemplate<String, Object> kafkaTemplate(
      ProducerFactory<String, Object> producerFactory) {
    return new KafkaTemplate<>(producerFactory);
  }

  @Bean
  public NewTopic orderTopic() {
    return TopicBuilder.name(this.orderTopic)
        .partitions(PARTITIONS)
        .replicas(REPLICAS)
        .build();
  }
}
