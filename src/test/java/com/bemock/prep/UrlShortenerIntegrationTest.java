package com.bemock.prep;

import com.bemock.prep.dto.ShortUrlResponse;
import com.bemock.prep.dto.UrlStatsResponse;
import com.bemock.prep.model.ShortUrl;
import com.bemock.prep.repository.ShortUrlRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UrlShortenerIntegrationTest {

    private static final String LONG_URL = "https://example.com/some/very/long/path?q=1";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ShortUrlRepository shortUrlRepository;

    @LocalServerPort
    private int port;

    /** JDK client never follows redirects by default, so we can assert the 302 itself. */
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Test
    void shortenRedirectAndCountVisits() throws Exception {
        ShortUrlResponse created = shorten(Map.of("url", LONG_URL));
        assertThat(created.code()).matches("[0-9A-Za-z]{1,8}");
        assertThat(created.shortUrl()).endsWith("/" + created.code());

        HttpResponse<Void> redirect = visit(created.code());
        assertThat(redirect.statusCode()).isEqualTo(302);
        assertThat(redirect.headers().firstValue("Location")).hasValue(LONG_URL);

        UrlStatsResponse stats = restTemplate.getForObject(
                "/api/urls/" + created.code() + "/stats", UrlStatsResponse.class);
        assertThat(stats.originalUrl()).isEqualTo(LONG_URL);
        assertThat(stats.visitCount()).isEqualTo(1);
        assertThat(stats.createdAt()).isNotNull();
    }

    @Test
    void shorteningSameUrlTwiceGivesDistinctCodes() {
        assertThat(shorten(Map.of("url", LONG_URL)).code())
                .isNotEqualTo(shorten(Map.of("url", LONG_URL)).code());
    }

    @Test
    void concurrentVisitsAreCountedExactly() throws Exception {
        String code = shorten(Map.of("url", LONG_URL)).code();
        int visits = 50;
        ExecutorService pool = Executors.newFixedThreadPool(visits);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();
        for (int i = 0; i < visits; i++) {
            results.add(pool.submit(() -> {
                start.await();
                return visit(code).statusCode();
            }));
        }
        start.countDown();
        for (Future<Integer> result : results) {
            assertThat(result.get()).isEqualTo(302);
        }
        pool.shutdown();

        UrlStatsResponse stats = restTemplate.getForObject("/api/urls/" + code + "/stats", UrlStatsResponse.class);
        assertThat(stats.visitCount()).isEqualTo(visits);
    }

    @Test
    void invalidUrlAndPastExpiryReturn400WithFieldErrors() {
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/urls",
                Map.of("url", "not a url", "expiresAt", Instant.now().minus(1, ChronoUnit.DAYS).toString()),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat((List<Map<String, String>>) response.getBody().get("fieldErrors"))
                .extracting(e -> e.get("field"))
                .contains("url", "expiresAt");
    }

    @Test
    void unknownCodeReturns404() {
        assertThat(restTemplate.getForEntity("/zzzzzzz", Map.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(restTemplate.getForEntity("/api/urls/zzzzzzz/stats", Map.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void expiredCodeReturns410() {
        ShortUrl expired = new ShortUrl();
        expired.setCode("expired1");
        expired.setOriginalUrl(LONG_URL);
        expired.setExpiresAt(Instant.now().minus(1, ChronoUnit.MINUTES));
        shortUrlRepository.save(expired);

        ResponseEntity<Map> response = restTemplate.getForEntity("/expired1", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.GONE);
        assertThat(response.getBody()).containsEntry("status", 410);
    }

    private HttpResponse<Void> visit(String code) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/" + code)).build();
        return httpClient.send(request, HttpResponse.BodyHandlers.discarding());
    }

    private ShortUrlResponse shorten(Map<String, String> body) {
        ResponseEntity<ShortUrlResponse> response = restTemplate.postForEntity("/api/urls", body, ShortUrlResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }
}
