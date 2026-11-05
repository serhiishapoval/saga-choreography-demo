package com.saga.log.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saga.dto.order.CreateOrderAction;
import com.saga.log.entity.KafkaMessageEntity;
import com.saga.log.repository.KafkaMessageRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KafkaLogServiceImplTest {

  private static final String SERVICE_SOURCE = "log-journal-service";

  @Mock private KafkaMessageRepository kafkaMessageRepository;

  @Mock private ObjectMapper objectMapper;

  @InjectMocks private KafkaLogServiceImpl kafkaLogService;

  @Test
  void givenTracedMessage_whenLogKafkaMessage_thenSavesEntityWithOperationIdAndType()
      throws JsonProcessingException {
    // given
    final UUID operationId = UUID.randomUUID();
    final CreateOrderAction message =
        CreateOrderAction.builder()
            .operationId(operationId)
            .productId(UUID.randomUUID())
            .quantity(2)
            .build();
    when(this.objectMapper.writeValueAsString(message)).thenReturn("{\"quantity\":2}");
    final ArgumentCaptor<KafkaMessageEntity> captor =
        ArgumentCaptor.forClass(KafkaMessageEntity.class);

    // when
    this.kafkaLogService.logKafkaMessage("order-topic", message, SERVICE_SOURCE);

    // then
    verify(this.kafkaMessageRepository).save(captor.capture());
    final KafkaMessageEntity expected =
        KafkaMessageEntity.builder()
            .topic("order-topic")
            .messageType("CreateOrderAction")
            .operationId(operationId)
            .messageContent("{\"quantity\":2}")
            .serviceSource(SERVICE_SOURCE)
            .build();
    assertThat(captor.getValue())
        .usingRecursiveComparison()
        .ignoringFields("timestamp")
        .isEqualTo(expected);
    assertThat(captor.getValue().getTimestamp()).isNotNull();
  }

  @Test
  void givenNonTracedMessage_whenLogKafkaMessage_thenSavesEntityWithoutOperationId()
      throws JsonProcessingException {
    // given
    final String message = "plain message";
    when(this.objectMapper.writeValueAsString(message)).thenReturn("\"plain message\"");
    final ArgumentCaptor<KafkaMessageEntity> captor =
        ArgumentCaptor.forClass(KafkaMessageEntity.class);

    // when
    this.kafkaLogService.logKafkaMessage("payment-topic", message, SERVICE_SOURCE);

    // then
    verify(this.kafkaMessageRepository).save(captor.capture());
    final KafkaMessageEntity expected =
        KafkaMessageEntity.builder()
            .topic("payment-topic")
            .messageType("String")
            .operationId(null)
            .messageContent("\"plain message\"")
            .serviceSource(SERVICE_SOURCE)
            .build();
    assertThat(captor.getValue())
        .usingRecursiveComparison()
        .ignoringFields("timestamp")
        .isEqualTo(expected);
  }

  @Test
  void givenSerializationFails_whenLogKafkaMessage_thenDoesNotSaveAndDoesNotThrow()
      throws JsonProcessingException {
    // given
    final String message = "broken";
    when(this.objectMapper.writeValueAsString(message))
        .thenThrow(new JsonProcessingException("boom") {});

    // when
    this.kafkaLogService.logKafkaMessage("product-topic", message, SERVICE_SOURCE);

    // then
    verify(this.kafkaMessageRepository, never()).save(any());
  }

  @Test
  void givenOperationId_whenGetMessagesByOperationId_thenReturnsRepositoryResult() {
    // given
    final UUID operationId = UUID.randomUUID();
    final List<KafkaMessageEntity> stored =
        List.of(KafkaMessageEntity.builder().operationId(operationId).build());
    when(this.kafkaMessageRepository.findByOperationId(operationId)).thenReturn(stored);

    // when
    final List<KafkaMessageEntity> result =
        this.kafkaLogService.getMessagesByOperationId(operationId);

    // then
    assertThat(result).isEqualTo(stored);
  }

  @Test
  void givenTopic_whenGetMessagesByTopic_thenReturnsRepositoryResult() {
    // given
    final List<KafkaMessageEntity> stored =
        List.of(KafkaMessageEntity.builder().topic("order-topic").build());
    when(this.kafkaMessageRepository.findByTopic("order-topic")).thenReturn(stored);

    // when
    final List<KafkaMessageEntity> result = this.kafkaLogService.getMessagesByTopic("order-topic");

    // then
    assertThat(result).isEqualTo(stored);
  }

  @Test
  void givenMessageType_whenGetMessagesByType_thenReturnsRepositoryResult() {
    // given
    final List<KafkaMessageEntity> stored =
        List.of(KafkaMessageEntity.builder().messageType("CreateOrderAction").build());
    when(this.kafkaMessageRepository.findByMessageType("CreateOrderAction")).thenReturn(stored);

    // when
    final List<KafkaMessageEntity> result =
        this.kafkaLogService.getMessagesByType("CreateOrderAction");

    // then
    assertThat(result).isEqualTo(stored);
  }

  @Test
  void givenServiceSource_whenGetMessagesByServiceSource_thenReturnsRepositoryResult() {
    // given
    final List<KafkaMessageEntity> stored =
        List.of(KafkaMessageEntity.builder().serviceSource(SERVICE_SOURCE).build());
    when(this.kafkaMessageRepository.findByServiceSource(SERVICE_SOURCE)).thenReturn(stored);

    // when
    final List<KafkaMessageEntity> result =
        this.kafkaLogService.getMessagesByServiceSource(SERVICE_SOURCE);

    // then
    assertThat(result).isEqualTo(stored);
  }
}
