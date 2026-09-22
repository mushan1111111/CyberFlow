package com.cyberflow.admin.system.notification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/** Sends plain-text notifications using each platform's custom-robot protocol. */
@Component
@RequiredArgsConstructor
public class NotificationWebhookClient {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    public void send(NotificationPlatform platform, String webhookUrl, String secret, String message) {
        try {
            long timestamp = platform == NotificationPlatform.DINGTALK
                    ? Instant.now().toEpochMilli() : Instant.now().getEpochSecond();
            URI uri = signedUri(platform, webhookUrl, secret, timestamp);
            String json = objectMapper.writeValueAsString(payload(platform, secret, message, timestamp));
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(12))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300 || !isSuccessfulBody(response.body())) {
                throw new NotificationDeliveryException("机器人拒绝了通知，请检查 Webhook、安全设置与签名密钥");
            }
        } catch (NotificationDeliveryException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NotificationDeliveryException("发送测试通知已中断", e);
        } catch (Exception e) {
            throw new NotificationDeliveryException("无法连接机器人，请检查网络和配置", e);
        }
    }

    URI signedUri(NotificationPlatform platform, String webhookUrl, String secret, long timestamp) throws Exception {
        URI validated = platform.validateWebhook(webhookUrl);
        if (platform != NotificationPlatform.DINGTALK || secret == null || secret.isBlank()) return validated;
        String sign = hmacBase64(secret, timestamp + "\n" + secret);
        String separator = validated.getQuery() == null || validated.getQuery().isBlank() ? "?" : "&";
        return URI.create(validated + separator + "timestamp=" + timestamp + "&sign="
                + URLEncoder.encode(sign, StandardCharsets.UTF_8));
    }

    Map<String, Object> payload(NotificationPlatform platform, String secret, String message, long timestamp) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        if (platform == NotificationPlatform.DINGTALK) {
            body.put("msgtype", "text");
            body.put("text", Map.of("content", message));
            return body;
        }
        if (secret != null && !secret.isBlank()) {
            body.put("timestamp", String.valueOf(timestamp));
            body.put("sign", feishuSign(timestamp, secret));
        }
        body.put("msg_type", "text");
        body.put("content", Map.of("text", message));
        return body;
    }

    private String feishuSign(long timestamp, String secret) throws Exception {
        String stringToSign = timestamp + "\n" + secret;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(stringToSign.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return Base64.getEncoder().encodeToString(mac.doFinal(new byte[0]));
    }

    private String hmacBase64(String key, String content) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return Base64.getEncoder().encodeToString(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
    }

    private boolean isSuccessfulBody(String body) {
        if (body == null || body.isBlank()) return true;
        try {
            JsonNode root = objectMapper.readTree(body);
            if (root.has("errcode")) return root.path("errcode").asInt(-1) == 0;
            if (root.has("code")) return root.path("code").asInt(-1) == 0;
            if (root.has("StatusCode")) return root.path("StatusCode").asInt(-1) == 0;
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }
}

