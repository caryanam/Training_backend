package com.training.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@Slf4j
public class LiveKitTokenService {

    @Value("${livekit.url:ws://localhost:7880}")
    private String livekitUrl;

    @Value("${livekit.api-key:devkey}")
    private String apiKey;

    @Value("${livekit.api-secret:secret}")
    private String apiSecret;

    @Value("${livekit.token-ttl:3600}")
    private long tokenTtlSeconds;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String getLivekitUrl() {
        return livekitUrl;
    }

    /**
     * Generates a signed LiveKit JWT token for Faculty (Publish + Subscribe + Screen Share + Mic + Camera + Room Admin)
     */
    public String createFacultyToken(String roomName, String participantIdentity, String participantName, String metadata) {
        Map<String, Object> videoGrant = new HashMap<>();
        videoGrant.put("room", roomName);
        videoGrant.put("roomJoin", true);
        videoGrant.put("canPublish", true);
        videoGrant.put("canSubscribe", true);
        videoGrant.put("canPublishData", true);
        videoGrant.put("canPublishSources", List.of("camera", "microphone", "screen_share", "screen_share_audio"));
        videoGrant.put("roomAdmin", true);

        return buildLiveKitJwt(roomName, participantIdentity, participantName, metadata, videoGrant);
    }

    /**
     * Generates a signed LiveKit JWT token for Student (Subscribe ONLY, publish = FALSE, roomAdmin = FALSE)
     */
    public String createStudentToken(String roomName, String participantIdentity, String participantName, String metadata) {
        Map<String, Object> videoGrant = new HashMap<>();
        videoGrant.put("room", roomName);
        videoGrant.put("roomJoin", true);
        videoGrant.put("canPublish", false);
        videoGrant.put("canSubscribe", true);
        videoGrant.put("canPublishData", false);
        videoGrant.put("roomAdmin", false);

        return buildLiveKitJwt(roomName, participantIdentity, participantName, metadata, videoGrant);
    }

    private String buildLiveKitJwt(String roomName, String participantIdentity, String participantName, String metadata, Map<String, Object> videoGrant) {
        long nowSeconds = System.currentTimeMillis() / 1000;
        long expSeconds = nowSeconds + tokenTtlSeconds;

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("iss", apiKey);
        payload.put("sub", participantIdentity);
        payload.put("iat", nowSeconds);
        payload.put("nbf", nowSeconds);
        payload.put("exp", expSeconds);
        if (participantName != null && !participantName.isBlank()) {
            payload.put("name", participantName);
        }
        if (metadata != null && !metadata.isBlank()) {
            payload.put("metadata", metadata);
        }
        payload.put("video", videoGrant);

        try {
            Map<String, Object> header = new LinkedHashMap<>();
            header.put("alg", "HS256");
            header.put("typ", "JWT");

            String headerJson = objectMapper.writeValueAsString(header);
            String payloadJson = objectMapper.writeValueAsString(payload);

            String headerB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
            String payloadB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));

            String dataToSign = headerB64 + "." + payloadB64;

            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] signatureBytes = mac.doFinal(dataToSign.getBytes(StandardCharsets.US_ASCII));
            String signatureB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(signatureBytes);

            return dataToSign + "." + signatureB64;
        } catch (Exception e) {
            log.error("Failed to generate LiveKit JWT token: {}", e.getMessage(), e);
            throw new RuntimeException("Could not generate LiveKit access token: " + e.getMessage(), e);
        }
    }
}
