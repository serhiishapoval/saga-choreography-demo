package com.saga.product.service.impl;

import com.saga.product.dao.ProductRepository;
import com.saga.product.entity.ProductEntity;
import com.saga.product.service.ProductService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

  private final ProductRepository productRepository;

  private final KafkaTemplate<String, Object> kafkaTemplate;

  @Override
  public List<ProductEntity> getAllProducts() {
    return this.productRepository.findAll();
  }

  @Override
  public void buyProduct(final UUID productId, final Integer quantity) {
    final ProductEntity product = this.productRepository.findById(productId).orElse(null);

    if (product != null) {
      product.setQuantity(product.getQuantity() - quantity);
      this.productRepository.save(product);
    } else {
      log.info("Product not found with id {}", productId);
    }
  }

  @Override
  public void rollbackProduct(final UUID productId, final Integer quantity) {
    final ProductEntity product = this.productRepository.findById(productId).orElse(null);

    if (product != null) {
      product.setQuantity(product.getQuantity() + quantity);
      this.productRepository.save(product);
    } else {
      log.info("Product not found with id {}", productId);
    }
  }
}
