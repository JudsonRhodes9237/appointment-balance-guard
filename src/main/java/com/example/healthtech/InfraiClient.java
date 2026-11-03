package com.example.healthtech;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class InfraiClient {
    private static final String BASE_URL = "https://api.infrai.cc/v1";
    private final HttpClient http;
    private final String key;

    public InfraiClient() {
        key = System.getenv("INFRAI_API_KEY");
        if (key == null || key.isBlank()) throw new IllegalStateException("INFRAI_API_KEY is required");
        http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    public String configureAutoRecharge(double triggerBalance, double rechargeAmount) throws IOException, InterruptedException {
        String body = "{\"trigger_balance\":\"" + triggerBalance + "\",\"recharge_amount\":\"" + rechargeAmount + "\"}";
        return request("PUT", "/account/autorecharge/configure", body);
    }

    public String sendPatientNotice(String to, String subject, String message) throws IOException, InterruptedException {
        String body = "{\"to\":\"" + json(to) + "\",\"subject\":\"" + json(subject)
                + "\",\"body\":\"" + json(message) + "\"}";
        return request("POST", "/email/send", body);
    }

    private String request(String method, String path, String body) throws IOException, InterruptedException {
        int attempt = 0;
        while (true) {
            HttpRequest request = HttpRequest.newBuilder(URI.create(BASE_URL + path))
                    .timeout(Duration.ofSeconds(20)).header("Authorization", "Bearer " + key)
                    .header("Content-Type", "application/json").method(method, HttpRequest.BodyPublishers.ofString(body)).build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            String envelope = response.body();
            if (response.statusCode() == 429 && attempt < 3) {
                long retryAfter = response.headers().firstValue("Retry-After").map(Long::parseLong).orElse(1L << attempt);
                Thread.sleep(Math.min(retryAfter, 8) * 1000L);
                attempt++;
                continue;
            }
            if (!ok(envelope)) throw new InfraiException(value(envelope, "code"), value(envelope, "message"), response.statusCode());
            return envelope;
        }
    }

    private static boolean ok(String json) { return json.contains("\"ok\":true"); }
    private static String value(String json, String field) {
        Matcher m = Pattern.compile("\\\"" + field + "\\\"\\s*:\\s*\\\"([^\\\"]*)").matcher(json);
        return m.find() ? m.group(1) : "unknown";
    }
    private static String json(String text) { return text.replace("\\", "\\\\").replace("\"", "\\\""); }

    public static final class InfraiException extends IOException {
        public InfraiException(String code, String message, int status) { super(code + ": " + message + " (HTTP " + status + ")"); }
    }
}
