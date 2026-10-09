package com.bemock.prep;

import com.bemock.prep.dto.LoginResponse;
import com.bemock.prep.dto.UserResponse;
import com.bemock.prep.model.Role;
import com.bemock.prep.model.User;
import com.bemock.prep.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthIntegrationTest {

    @Autowired
    private TestRestTemplate anonymous;

    @Autowired
    private RestTemplateBuilder restTemplateBuilder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtDecoder jwtDecoder;

    @LocalServerPort
    private int port;

    @Test
    void registerStoresHashedPasswordAndRejectsDuplicateEmail() {
        String email = uniqueEmail();
        Map<String, String> body = Map.of("email", email, "password", "password123");

        ResponseEntity<UserResponse> created = anonymous.postForEntity("/api/auth/register", body, UserResponse.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody().role()).isEqualTo(Role.USER);

        User stored = userRepository.findByEmail(email).orElseThrow();
        assertThat(stored.getPasswordHash()).isNotEqualTo("password123").startsWith("$2");

        assertThat(anonymous.postForEntity("/api/auth/register", body, Map.class).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void loginReturnsTokenThatExpiresIn15Minutes() {
        String email = uniqueEmail();
        anonymous.postForEntity("/api/auth/register", Map.of("email", email, "password", "password123"), Map.class);

        LoginResponse login = anonymous.postForObject("/api/auth/login",
                Map.of("email", email, "password", "password123"), LoginResponse.class);

        assertThat(login.expiresIn()).isEqualTo(900);
        Jwt jwt = jwtDecoder.decode(login.accessToken());
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(Duration.ofMinutes(15));
    }

    @Test
    void wrongPasswordReturns401Json() {
        String email = uniqueEmail();
        anonymous.postForEntity("/api/auth/register", Map.of("email", email, "password", "password123"), Map.class);

        ResponseEntity<Map> response = anonymous.postForEntity("/api/auth/login",
                Map.of("email", email, "password", "wrong-password"), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).containsEntry("status", 401);
    }

    @Test
    void userCanViewOwnProfile() {
        String token = AuthTestSupport.registerAndLogin(anonymous);

        ResponseEntity<UserResponse> me = AuthTestSupport.withToken(restTemplateBuilder, port, token)
                .getForEntity("/api/users/me", UserResponse.class);

        assertThat(me.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(me.getBody().email()).startsWith("user-");
    }

    @Test
    void requestWithoutTokenReturns401Json() {
        ResponseEntity<Map> response = anonymous.getForEntity("/api/users/me", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(response.getBody()).containsEntry("status", 401).containsEntry("path", "/api/users/me");
    }

    @Test
    void apiRequiresLoginButHealthIsPublic() {
        assertThat(anonymous.getForEntity("/api/tasks", Map.class).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(anonymous.getForEntity("/actuator/health", Map.class).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void invalidTokenReturns401Json() {
        ResponseEntity<Map> response = AuthTestSupport.withToken(restTemplateBuilder, port, "not.a.jwt")
                .getForEntity("/api/users/me", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).containsEntry("status", 401);
    }

    @Test
    void userCannotListAllUsers() {
        String token = AuthTestSupport.registerAndLogin(anonymous);

        ResponseEntity<Map> response = AuthTestSupport.withToken(restTemplateBuilder, port, token)
                .getForEntity("/api/users", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(response.getBody()).containsEntry("status", 403);
    }

    @Test
    void adminCanListAllUsers() {
        String email = uniqueEmail();
        User admin = new User();
        admin.setEmail(email);
        admin.setPasswordHash(passwordEncoder.encode("admin-password"));
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);
        String token = AuthTestSupport.login(anonymous, email, "admin-password");

        ResponseEntity<UserResponse[]> response = AuthTestSupport.withToken(restTemplateBuilder, port, token)
                .getForEntity("/api/users", UserResponse[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).extracting(UserResponse::email).contains(email);
    }

    private static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }
}
