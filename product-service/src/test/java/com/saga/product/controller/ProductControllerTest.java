package com.saga.product.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.saga.dto.product.ProductReserveAction;
import com.saga.product.dto.BuySomethingRequest;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

  private static final String PRODUCT_TOPIC = "product-topic";

  @Mock private KafkaTemplate<String, Object> kafkaTemplate;

  @InjectMocks private ProductController productController;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(this.productController, "productTopic", PRODUCT_TOPIC);
  }

  @Test
  void givenBuyRequest_whenBuy_thenSendsProductReserveActionAndReturnsOk() {
    // given
    final UUID productId = UUID.randomUUID();
    final BuySomethingRequest request = new BuySomethingRequest(productId, 3);
    final ArgumentCaptor<ProductReserveAction> captor =
        ArgumentCaptor.forClass(ProductReserveAction.class);

    // when
    final ResponseEntity<Void> response = this.productController.getProducts(request);

    // then
    verify(this.kafkaTemplate).send(eq(PRODUCT_TOPIC), captor.capture());
    final ProductReserveAction sent = captor.getValue();
    assertThat(sent.getOperationId()).isNotNull();
    final ProductReserveAction expected =
        ProductReserveAction.builder()
            .operationId(sent.getOperationId())
            .productId(productId)
            .quantity(3)
            .build();
    assertThat(sent).isEqualTo(expected);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }
}
