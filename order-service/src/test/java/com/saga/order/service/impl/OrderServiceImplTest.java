package com.saga.order.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.saga.order.dao.OrderRepository;
import com.saga.order.entity.OrderEntity;
import com.saga.order.entity.OrderStatus;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

  @Mock private OrderRepository orderRepository;

  @InjectMocks private OrderServiceImpl orderService;

  @Test
  void givenProductAndQuantity_whenCreateOrder_thenSavesNewOrderWithStatusNew() {
    // given
    final UUID productId = UUID.randomUUID();
    final Integer quantity = 3;
    final ArgumentCaptor<OrderEntity> captor = ArgumentCaptor.forClass(OrderEntity.class);
    when(this.orderRepository.save(any(OrderEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // when
    final OrderEntity result = this.orderService.createOrder(productId, quantity);

    // then
    verify(this.orderRepository).save(captor.capture());
    final OrderEntity expected =
        new OrderEntity(null, productId, quantity, OrderStatus.NEW.getStatus());
    assertThat(captor.getValue())
        .usingRecursiveComparison()
        .ignoringFields("orderId")
        .isEqualTo(expected);
    assertThat(captor.getValue().getOrderId()).isNotNull();
    assertThat(result).isEqualTo(captor.getValue());
  }

  @Test
  void givenExistingOrder_whenCancelOrder_thenSavesOrderWithStatusCancelled() {
    // given
    final UUID orderId = UUID.randomUUID();
    final OrderEntity existing =
        new OrderEntity(orderId, UUID.randomUUID(), 2, OrderStatus.NEW.getStatus());
    when(this.orderRepository.findById(orderId)).thenReturn(Optional.of(existing));

    // when
    this.orderService.cancelOrder(orderId);

    // then
    final OrderEntity expected =
        new OrderEntity(orderId, existing.getProductId(), 2, OrderStatus.CANCELLED.getStatus());
    verify(this.orderRepository).save(expected);
  }

  @Test
  void givenMissingOrder_whenCancelOrder_thenDoesNotSave() {
    // given
    final UUID orderId = UUID.randomUUID();
    when(this.orderRepository.findById(orderId)).thenReturn(Optional.empty());

    // when
    this.orderService.cancelOrder(orderId);

    // then
    verify(this.orderRepository, never()).save(any());
  }

  @Test
  void givenExistingOrder_whenPayOrder_thenSavesOrderWithStatusPaid() {
    // given
    final UUID orderId = UUID.randomUUID();
    final OrderEntity existing =
        new OrderEntity(orderId, UUID.randomUUID(), 2, OrderStatus.NEW.getStatus());
    when(this.orderRepository.findById(orderId)).thenReturn(Optional.of(existing));

    // when
    this.orderService.payOrder(orderId);

    // then
    final OrderEntity expected =
        new OrderEntity(orderId, existing.getProductId(), 2, OrderStatus.PAID.getStatus());
    verify(this.orderRepository).save(expected);
  }

  @Test
  void givenMissingOrder_whenPayOrder_thenDoesNotSave() {
    // given
    final UUID orderId = UUID.randomUUID();
    when(this.orderRepository.findById(orderId)).thenReturn(Optional.empty());

    // when
    this.orderService.payOrder(orderId);

    // then
    verify(this.orderRepository, never()).save(any());
  }
}
