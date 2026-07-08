package com.minyaryung.livelab.domain.auth;

public interface OAuthVerifier {
    OAuthResult verify(String credential);
    record OAuthResult(String email, String name, String picture) {}
}
