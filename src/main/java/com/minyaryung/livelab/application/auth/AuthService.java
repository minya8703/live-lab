package com.minyaryung.livelab.application.auth;

import com.minyaryung.livelab.domain.auth.OAuthVerifier;
import com.minyaryung.livelab.domain.auth.TokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final OAuthVerifier oAuthVerifier;
    private final TokenProvider tokenProvider;
    private final String masterEmail;
    private final String clientId;

    public AuthService(OAuthVerifier oAuthVerifier, TokenProvider tokenProvider,
                       @Value("${livelab.auth.google-client-id}") String clientId,
                       @Value("${livelab.auth.master-email}") String masterEmail) {
        this.oAuthVerifier = oAuthVerifier;
        this.tokenProvider = tokenProvider;
        this.clientId = clientId;
        this.masterEmail = masterEmail;
    }

    public String getClientId() { return clientId; }

    public boolean isMaster(String email) { return masterEmail.equalsIgnoreCase(email); }

    public OAuthVerifier.OAuthResult verifyOAuth(String credential) {
        return oAuthVerifier.verify(credential);
    }

    public String issueToken(String email, String name, String picture) {
        return tokenProvider.generate(email, name, picture);
    }

    public TokenProvider.TokenClaims parseToken(String token) {
        return tokenProvider.parse(token);
    }
}
