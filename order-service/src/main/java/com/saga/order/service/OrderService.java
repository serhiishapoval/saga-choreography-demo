package com.saga.order.service;

import com.saga.order.entity.OrderEntity;
import java.util.UUID;

public interface OrderService {

  OrderEntity createOrder(UUID productId, Integer quantity);

  void cancelOrder(UUID orderId);

  void payOrder(UUID orderId);
}
