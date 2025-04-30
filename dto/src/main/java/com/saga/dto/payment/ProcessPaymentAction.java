package com.saga.dto.payment;

import com.saga.dto.trace.BaseTracing;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@NoArgsConstructor
public class ProcessPaymentAction extends BaseTracing {

  private UUID orderId;
  private UUID productId;
  private Double amountToBePaid;
  private String creditCardNumber;
  private Integer quantity;
}
