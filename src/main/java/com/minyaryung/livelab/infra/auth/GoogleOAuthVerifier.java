package com.minyaryung.livelab.infra.auth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.minyaryung.livelab.domain.auth.OAuthVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class GoogleOAuthVerifier implements OAuthVerifier {

    private static final Logger log = LoggerFactory.getLogger(GoogleOAuthVerifier.class);
    private final GoogleIdTokenVerifier verifier;

    public GoogleOAuthVerifier(@Value("${livelab.auth.google-client-id}") String clientId) {
        this.verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(clientId))
                .build();
    }

    @Override
    public OAuthResult verify(String credential) {
        try {
            GoogleIdToken idToken = verifier.verify(credential);
            if (idToken == null) return null;
            GoogleIdToken.Payload payload = idToken.getPayload();
            return new OAuthResult(
                    payload.getEmail(),
                    (String) payload.get("name"),
                    (String) payload.get("picture"));
        } catch (Exception e) {
            log.warn("Google credential verification failed errorType={}",
                    e.getClass().getSimpleName());
            return null;
        }
    }
}
