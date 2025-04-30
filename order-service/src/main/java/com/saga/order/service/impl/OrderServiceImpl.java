package com.saga.order.service.impl;

import com.saga.order.dao.OrderRepository;
import com.saga.order.entity.OrderEntity;
import com.saga.order.entity.OrderStatus;
import com.saga.order.service.OrderService;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

  private final OrderRepository orderRepository;

  @Override
  public OrderEntity createOrder(final UUID productId, final Integer quantity) {
    final OrderEntity orderEntity = new OrderEntity(UUID.randomUUID(), productId, quantity,
        OrderStatus.NEW.getStatus());

    return this.orderRepository.save(orderEntity);
  }

  @Override
  public void cancelOrder(UUID orderId) {
    final Optional<OrderEntity> optionalOrderEntity = this.orderRepository.findById(orderId);

    if (optionalOrderEntity.isPresent()) {
      final OrderEntity orderEntity = optionalOrderEntity.get();
      orderEntity.setOrderStatus(OrderStatus.CANCELLED.getStatus());
      this.orderRepository.save(orderEntity);
    }
  }

  @Override
  public void payOrder(UUID orderId) {
    final Optional<OrderEntity> optionalOrderEntity = this.orderRepository.findById(orderId);

    if (optionalOrderEntity.isPresent()) {
      final OrderEntity orderEntity = optionalOrderEntity.get();
      orderEntity.setOrderStatus(OrderStatus.PAID.getStatus());
      this.orderRepository.save(orderEntity);
    }
  }
}
