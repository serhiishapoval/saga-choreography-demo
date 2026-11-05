package com.saga.payment.service.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

import com.saga.payment.dao.PaymentRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

  @Mock private PaymentRepository paymentRepository;

  @InjectMocks private PaymentServiceImpl paymentService;

  @Test
  void givenPaymentDetails_whenPay_thenThrowsPaymentFailedAndDoesNotSave() {
    // given
    final UUID orderId = UUID.randomUUID();

    // when / then
    assertThatThrownBy(
            () -> this.paymentService.pay(orderId, UUID.randomUUID(), 3.0D, "1234 5678 9012 3456"))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Payment failed");
    verifyNoInteractions(this.paymentRepository);
  }
}
