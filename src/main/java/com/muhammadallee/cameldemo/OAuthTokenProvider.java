package com.muhammadallee.cameldemo;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public final class OAuthTokenProvider {

    private final String tokenUrl;
    private final String clientId;
    private final String clientSecret;

    private volatile String token;
    private volatile long expiresAt;

    public OAuthTokenProvider(String tokenUrl, String clientId, String clientSecret) {
        this.tokenUrl = tokenUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public synchronized String getToken() {
        if (token != null && System.currentTimeMillis() < expiresAt) {
            return token;
        }

        // Simple blocking token fetch (called rarely)
        // Use Apache HttpClient or HttpURLConnection
        TokenResponse tr = TokenFetcher.fetch(tokenUrl, clientId, clientSecret);

        token = tr.getAccessToken();
        expiresAt = System.currentTimeMillis() + (tr.getExpiresIn() - 30) * 1000;
        return token;
    }
}
