package com.training.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class LiveKitTokenService {

    @Value("${livekit.url:wss://livekit.internal}")
    private String livekitUrl;

    @Value("${livekit.api-key:devkey}")
    private String apiKey;

    @Value("${livekit.api-secret:secret_livekit_key_super_secure_2026_prod_edition}")
    private String apiSecret;

    @Value("${livekit.token-ttl:3600}")
    private long tokenTtlSeconds;

    public String getLivekitUrl() {
        return livekitUrl;
    }

    private Key getSigningKey() {
        byte[] keyBytes = apiSecret.getBytes(StandardCharsets.UTF_8);
        // Ensure key is at least 256 bits (32 bytes)
        if (keyBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            return Keys.hmacShaKeyFor(padded);
        }
        return Keys.hmacShaKeyFor(keyBytes);
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
        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);
        Date expiration = new Date(nowMillis + (tokenTtlSeconds * 1000));

        Map<String, Object> claims = new HashMap<>();
        claims.put("video", videoGrant);
        if (metadata != null && !metadata.isBlank()) {
            claims.put("metadata", metadata);
        }
        if (participantName != null && !participantName.isBlank()) {
            claims.put("name", participantName);
        }

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(participantIdentity)
                .setIssuer(apiKey)
                .setIssuedAt(now)
                .setNotBefore(now)
                .setExpiration(expiration)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }
}
