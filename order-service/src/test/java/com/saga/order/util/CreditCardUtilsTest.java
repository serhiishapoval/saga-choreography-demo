package com.saga.order.util;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CreditCardUtilsTest {

  @Test
  void givenNoInput_whenGetCreditCardNumber_thenReturnsFourGroupsOfFourDigits() {
    // given
    final String groupedFormat = "\\d{4} \\d{4} \\d{4} \\d{4}";

    // when
    final String creditCardNumber = CreditCardUtils.getCreditCardNumber();

    // then
    assertTrue(creditCardNumber.matches(groupedFormat));
  }
}
