package com.bemock.prep;

import com.bemock.prep.dto.PageResponse;
import com.bemock.prep.dto.ProductResponse;
import com.bemock.prep.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductCatalogIntegrationTest {

    private static final ParameterizedTypeReference<PageResponse<ProductResponse>> PAGE =
            new ParameterizedTypeReference<>() {
            };

    @Autowired
    private TestRestTemplate anonymous;

    @Autowired
    private RestTemplateBuilder restTemplateBuilder;

    /** Spy on the real repository to count how often a lookup actually reaches the database. */
    @MockitoSpyBean
    private ProductRepository productRepository;

    @LocalServerPort
    private int port;

    private TestRestTemplate user;

    @BeforeEach
    void authenticate() {
        user = AuthTestSupport.withToken(restTemplateBuilder, port, AuthTestSupport.registerAndLogin(anonymous));
    }

    @Test
    void listReturnsPageMetadata() {
        PageResponse<ProductResponse> page = list("/api/products?page=0&size=10");

        assertThat(page.content()).hasSize(10);
        assertThat(page.size()).isEqualTo(10);
        assertThat(page.totalElements()).isGreaterThanOrEqualTo(90);
        assertThat(page.totalPages()).isEqualTo((int) Math.ceil(page.totalElements() / 10.0));
    }

    @Test
    void allFiltersCombineInOneRequest() {
        PageResponse<ProductResponse> page = list(
                "/api/products?category=Electronics&minPrice=50&maxPrice=400&inStock=true&q=product 0&size=100");

        assertThat(page.content()).isNotEmpty().allSatisfy(product -> {
            assertThat(product.category()).isEqualTo("Electronics");
            assertThat(product.price()).isBetween(new BigDecimal("50"), new BigDecimal("400"));
            assertThat(product.stock()).isPositive();
            assertThat(product.name().toLowerCase()).contains("product 0");
        });
    }

    @Test
    void sortsByAnyField() {
        PageResponse<ProductResponse> page = list("/api/products?sort=price,desc&size=50");

        assertThat(page.content()).extracting(ProductResponse::price)
                .isSortedAccordingTo(Comparator.reverseOrder());
    }

    @Test
    void pageSizeIsCappedAt100() {
        assertThat(list("/api/products?size=500").size()).isEqualTo(100);
    }

    @Test
    void invalidSortOrFilterReturns400() {
        assertThat(user.getForEntity("/api/products?sort=nope", Map.class).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(user.getForEntity("/api/products?minPrice=-1", Map.class).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(user.getForEntity("/api/products?minPrice=10&maxPrice=5", Map.class).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void repeatedLookupsAreCachedAndUpdatesAreNeverStale() {
        long id = 1;
        clearInvocations(productRepository);

        ProductResponse first = user.getForObject("/api/products/" + id, ProductResponse.class);
        user.getForObject("/api/products/" + id, ProductResponse.class);
        user.getForObject("/api/products/" + id, ProductResponse.class);
        verify(productRepository, times(1)).findById(id);

        TestRestTemplate admin = adminClient();
        Map<String, Object> update = Map.of("name", "Renamed product", "category", first.category(),
                "price", first.price(), "stock", first.stock(), "rating", first.rating());
        assertThat(admin.exchange("/api/products/" + id, HttpMethod.PUT, new HttpEntity<>(update),
                ProductResponse.class).getStatusCode()).isEqualTo(HttpStatus.OK);

        assertThat(user.getForObject("/api/products/" + id, ProductResponse.class).name())
                .isEqualTo("Renamed product");
    }

    @Test
    void deletedProductIsNotServedFromCache() {
        long id = 2;
        assertThat(user.getForEntity("/api/products/" + id, ProductResponse.class).getStatusCode())
                .isEqualTo(HttpStatus.OK);

        assertThat(adminClient().exchange("/api/products/" + id, HttpMethod.DELETE, HttpEntity.EMPTY, Void.class)
                .getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        assertThat(user.getForEntity("/api/products/" + id, Map.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void userCannotModifyProducts() {
        assertThat(user.exchange("/api/products/3", HttpMethod.DELETE, HttpEntity.EMPTY, Map.class).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    private PageResponse<ProductResponse> list(String url) {
        ResponseEntity<PageResponse<ProductResponse>> response = user.exchange(url, HttpMethod.GET, null, PAGE);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    private TestRestTemplate adminClient() {
        return AuthTestSupport.withToken(restTemplateBuilder, port, AuthTestSupport.adminLogin(anonymous));
    }
}
