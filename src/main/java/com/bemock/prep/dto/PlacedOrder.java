package com.bemock.prep.dto;

/** {@code created} is false when the request was a retry of an order that already exists. */
public record PlacedOrder(OrderResponse order, boolean created) {
}
