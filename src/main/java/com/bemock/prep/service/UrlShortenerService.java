package com.bemock.prep.service;

import com.bemock.prep.dto.ShortUrlResponse;
import com.bemock.prep.dto.ShortenUrlRequest;
import com.bemock.prep.dto.UrlStatsResponse;
import com.bemock.prep.exception.GoneException;
import com.bemock.prep.exception.ResourceNotFoundException;
import com.bemock.prep.model.ShortUrl;
import com.bemock.prep.repository.ShortUrlRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class UrlShortenerService {

    private static final String BASE62 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int CODE_LENGTH = 7;
    private static final int MAX_ATTEMPTS = 5;

    private final SecureRandom random = new SecureRandom();
    private final ShortUrlRepository shortUrlRepository;

    /**
     * Every call creates a new code, even for a URL shortened before, so each link has its own
     * expiry and stats. Uniqueness is guaranteed by the DB unique index; a collision just retries.
     * Not {@code @Transactional}: each attempt is its own insert, so a failed one doesn't poison the next.
     */
    public ShortUrlResponse shorten(ShortenUrlRequest request, String baseUrl) {
        for (int attempt = 1; ; attempt++) {
            ShortUrl shortUrl = new ShortUrl();
            shortUrl.setCode(randomCode());
            shortUrl.setOriginalUrl(request.url());
            shortUrl.setExpiresAt(request.expiresAt());
            try {
                ShortUrl saved = shortUrlRepository.saveAndFlush(shortUrl);
                return new ShortUrlResponse(saved.getCode(), baseUrl + "/" + saved.getCode(),
                        saved.getOriginalUrl(), saved.getExpiresAt());
            } catch (DataIntegrityViolationException collision) {
                if (attempt == MAX_ATTEMPTS) {
                    throw collision;
                }
            }
        }
    }

    /** Resolves a code to its original URL and counts the visit. */
    @Transactional
    public String resolve(String code) {
        ShortUrl shortUrl = findByCode(code);
        if (shortUrl.isExpired(Instant.now())) {
            throw new GoneException("Short URL '%s' has expired".formatted(code));
        }
        shortUrlRepository.incrementVisitCount(shortUrl.getId());
        return shortUrl.getOriginalUrl();
    }

    @Transactional(readOnly = true)
    public UrlStatsResponse stats(String code) {
        return UrlStatsResponse.from(findByCode(code));
    }

    private ShortUrl findByCode(String code) {
        return shortUrlRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Short URL '%s' not found".formatted(code)));
    }

    private String randomCode() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(BASE62.charAt(random.nextInt(BASE62.length())));
        }
        return code.toString();
    }
}
