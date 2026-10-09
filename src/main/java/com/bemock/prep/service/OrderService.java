package com.bemock.prep.service;

import com.bemock.prep.config.CacheConfig;
import com.bemock.prep.dto.OrderRequest;
import com.bemock.prep.dto.OrderResponse;
import com.bemock.prep.dto.PlacedOrder;
import com.bemock.prep.exception.ConflictException;
import com.bemock.prep.exception.ResourceNotFoundException;
import com.bemock.prep.model.Order;
import com.bemock.prep.model.OrderItem;
import com.bemock.prep.model.OrderStatus;
import com.bemock.prep.repository.OrderRepository;
import com.bemock.prep.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final TransactionTemplate transactionTemplate;
    private final CacheManager cacheManager;

    /**
     * Idempotent: a retry with the same key returns the existing order instead of creating another.
     * The unique (user_id, idempotency_key) index is the arbiter for simultaneous retries: the loser's
     * insert waits for the winner's commit, fails, and then reads the winner's order.
     */
    public PlacedOrder place(Long userId, String idempotencyKey, OrderRequest request) {
        var existing = orderRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey);
        if (existing.isPresent()) {
            return new PlacedOrder(OrderResponse.from(existing.get()), false);
        }
        try {
            OrderResponse created = transactionTemplate.execute(status -> create(userId, idempotencyKey, request));
            return new PlacedOrder(Objects.requireNonNull(created), true);
        } catch (DataIntegrityViolationException duplicate) {
            return orderRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey)
                    .map(order -> new PlacedOrder(OrderResponse.from(order), false))
                    .orElseThrow(() -> duplicate);
        }
    }

    @Transactional(readOnly = true)
    public OrderResponse get(Long userId, Long orderId) {
        return OrderResponse.from(orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId)));
    }

    /** The row lock makes a second, simultaneous cancel wait and then see CANCELLED, so stock is returned once. */
    @Transactional
    public OrderResponse cancel(Long userId, Long orderId) {
        Order order = orderRepository.findWithLockByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new ConflictException("Order %d is already cancelled".formatted(orderId));
        }
        order.setStatus(OrderStatus.CANCELLED);
        for (OrderItem item : order.getItems()) {
            productRepository.releaseStock(item.getProductId(), item.getQuantity());
            evictProduct(item.getProductId());
        }
        return OrderResponse.from(order);
    }

    /**
     * Runs in one transaction: any item failing throws, which rolls back every reservation (all-or-nothing).
     * The order row is inserted first so a duplicate key fails before any stock is touched.
     */
    private OrderResponse create(Long userId, String idempotencyKey, OrderRequest request) {
        Order order = new Order();
        order.setUserId(userId);
        order.setIdempotencyKey(idempotencyKey);
        order.setStatus(OrderStatus.PLACED);
        orderRepository.saveAndFlush(order);

        // Merge duplicate lines and reserve in ascending product id, so two orders touching the same
        // products always lock rows in the same order and cannot deadlock.
        Map<Long, Integer> quantities = request.items().stream().collect(Collectors.groupingBy(
                OrderRequest.Item::productId, TreeMap::new, Collectors.summingInt(OrderRequest.Item::quantity)));
        quantities.forEach((productId, quantity) -> {
            if (productRepository.reserveStock(productId, quantity) == 0) {
                if (!productRepository.existsById(productId)) {
                    throw new ResourceNotFoundException("Product", productId);
                }
                throw new ConflictException("Insufficient stock for product %d".formatted(productId));
            }
            order.addItem(productId, quantity);
            evictProduct(productId);
        });
        return OrderResponse.from(orderRepository.saveAndFlush(order));
    }

    /** Stock changed, so the cached product (Q4) is stale; the eviction runs after commit. */
    private void evictProduct(Long productId) {
        Cache products = cacheManager.getCache(CacheConfig.PRODUCTS);
        if (products != null) {
            products.evict(productId);
        }
    }
}
