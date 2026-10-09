package com.bemock.prep.service;

import com.bemock.prep.config.CacheConfig;
import com.bemock.prep.dto.PageResponse;
import com.bemock.prep.dto.ProductRequest;
import com.bemock.prep.dto.ProductResponse;
import com.bemock.prep.exception.BadRequestException;
import com.bemock.prep.exception.ResourceNotFoundException;
import com.bemock.prep.model.Product;
import com.bemock.prep.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static com.bemock.prep.repository.ProductSpecifications.hasCategory;
import static com.bemock.prep.repository.ProductSpecifications.inStockOnly;
import static com.bemock.prep.repository.ProductSpecifications.nameContains;
import static com.bemock.prep.repository.ProductSpecifications.priceAtLeast;
import static com.bemock.prep.repository.ProductSpecifications.priceAtMost;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> list(String category, BigDecimal minPrice, BigDecimal maxPrice,
                                              boolean inStock, String q, Pageable pageable) {
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new BadRequestException("minPrice must not be greater than maxPrice");
        }
        Specification<Product> filters = Specification.allOf(
                hasCategory(category),
                priceAtLeast(minPrice),
                priceAtMost(maxPrice),
                inStockOnly(inStock),
                nameContains(q));
        return PageResponse.from(productRepository.findAll(filters, pageable).map(ProductResponse::from));
    }

    /** Cached by id: only the first lookup hits the DB. Unknown ids throw, so they are never cached. */
    @Cacheable(cacheNames = CacheConfig.PRODUCTS, key = "#id")
    @Transactional(readOnly = true)
    public ProductResponse get(Long id) {
        return ProductResponse.from(findProduct(id));
    }

    /** Evicts rather than puts: the next read reloads the committed row. */
    @CacheEvict(cacheNames = CacheConfig.PRODUCTS, key = "#id")
    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = findProduct(id);
        product.setName(request.name());
        product.setCategory(request.category());
        product.setPrice(request.price());
        product.setStock(request.stock());
        product.setRating(request.rating());
        return ProductResponse.from(product);
    }

    @CacheEvict(cacheNames = CacheConfig.PRODUCTS, key = "#id")
    @Transactional
    public void delete(Long id) {
        productRepository.delete(findProduct(id));
    }

    private Product findProduct(Long id) {
        return productRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Product", id));
    }
}
