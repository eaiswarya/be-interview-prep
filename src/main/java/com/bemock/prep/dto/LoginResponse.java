package com.bemock.prep.dto;

/** {@code expiresIn} is in seconds, following the OAuth2 token response convention. */
public record LoginResponse(String accessToken, String tokenType, long expiresIn) {
}
