package com.saga.log.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saga.dto.trace.BaseTracing;
import com.saga.log.entity.KafkaMessageEntity;
import com.saga.log.repository.KafkaMessageRepository;
import com.saga.log.service.KafkaLogService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaLogServiceImpl implements KafkaLogService {

  private final KafkaMessageRepository kafkaMessageRepository;
  private final ObjectMapper objectMapper;

  @Override
  public void logKafkaMessage(final String topic, final Object message,
      final String serviceSource) {
    try {
      String messageType = message.getClass().getSimpleName();
      UUID operationId = null;

      if (message instanceof BaseTracing) {
        operationId = ((BaseTracing) message).getOperationId();
      }

      final String messageContent = objectMapper.writeValueAsString(message);

      final KafkaMessageEntity kafkaMessageEntity = KafkaMessageEntity.builder()
          .topic(topic)
          .messageType(messageType)
          .operationId(operationId)
          .messageContent(messageContent)
          .timestamp(LocalDateTime.now())
          .serviceSource(serviceSource)
          .build();

      this.kafkaMessageRepository.save(kafkaMessageEntity);

      log.info("Logged Kafka message: topic={}, messageType={}, operationId={}, serviceSource={}",
          topic, messageType, operationId, serviceSource);
    } catch (Exception e) {
      log.error("Error logging Kafka message: {}", e.getMessage(), e);
    }
  }

  @Override
  public List<KafkaMessageEntity> getMessagesByOperationId(final UUID operationId) {
    return this.kafkaMessageRepository.findByOperationId(operationId);
  }

  @Override
  public List<KafkaMessageEntity> getMessagesByTopic(String topic) {
    return this.kafkaMessageRepository.findByTopic(topic);
  }

  @Override
  public List<KafkaMessageEntity> getMessagesByType(String messageType) {
    return this.kafkaMessageRepository.findByMessageType(messageType);
  }

  @Override
  public List<KafkaMessageEntity> getMessagesByServiceSource(String serviceSource) {
    return this.kafkaMessageRepository.findByServiceSource(serviceSource);
  }
}