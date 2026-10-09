package com.bemock.prep;

import com.bemock.prep.dto.OrderResponse;
import com.bemock.prep.model.Product;
import com.bemock.prep.repository.OrderRepository;
import com.bemock.prep.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderServiceIntegrationTest {

    @Autowired
    private TestRestTemplate anonymous;

    @Autowired
    private RestTemplateBuilder restTemplateBuilder;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @LocalServerPort
    private int port;

    private TestRestTemplate user;

    @BeforeEach
    void authenticate() {
        user = AuthTestSupport.withToken(restTemplateBuilder, port, AuthTestSupport.registerAndLogin(anonymous));
    }

    @Test
    void fiftySimultaneousOrdersForStockOfTenSellExactlyTen() throws Exception {
        long productId = 50;
        setStock(productId, 10);
        int customers = 50;
        ExecutorService pool = Executors.newFixedThreadPool(customers);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<HttpStatusCode>> results = new ArrayList<>();
        for (int i = 0; i < customers; i++) {
            results.add(pool.submit(() -> {
                start.await();
                return placeOrder(UUID.randomUUID().toString(), productId, 1).getStatusCode();
            }));
        }
        start.countDown();
        List<HttpStatusCode> statuses = new ArrayList<>();
        for (Future<HttpStatusCode> result : results) {
            statuses.add(result.get());
        }
        pool.shutdown();

        assertThat(statuses).filteredOn(HttpStatus.CREATED::equals).hasSize(10);
        assertThat(statuses).filteredOn(HttpStatus.CONFLICT::equals).hasSize(40);
        assertThat(stockOf(productId)).isZero();
    }

    @Test
    void retryingTheSameRequestCreatesOnlyOneOrder() {
        long productId = 51;
        setStock(productId, 5);
        String key = UUID.randomUUID().toString();

        ResponseEntity<Map> first = placeOrder(key, productId, 2);
        ResponseEntity<Map> retry = placeOrder(key, productId, 2);

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(retry.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(retry.getBody().get("id")).isEqualTo(first.getBody().get("id"));
        assertThat(orderRepository.countByIdempotencyKey(key)).isEqualTo(1);
        assertThat(stockOf(productId)).isEqualTo(3);
    }

    @Test
    void simultaneousRetriesWithTheSameKeyCreateOnlyOneOrder() throws Exception {
        long productId = 52;
        setStock(productId, 100);
        String key = UUID.randomUUID().toString();
        int retries = 10;
        ExecutorService pool = Executors.newFixedThreadPool(retries);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<ResponseEntity<Map>>> results = new ArrayList<>();
        for (int i = 0; i < retries; i++) {
            results.add(pool.submit(() -> {
                start.await();
                return placeOrder(key, productId, 1);
            }));
        }
        start.countDown();
        List<Object> orderIds = new ArrayList<>();
        for (Future<ResponseEntity<Map>> result : results) {
            assertThat(result.get().getStatusCode().is2xxSuccessful()).isTrue();
            orderIds.add(result.get().getBody().get("id"));
        }
        pool.shutdown();

        assertThat(orderIds).containsOnly(orderIds.getFirst());
        assertThat(orderRepository.countByIdempotencyKey(key)).isEqualTo(1);
        assertThat(stockOf(productId)).isEqualTo(99);
    }

    @Test
    void orderIsAllOrNothing() {
        setStock(53, 5);
        setStock(54, 1);

        ResponseEntity<Map> response = post(UUID.randomUUID().toString(), Map.of("items", List.of(
                Map.of("productId", 53, "quantity", 2),
                Map.of("productId", 54, "quantity", 3))));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat((String) response.getBody().get("message")).contains("Insufficient stock for product 54");
        assertThat(stockOf(53)).isEqualTo(5);
        assertThat(stockOf(54)).isEqualTo(1);
    }

    @Test
    void cancelReturnsStockAndCannotBeRepeated() {
        long productId = 55;
        setStock(productId, 4);
        OrderResponse order = user.exchange("/api/orders", HttpMethod.POST,
                request(UUID.randomUUID().toString(), Map.of("items", List.of(Map.of("productId", productId, "quantity", 3)))),
                OrderResponse.class).getBody();
        assertThat(stockOf(productId)).isEqualTo(1);

        ResponseEntity<OrderResponse> cancelled = user.postForEntity("/api/orders/" + order.id() + "/cancel", null,
                OrderResponse.class);
        assertThat(cancelled.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(cancelled.getBody().status()).hasToString("CANCELLED");
        assertThat(stockOf(productId)).isEqualTo(4);

        assertThat(user.postForEntity("/api/orders/" + order.id() + "/cancel", null, Map.class).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        assertThat(stockOf(productId)).isEqualTo(4);
    }

    @Test
    void invalidRequestsAreRejected() {
        assertThat(placeOrder(UUID.randomUUID().toString(), 999_999, 1).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(placeOrder(UUID.randomUUID().toString(), 56, 0).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(post(UUID.randomUUID().toString(), Map.of("items", List.of())).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(user.postForEntity("/api/orders",
                Map.of("items", List.of(Map.of("productId", 56, "quantity", 1))), Map.class).getStatusCode())
                .as("missing Idempotency-Key header").isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void usersCannotSeeOtherUsersOrders() {
        setStock(57, 5);
        Object orderId = placeOrder(UUID.randomUUID().toString(), 57, 1).getBody().get("id");
        TestRestTemplate otherUser = AuthTestSupport.withToken(restTemplateBuilder, port,
                AuthTestSupport.registerAndLogin(anonymous));

        assertThat(user.getForEntity("/api/orders/" + orderId, Map.class).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(otherUser.getForEntity("/api/orders/" + orderId, Map.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    private ResponseEntity<Map> placeOrder(String key, long productId, int quantity) {
        return post(key, Map.of("items", List.of(Map.of("productId", productId, "quantity", quantity))));
    }

    private ResponseEntity<Map> post(String key, Map<String, Object> body) {
        return user.postForEntity("/api/orders", request(key, body), Map.class);
    }

    private static HttpEntity<Map<String, Object>> request(String key, Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Idempotency-Key", key);
        return new HttpEntity<>(body, headers);
    }

    private void setStock(long productId, int stock) {
        Product product = productRepository.findById(productId).orElseThrow();
        product.setStock(stock);
        productRepository.save(product);
    }

    private int stockOf(long productId) {
        return productRepository.findById(productId).orElseThrow().getStock();
    }
}
