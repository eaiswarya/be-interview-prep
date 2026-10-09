package com.bemock.prep.dto;

import com.bemock.prep.model.ShortUrl;

import java.time.Instant;

public record UrlStatsResponse(String code, String originalUrl, long visitCount, Instant createdAt, Instant expiresAt) {

    public static UrlStatsResponse from(ShortUrl shortUrl) {
        return new UrlStatsResponse(shortUrl.getCode(), shortUrl.getOriginalUrl(), shortUrl.getVisitCount(),
                shortUrl.getCreatedAt(), shortUrl.getExpiresAt());
    }
}
