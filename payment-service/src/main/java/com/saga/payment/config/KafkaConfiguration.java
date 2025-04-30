package com.saga.payment.config;

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

  @Value("${kafka.topic.name.payment.topic}")
  private String paymentTopic;

  @Bean
  public KafkaTemplate<String, Object> kafkaTemplate(
      ProducerFactory<String, Object> producerFactory) {
    return new KafkaTemplate<>(producerFactory);
  }

  @Bean
  public NewTopic paymentTopic() {
    return TopicBuilder.name(this.paymentTopic)
        .partitions(PARTITIONS)
        .replicas(REPLICAS)
        .build();
  }

}
