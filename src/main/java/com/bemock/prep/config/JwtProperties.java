package com.bemock.prep.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** {@code app.jwt.*}: the secret comes from the JWT_SECRET env var and is never committed. */
@ConfigurationProperties("app.jwt")
public record JwtProperties(String secret, Duration ttl) {
}
