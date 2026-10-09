package com.bemock.prep.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** {@code app.admin.*}: optional bootstrap admin, from the ADMIN_EMAIL / ADMIN_PASSWORD env vars. */
@ConfigurationProperties("app.admin")
public record AdminProperties(String email, String password) {
}
