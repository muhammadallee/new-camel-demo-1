package com.muhammadallee.cameldemo;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class AsyncRestNotifier {

    private final CloseableHttpAsyncClient client;
    private final OAuthTokenProvider tokenProvider;
    private final CircuitBreaker circuitBreaker;

    public AsyncRestNotifier(OAuthTokenProvider tp, CircuitBreaker cb) {
        this.tokenProvider = tp;
        this.circuitBreaker = cb;
        this.client = HttpAsyncClients.createDefault();
        this.client.start();
    }

    public void post(String url, String json, Consumer<Boolean> callback) {

        if (!circuitBreaker.tryAcquirePermission()) {
            callback.accept(false);
            return;
        }

        HttpPost post = new HttpPost(url);
        post.setHeader("Authorization", "Bearer " + tokenProvider.getToken());
        post.setHeader("Content-Type", "application/json");
        post.setEntity(new StringEntity(json, StandardCharsets.UTF_8));

        client.execute(post, new FutureCallback<HttpResponse>() {

            @Override
            public void completed(HttpResponse response) {
                circuitBreaker.onSuccess(0, TimeUnit.SECONDS);
                callback.accept(response.getStatusLine().getStatusCode() < 300);
            }

            @Override
            public void failed(Exception ex) {
                circuitBreaker.onError(0, TimeUnit.SECONDS, ex);
                callback.accept(false);
            }

            @Override
            public void cancelled() {
                callback.accept(false);
            }
        });
    }
}
