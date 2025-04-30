package com.saga.product.dto;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BuySomethingRequest {

  private UUID productId;
  private Integer quantity;
}
