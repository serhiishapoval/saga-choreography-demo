package com.saga.dto.order;

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
public class CancelOrderAction extends BaseTracing {

  private UUID orderId;
  private UUID productId;
  private Integer quantity;
}
