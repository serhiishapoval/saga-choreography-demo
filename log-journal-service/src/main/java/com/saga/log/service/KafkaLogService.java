package com.saga.log.service;

import com.saga.log.entity.KafkaMessageEntity;
import java.util.List;
import java.util.UUID;

public interface KafkaLogService {

  void logKafkaMessage(String topic, Object message, String serviceSource);

  List<KafkaMessageEntity> getMessagesByOperationId(UUID operationId);

  List<KafkaMessageEntity> getMessagesByTopic(String topic);

  List<KafkaMessageEntity> getMessagesByType(String messageType);

  List<KafkaMessageEntity> getMessagesByServiceSource(String serviceSource);

}
