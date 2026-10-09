package com.bemock.prep.controller;

import com.bemock.prep.dto.PageResponse;
import com.bemock.prep.dto.ProductRequest;
import com.bemock.prep.dto.ProductResponse;
import com.bemock.prep.service.ProductService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * All filters optional and combinable. Paging/sorting via {@code page}, {@code size}, {@code sort=field,dir};
     * size is capped at 100 by {@code spring.data.web.pageable.max-page-size}.
     */
    @GetMapping
    public PageResponse<ProductResponse> list(@RequestParam(required = false) String category,
                                              @RequestParam(required = false) @PositiveOrZero BigDecimal minPrice,
                                              @RequestParam(required = false) @PositiveOrZero BigDecimal maxPrice,
                                              @RequestParam(defaultValue = "false") boolean inStock,
                                              @RequestParam(required = false) String q,
                                              @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return productService.list(category, minPrice, maxPrice, inStock, q, pageable);
    }

    @GetMapping("/{id}")
    public ProductResponse get(@PathVariable Long id) {
        return productService.get(id);
    }

    /** ADMIN only, enforced in {@code SecurityConfig}. */
    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return productService.update(id, request);
    }

    /** ADMIN only, enforced in {@code SecurityConfig}. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
