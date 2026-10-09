package com.bemock.prep.controller;

import com.bemock.prep.dto.OrderRequest;
import com.bemock.prep.dto.OrderResponse;
import com.bemock.prep.dto.PlacedOrder;
import com.bemock.prep.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /** 201 for a new order; 200 with the same order when the Idempotency-Key was already used (a retry). */
    @PostMapping
    public ResponseEntity<OrderResponse> place(@AuthenticationPrincipal Jwt jwt,
                                               @RequestHeader("Idempotency-Key") @NotBlank @Size(max = 100) String idempotencyKey,
                                               @Valid @RequestBody OrderRequest request) {
        PlacedOrder placed = orderService.place(userId(jwt), idempotencyKey, request);
        if (!placed.created()) {
            return ResponseEntity.ok(placed.order());
        }
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(placed.order().id()).toUri();
        return ResponseEntity.created(location).body(placed.order());
    }

    @GetMapping("/{id}")
    public OrderResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return orderService.get(userId(jwt), id);
    }

    @PostMapping("/{id}/cancel")
    public OrderResponse cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return orderService.cancel(userId(jwt), id);
    }

    private static Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
