package com.saga.log.entity;

import java.time.LocalDateTime;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "kafka_messages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KafkaMessageEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String topic;

  @Column(nullable = false)
  private String messageType;

  @Column(nullable = false)
  private UUID operationId;

  @Column(columnDefinition = "TEXT")
  private String messageContent;

  @Column(nullable = false)
  private LocalDateTime timestamp;

  @Column(nullable = false)
  private String serviceSource;
}
