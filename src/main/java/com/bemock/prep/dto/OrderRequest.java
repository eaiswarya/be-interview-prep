package com.bemock.prep.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record OrderRequest(@NotEmpty @Size(max = 50) List<@Valid @NotNull Item> items) {

    public record Item(@NotNull Long productId, @NotNull @Positive @Max(1000) Integer quantity) {
    }
}
