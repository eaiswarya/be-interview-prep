package com.bemock.prep.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "short_url")
public class ShortUrl extends BaseEntity {

    @Column(nullable = false, unique = true, length = 8)
    private String code;

    @Column(nullable = false)
    private String originalUrl;

    private Instant expiresAt;

    /** Only ever changed by the atomic increment in {@code ShortUrlRepository}. */
    @Column(nullable = false, insertable = false, updatable = false)
    private long visitCount;

    public boolean isExpired(Instant now) {
        return expiresAt != null && !expiresAt.isAfter(now);
    }
}
