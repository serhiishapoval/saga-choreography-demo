package com.saga.payment.service;

import java.util.UUID;

public interface PaymentService {

  UUID pay(UUID orderId, UUID productId, Double amountToBePaid, String creditCardNumber);
}
