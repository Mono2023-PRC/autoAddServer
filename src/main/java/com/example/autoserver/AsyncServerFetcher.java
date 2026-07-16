package com.example.autoserver;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.ClientChatEvent;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class AsyncServerFetcher {
    private static final int MAX_RETRY = 5;
    private static final long RETRY_INTERVAL_MS = 3000;
    
    private final HttpClient httpClient;
    private final ExecutorService executor;
    private final AtomicInteger retryCount;
    private CompletableFuture<Void> currentTask;
    private boolean cancelled;

    public AsyncServerFetcher() {
        httpClient = HttpClient.newHttpClient();
        executor = Executors.newSingleThreadExecutor();
        retryCount = new AtomicInteger(0);
    }

    public void fetchServerIp(String apiUrl, ServerFetchCallback callback) {
        cancelled = false;
        retryCount.set(0);
        executeFetch(apiUrl, callback);
    }

    private void executeFetch(String apiUrl, ServerFetchCallback callback) {
        if (cancelled) {
            return;
        }

        String fullUrl = apiUrl.startsWith("http") ? apiUrl : "http://" + apiUrl;
        if (!fullUrl.endsWith("/getip")) {
            fullUrl += "/getip";
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(fullUrl))
                .GET()
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApplyAsync(response -> {
                    if (response.statusCode() == 200) {
                        return response.body().trim();
                    } else {
                        throw new RuntimeException("HTTP error: " + response.statusCode());
                    }
                }, executor)
                .thenAcceptAsync(ip -> {
                    if (!cancelled) {
                        retryCount.set(0);
                        callback.onSuccess(ip);
                    }
                }, executor)
                .exceptionallyAsync(error -> {
                    if (cancelled) {
                        return null;
                    }

                    int currentRetry = retryCount.incrementAndGet();
                    showToast("第" + currentRetry + "次请求:失败,自动等待" + (RETRY_INTERVAL_MS / 1000) + "秒后再尝试");

                    if (currentRetry >= MAX_RETRY) {
                        callback.onFailure("已达到最大重试次数");
                    } else {
                        try {
                            Thread.sleep(RETRY_INTERVAL_MS);
                            executeFetch(apiUrl, callback);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                    return null;
                }, executor);
    }

    public void cancel() {
        cancelled = true;
        retryCount.set(0);
        if (currentTask != null) {
            currentTask.cancel(true);
        }
    }

    public void resetAndRetry(String apiUrl, ServerFetchCallback callback) {
        cancel();
        fetchServerIp(apiUrl, callback);
    }

    public int getRetryCount() {
        return retryCount.get();
    }

    private void showToast(String message) {
        Minecraft.getInstance().execute(() -> {
            Minecraft.getInstance().getToasts().addToast(new SimpleToast(
                    Component.literal("AutoServer"),
                    Component.literal(message)
            ));
        });
    }

    public void shutdown() {
        executor.shutdown();
        httpClient.close();
    }

    public interface ServerFetchCallback {
        void onSuccess(String ip);
        void onFailure(String error);
    }
}
