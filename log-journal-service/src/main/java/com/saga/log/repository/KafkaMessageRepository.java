package com.saga.log.repository;

import com.saga.log.entity.KafkaMessageEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KafkaMessageRepository extends JpaRepository<KafkaMessageEntity, Long> {
  
  List<KafkaMessageEntity> findByOperationId(UUID operationId);
  
  List<KafkaMessageEntity> findByTopic(String topic);
  
  List<KafkaMessageEntity> findByMessageType(String messageType);
  
  List<KafkaMessageEntity> findByServiceSource(String serviceSource);
}