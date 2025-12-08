package com.muhammadallee.cameldemo;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public final class TokenFetcher {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private TokenFetcher() {}

    public static TokenResponse fetch(
            String tokenUrl,
            String clientId,
            String clientSecret) {

        try (CloseableHttpClient client = HttpClients.createDefault()) {

            HttpPost post = new HttpPost(tokenUrl);
            post.setEntity(new UrlEncodedFormEntity(Arrays.asList(
                    new BasicNameValuePair("grant_type", "client_credentials"),
                    new BasicNameValuePair("client_id", clientId),
                    new BasicNameValuePair("client_secret", clientSecret)
            ), StandardCharsets.UTF_8));

            try (CloseableHttpResponse res = client.execute(post)) {
                return MAPPER.readValue(
                        res.getEntity().getContent(),
                        TokenResponse.class
                );
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch OAuth token", e);
        }
    }
}
