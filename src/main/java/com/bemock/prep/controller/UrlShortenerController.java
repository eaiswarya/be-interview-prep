package com.bemock.prep.controller;

import com.bemock.prep.dto.ShortUrlResponse;
import com.bemock.prep.dto.ShortenUrlRequest;
import com.bemock.prep.dto.UrlStatsResponse;
import com.bemock.prep.service.UrlShortenerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequiredArgsConstructor
public class UrlShortenerController {

    private final UrlShortenerService urlShortenerService;

    @PostMapping("/api/urls")
    public ResponseEntity<ShortUrlResponse> shorten(@Valid @RequestBody ShortenUrlRequest request) {
        String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().toUriString();
        ShortUrlResponse created = urlShortenerService.shorten(request, baseUrl);
        return ResponseEntity.created(URI.create(created.shortUrl())).body(created);
    }

    /** 302 (not 301) so browsers don't cache the redirect and every visit reaches us to be counted. */
    @GetMapping("/{code:[0-9A-Za-z]{1,8}}")
    public ResponseEntity<Void> redirect(@PathVariable String code) {
        return ResponseEntity.status(302).location(URI.create(urlShortenerService.resolve(code))).build();
    }

    @GetMapping("/api/urls/{code}/stats")
    public UrlStatsResponse stats(@PathVariable String code) {
        return urlShortenerService.stats(code);
    }
}
