package com.minyaryung.livelab.presentation.auth;

import com.minyaryung.livelab.application.auth.AuthService;
import com.minyaryung.livelab.domain.auth.OAuthVerifier;
import com.minyaryung.livelab.domain.auth.TokenProvider;
import com.minyaryung.livelab.infra.security.AuthCookieService;
import com.minyaryung.livelab.infra.common.LoginAttemptRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthCookieService cookies;
    private final LoginAttemptRateLimiter loginRateLimiter;

    public AuthController(AuthService authService, AuthCookieService cookies,
                          LoginAttemptRateLimiter loginRateLimiter) {
        this.authService = authService;
        this.cookies = cookies;
        this.loginRateLimiter = loginRateLimiter;
    }

    @GetMapping("/client-id")
    public ResponseEntity<Map<String, String>> clientId() {
        return ResponseEntity.ok(Map.of("clientId", authService.getClientId()));
    }

    @PostMapping("/google")
    public ResponseEntity<Map<String, Object>> googleLogin(@RequestBody Map<String, String> body,
                                                            HttpServletRequest request) {
        String credential = body.get("credential");
        if (credential == null || credential.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing credential");
        if (!loginRateLimiter.tryAcquire(request.getRemoteAddr()))
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many login attempts. Try again later.");
        OAuthVerifier.OAuthResult result = authService.verifyOAuth(credential);
        if (result == null)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Google token");
        boolean isMaster = authService.isMaster(result.email());
        String name = result.name() != null ? result.name() : "";
        String picture = result.picture() != null ? result.picture() : "";
        String jwt = authService.issueToken(result.email(), name, picture);
        String csrfToken = cookies.newCsrfToken();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookies.sessionCookie(jwt).toString())
                .header(HttpHeaders.SET_COOKIE, cookies.csrfCookie(csrfToken).toString())
                .body(Map.of(
                "email", result.email(),
                "name", name,
                "picture", picture,
                "master", isMaster));
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(HttpServletRequest request) {
        String token = cookies.sessionToken(request);
        if (token == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing session");
        try {
            TokenProvider.TokenClaims claims = authService.parseToken(token);
            return ResponseEntity.ok(Map.of(
                    "email", claims.email(), "name", claims.name(),
                    "picture", claims.picture(), "master", authService.isMaster(claims.email())));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        if (!cookies.hasValidCsrfToken(request))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid CSRF token");
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookies.clearSessionCookie().toString())
                .header(HttpHeaders.SET_COOKIE, cookies.clearCsrfCookie().toString())
                .build();
    }
}
