package com.bemock.prep;

import com.bemock.prep.dto.LoginResponse;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;

import java.util.Map;
import java.util.UUID;

/** Registers a fresh USER and returns a client that sends its bearer token on every request. */
public final class AuthTestSupport {

    public static final String PASSWORD = "password123";

    /** Matches app.admin.* in src/test/resources/config/application.yml. */
    private static final String ADMIN_EMAIL = "admin@test.local";
    private static final String ADMIN_PASSWORD = "admin-password-123";

    private AuthTestSupport() {
    }

    public static String registerAndLogin(TestRestTemplate anonymous) {
        String email = "user-" + UUID.randomUUID() + "@example.com";
        anonymous.postForEntity("/api/auth/register", Map.of("email", email, "password", PASSWORD), Map.class);
        return login(anonymous, email, PASSWORD);
    }

    public static String adminLogin(TestRestTemplate anonymous) {
        return login(anonymous, ADMIN_EMAIL, ADMIN_PASSWORD);
    }

    public static String login(TestRestTemplate anonymous, String email, String password) {
        return anonymous.postForObject("/api/auth/login", Map.of("email", email, "password", password),
                LoginResponse.class).accessToken();
    }

    public static TestRestTemplate withToken(RestTemplateBuilder builder, int port, String token) {
        return new TestRestTemplate(builder
                .rootUri("http://localhost:" + port)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }
}
