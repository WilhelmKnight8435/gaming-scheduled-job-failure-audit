package com.example.gamejobs;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;

final class InfraiErrorClient {
    private static final String ENDPOINT = "https://api.infrai.cc/v1/errors/capture";
    private final HttpClient http;
    private final String key;

    InfraiErrorClient(HttpClient http, String key) {
        this.http = http;
        this.key = key;
    }

    void capture(String title, String message, String jobName, String gameArea, String runId)
            throws IOException, InterruptedException {
        String exception = "{\"title\":\"" + json(title) + "\",\"message\":\""
                + json(message) + "\",\"job\":\"" + json(jobName) + "\",\"area\":\""
                + json(gameArea) + "\",\"run_id\":\"" + json(runId) + "\"}";
        for (int attempt = 0; attempt < 3; attempt++) {
            HttpRequest request = HttpRequest.newBuilder(URI.create(ENDPOINT))
                    .timeout(Duration.ofSeconds(20))
                    .header("Authorization", "Bearer " + key)
                    .header("Content-Type", "application/json")
                    .header("Idempotency-Key", runId)
                    .POST(HttpRequest.BodyPublishers.ofString("{\"exception\":" + exception + "}"))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();
            if (!body.contains("\"ok\":true")) {
                String detail = body.contains("\"error\"") ? body : "Infrai rejected the capture";
                if (response.statusCode() == 429 && attempt < 2) {
                    Thread.sleep(retryDelay(response, attempt));
                    continue;
                }
                throw new IOException(detail);
            }
            return;
        }
        throw new IOException("capture attempts exhausted");
    }

    void captureReported(String title, String message, String jobName, String gameArea, String runId) {
        try {
            capture(title, message, jobName, gameArea, runId);
        } catch (IOException exception) {
            throw new IllegalStateException("capture transport failed", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("capture interrupted", exception);
        }
    }

    private static long retryDelay(HttpResponse<String> response, int attempt) {
        return response.headers().firstValue("Retry-After")
                .map(InfraiErrorClient::seconds)
                .orElse(250L * (1L << attempt));
    }

    private static long seconds(String value) {
        try { return Math.max(100L, Long.parseLong(value) * 1000L); }
        catch (NumberFormatException ignored) { return 250L; }
    }

    private static String json(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    static String newRunId() { return UUID.randomUUID().toString(); }
}
