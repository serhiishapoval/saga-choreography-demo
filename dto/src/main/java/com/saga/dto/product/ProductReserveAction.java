package com.saga.dto.product;

import com.saga.dto.trace.BaseTracing;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@NoArgsConstructor
public class ProductReserveAction extends BaseTracing {

  private UUID productId;
  private Integer quantity;
}
