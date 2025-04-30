package com.saga.order.util;

import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CreditCardUtils {

  public static String getCreditCardNumber() {
    Random random = new Random();

    // Generate 16 random digits as a stream
    String creditCardNumber = IntStream.range(0, 16)
        .map(i -> random.nextInt(10))
        .mapToObj(String::valueOf)
        .collect(Collectors.joining(""));

    // Group into sets of 4 and join with spaces
    return IntStream.range(0, creditCardNumber.length())
        .filter(i -> i % 4 == 0)
        .mapToObj(start -> creditCardNumber.substring(start,
            Math.min(start + 4, creditCardNumber.length())))
        .collect(Collectors.joining(" "));

  }

}
