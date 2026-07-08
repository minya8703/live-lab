package com.minyaryung.livelab.presentation.auth;

import com.minyaryung.livelab.application.auth.AuthService;
import com.minyaryung.livelab.domain.auth.OAuthVerifier;
import com.minyaryung.livelab.domain.auth.TokenProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) { this.authService = authService; }

    @GetMapping("/client-id")
    public ResponseEntity<Map<String, String>> clientId() {
        return ResponseEntity.ok(Map.of("clientId", authService.getClientId()));
    }

    @PostMapping("/google")
    public ResponseEntity<Map<String, Object>> googleLogin(@RequestBody Map<String, String> body) {
        String credential = body.get("credential");
        if (credential == null || credential.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing credential");
        OAuthVerifier.OAuthResult result = authService.verifyOAuth(credential);
        if (result == null)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Google token");
        boolean isMaster = authService.isMaster(result.email());
        String jwt = authService.issueToken(result.email(), result.name(), result.picture());
        return ResponseEntity.ok(Map.of(
                "token", jwt, "email", result.email(),
                "name", result.name() != null ? result.name() : "",
                "picture", result.picture() != null ? result.picture() : "",
                "master", isMaster));
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(@RequestHeader("Authorization") String authHeader) {
        String token = extractToken(authHeader);
        try {
            TokenProvider.TokenClaims claims = authService.parseToken(token);
            return ResponseEntity.ok(Map.of(
                    "email", claims.email(), "name", claims.name(),
                    "picture", claims.picture(), "master", authService.isMaster(claims.email())));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }
    }

    private static String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) return authHeader.substring(7);
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing Bearer token");
    }
}
