package com.saga.dto.product;

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
public class ProductRollbackAction extends BaseTracing {

  private UUID productId;
  private Integer quantity;
}
