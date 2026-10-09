package com.bemock.prep.repository;

import com.bemock.prep.model.Product;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * One small predicate per optional filter. Each returns {@code null} when its filter is absent,
 * which Spring Data ignores, so any combination composes into a single WHERE clause.
 */
public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    public static Specification<Product> hasCategory(String category) {
        return (root, query, cb) -> StringUtils.hasText(category) ? cb.equal(root.get("category"), category) : null;
    }

    public static Specification<Product> priceAtLeast(BigDecimal min) {
        return (root, query, cb) -> min == null ? null : cb.greaterThanOrEqualTo(root.get("price"), min);
    }

    public static Specification<Product> priceAtMost(BigDecimal max) {
        return (root, query, cb) -> max == null ? null : cb.lessThanOrEqualTo(root.get("price"), max);
    }

    public static Specification<Product> inStockOnly(boolean inStock) {
        return (root, query, cb) -> inStock ? cb.greaterThan(root.get("stock"), 0) : null;
    }

    /** Case-insensitive "name contains"; user input is escaped so % and _ match literally. */
    public static Specification<Product> nameContains(String text) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(text)) {
                return null;
            }
            String escaped = text.trim().toLowerCase(Locale.ROOT)
                    .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
            return cb.like(cb.lower(root.get("name")), "%" + escaped + "%", '\\');
        };
    }
}
