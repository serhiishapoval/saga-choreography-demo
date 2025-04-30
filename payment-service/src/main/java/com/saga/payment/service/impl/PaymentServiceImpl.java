package com.saga.payment.service.impl;

import com.saga.payment.dao.PaymentRepository;
import com.saga.payment.entity.PaymentEntity;
import com.saga.payment.service.PaymentService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

  private final PaymentRepository paymentRepository;

  @Override
  public UUID pay(final UUID orderId, final UUID productId, final Double amountToBePaid,
      final String creditCardNumber) {
    log.info("Paying order with id {} for amount {}", orderId, amountToBePaid);
    log.info(".................................................................");
    throw new RuntimeException("Payment failed");
    /*
    log.info("Payment successful for order with id {}", orderId);
    final UUID paymentId = UUID.randomUUID();
    final PaymentEntity paymentEntity = new PaymentEntity(paymentId, productId, orderId,
        amountToBePaid);
    this.paymentRepository.save(paymentEntity);
    log.info("Payment id: {}", paymentId);
    return paymentId;
     */
  }
}
