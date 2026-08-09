package com.minyaryung.livelab.infra.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;

@Component
public class AuthCookieService {

    public static final String SESSION_COOKIE = "livelab_session";
    public static final String CSRF_COOKIE = "livelab_csrf";
    public static final String CSRF_HEADER = "X-CSRF-Token";

    private final SecureRandom secureRandom = new SecureRandom();
    private final boolean secure;
    private final Duration maxAge;

    public AuthCookieService(@Value("${livelab.auth.cookie-secure:true}") boolean secure,
                             @Value("${livelab.auth.jwt-expiration-ms:86400000}") long expirationMs) {
        this.secure = secure;
        this.maxAge = Duration.ofMillis(expirationMs);
    }

    public String newCsrfToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public ResponseCookie sessionCookie(String jwt) {
        return cookie(SESSION_COOKIE, jwt, true, maxAge);
    }

    public ResponseCookie csrfCookie(String csrfToken) {
        return cookie(CSRF_COOKIE, csrfToken, false, maxAge);
    }

    public ResponseCookie clearSessionCookie() {
        return cookie(SESSION_COOKIE, "", true, Duration.ZERO);
    }

    public ResponseCookie clearCsrfCookie() {
        return cookie(CSRF_COOKIE, "", false, Duration.ZERO);
    }

    public String sessionToken(HttpServletRequest request) {
        return findCookie(request, SESSION_COOKIE);
    }

    public boolean hasValidCsrfToken(HttpServletRequest request) {
        String cookie = findCookie(request, CSRF_COOKIE);
        String header = request.getHeader(CSRF_HEADER);
        if (cookie == null || header == null) return false;
        return MessageDigest.isEqual(cookie.getBytes(StandardCharsets.UTF_8),
                header.getBytes(StandardCharsets.UTF_8));
    }

    private ResponseCookie cookie(String name, String value, boolean httpOnly, Duration age) {
        return ResponseCookie.from(name, value)
                .httpOnly(httpOnly)
                .secure(secure)
                .sameSite("Strict")
                .path("/")
                .maxAge(age)
                .build();
    }

    private static String findCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) return cookie.getValue();
        }
        return null;
    }
}
