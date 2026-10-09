package com.bemock.prep.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.time.Instant;

public record ShortenUrlRequest(
        @NotBlank
        @Size(max = 2048)
        @URL
        @Pattern(regexp = "^https?://.*", message = "must start with http:// or https://")
        String url,
        @Future Instant expiresAt) {
}
