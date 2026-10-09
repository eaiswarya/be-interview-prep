package com.bemock.prep.dto;

import java.time.Instant;

public record ShortUrlResponse(String code, String shortUrl, String originalUrl, Instant expiresAt) {
}
