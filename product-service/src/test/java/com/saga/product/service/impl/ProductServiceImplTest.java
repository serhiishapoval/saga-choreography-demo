package com.saga.product.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.saga.product.dao.ProductRepository;
import com.saga.product.entity.ProductEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

  @Mock private ProductRepository productRepository;

  @Mock private KafkaTemplate<String, Object> kafkaTemplate;

  @InjectMocks private ProductServiceImpl productService;

  @Test
  void givenProducts_whenGetAllProducts_thenReturnsRepositoryResult() {
    // given
    final List<ProductEntity> products =
        List.of(ProductEntity.builder().id(UUID.randomUUID()).name("gadget").quantity(5).build());
    when(this.productRepository.findAll()).thenReturn(products);

    // when
    final List<ProductEntity> result = this.productService.getAllProducts();

    // then
    assertThat(result).isEqualTo(products);
  }

  @ParameterizedTest(name = "stock {0}, buying {1} leaves {2}")
  @CsvSource({"10, 3, 7", "5, 5, 0", "1, 1, 0"})
  void givenExistingProduct_whenBuyProduct_thenSavesReducedQuantity(
      final int stock, final int bought, final int remaining) {
    // given
    final UUID productId = UUID.randomUUID();
    final ProductEntity product =
        ProductEntity.builder().id(productId).name("gadget").quantity(stock).build();
    when(this.productRepository.findById(productId)).thenReturn(Optional.of(product));

    // when
    this.productService.buyProduct(productId, bought);

    // then
    final ProductEntity expected =
        ProductEntity.builder().id(productId).name("gadget").quantity(remaining).build();
    verify(this.productRepository).save(expected);
  }

  @Test
  void givenMissingProduct_whenBuyProduct_thenDoesNotSave() {
    // given
    final UUID productId = UUID.randomUUID();
    when(this.productRepository.findById(productId)).thenReturn(Optional.empty());

    // when
    this.productService.buyProduct(productId, 2);

    // then
    verify(this.productRepository, never()).save(any());
  }

  @ParameterizedTest(name = "stock {0}, rolling back {1} gives {2}")
  @CsvSource({"7, 3, 10", "0, 5, 5", "4, 0, 4"})
  void givenExistingProduct_whenRollbackProduct_thenSavesIncreasedQuantity(
      final int stock, final int returned, final int expectedStock) {
    // given
    final UUID productId = UUID.randomUUID();
    final ProductEntity product =
        ProductEntity.builder().id(productId).name("gadget").quantity(stock).build();
    when(this.productRepository.findById(productId)).thenReturn(Optional.of(product));

    // when
    this.productService.rollbackProduct(productId, returned);

    // then
    final ProductEntity expected =
        ProductEntity.builder().id(productId).name("gadget").quantity(expectedStock).build();
    verify(this.productRepository).save(expected);
  }

  @Test
  void givenMissingProduct_whenRollbackProduct_thenDoesNotSave() {
    // given
    final UUID productId = UUID.randomUUID();
    when(this.productRepository.findById(productId)).thenReturn(Optional.empty());

    // when
    this.productService.rollbackProduct(productId, 2);

    // then
    verify(this.productRepository, never()).save(any());
  }
}
