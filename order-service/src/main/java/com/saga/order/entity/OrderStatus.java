package com.saga.order.entity;

public enum OrderStatus {

  NEW("NEW"),
  PAID("PAID"),
  CANCELLED("CANCELLED");

  private final String status;

  OrderStatus(String status) {
    this.status = status;
  }

  public String getStatus() {
    return status;
  }

}
