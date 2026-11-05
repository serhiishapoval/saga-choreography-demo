package com.saga.product.kafka;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.saga.dto.order.CreateOrderAction;
import com.saga.dto.product.ProductReserveAction;
import com.saga.dto.product.ProductRollbackAction;
import com.saga.product.service.ProductService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ProductHandlerTest {

  private static final String ORDER_TOPIC = "order-topic";

  @Mock private ProductService productService;

  @Mock private KafkaTemplate<String, Object> kafkaTemplate;

  @InjectMocks private ProductHandler productHandler;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(this.productHandler, "orderTopic", ORDER_TOPIC);
  }

  @Test
  void givenProductReserveAction_whenProductBought_thenSendsCreateOrderAction() {
    // given
    final UUID operationId = UUID.randomUUID();
    final UUID productId = UUID.randomUUID();
    final ProductReserveAction action =
        ProductReserveAction.builder()
            .operationId(operationId)
            .productId(productId)
            .quantity(2)
            .build();

    // when
    this.productHandler.handleProductAction(action);

    // then
    verify(this.productService).buyProduct(productId, 2);
    final CreateOrderAction expected =
        CreateOrderAction.builder()
            .operationId(operationId)
            .productId(productId)
            .quantity(2)
            .build();
    verify(this.kafkaTemplate).send(ORDER_TOPIC, expected);
  }

  @Test
  void givenProductReserveAction_whenBuyFails_thenDoesNotSendAndDoesNotThrow() {
    // given
    final UUID productId = UUID.randomUUID();
    final ProductReserveAction action =
        ProductReserveAction.builder()
            .operationId(UUID.randomUUID())
            .productId(productId)
            .quantity(2)
            .build();
    doThrow(new RuntimeException("out of stock"))
        .when(this.productService)
        .buyProduct(productId, 2);

    // when
    this.productHandler.handleProductAction(action);

    // then
    verifyNoInteractions(this.kafkaTemplate);
  }

  @Test
  void givenProductRollbackAction_whenHandled_thenRollsBackProductQuantity() {
    // given
    final UUID productId = UUID.randomUUID();
    final ProductRollbackAction action =
        ProductRollbackAction.builder()
            .operationId(UUID.randomUUID())
            .productId(productId)
            .quantity(4)
            .build();

    // when
    this.productHandler.handleProductAction(action);

    // then
    verify(this.productService).rollbackProduct(productId, 4);
    verifyNoInteractions(this.kafkaTemplate);
  }

  @Test
  void givenProductRollbackActionThatFails_whenHandled_thenDoesNotPropagateException() {
    // given
    final UUID productId = UUID.randomUUID();
    final ProductRollbackAction action =
        ProductRollbackAction.builder()
            .operationId(UUID.randomUUID())
            .productId(productId)
            .quantity(4)
            .build();
    doThrow(new RuntimeException("rollback failed"))
        .when(this.productService)
        .rollbackProduct(productId, 4);

    // when
    this.productHandler.handleProductAction(action);

    // then
    verifyNoInteractions(this.kafkaTemplate);
  }
}
