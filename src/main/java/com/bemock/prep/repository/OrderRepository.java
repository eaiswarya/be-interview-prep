package com.bemock.prep.repository;

import com.bemock.prep.model.Order;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = "items")
    Optional<Order> findByIdAndUserId(Long id, Long userId);

    @EntityGraph(attributePaths = "items")
    Optional<Order> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);

    /** SELECT ... FOR UPDATE: concurrent cancels of the same order run one after another. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "items")
    Optional<Order> findWithLockByIdAndUserId(Long id, Long userId);

    long countByIdempotencyKey(String idempotencyKey);
}
