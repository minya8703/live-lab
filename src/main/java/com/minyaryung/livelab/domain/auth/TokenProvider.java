package com.minyaryung.livelab.domain.auth;

public interface TokenProvider {
    String generate(String email, String name, String picture);
    TokenClaims parse(String token);
    record TokenClaims(String email, String name, String picture) {}
}
