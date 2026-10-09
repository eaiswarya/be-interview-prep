package com.bemock.prep.repository;

import com.bemock.prep.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    /**
     * Check-and-decrement in one atomic statement: the row lock taken by UPDATE makes concurrent
     * reservations for the same product run one after another, and the WHERE re-checks stock each time.
     * Returns 0 when there is not enough stock (or the product doesn't exist).
     */
    @Modifying
    @Query("UPDATE Product p SET p.stock = p.stock - :quantity WHERE p.id = :id AND p.stock >= :quantity")
    int reserveStock(Long id, int quantity);

    @Modifying
    @Query("UPDATE Product p SET p.stock = p.stock + :quantity WHERE p.id = :id")
    int releaseStock(Long id, int quantity);
}
