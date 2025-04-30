package com.saga.product.service;

import com.saga.product.entity.ProductEntity;
import java.util.List;
import java.util.UUID;

public interface ProductService {

  List<ProductEntity> getAllProducts();

  void buyProduct(UUID productId, Integer quantity);

  void rollbackProduct(UUID productId, Integer quantity);
}
