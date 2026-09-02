package com.training.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitingFilter implements Filter {

    private final Map<String, Bucket> loginBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> registerBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> adminBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> globalBuckets = new ConcurrentHashMap<>();

    private Bucket createNewBucket(int capacity, int refillAmount, Duration period) {
        Bandwidth limit = Bandwidth.classic(capacity, Refill.intervally(refillAmount, period));
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) 
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        
        if (request.getMethod().equalsIgnoreCase("OPTIONS")) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        String path = request.getRequestURI();
        String ip = getClientIP(request);

        boolean allowed = true;

        if (path.contains("/auth/login")) {
            Bucket bucket = loginBuckets.computeIfAbsent(ip, k -> createNewBucket(100, 100, Duration.ofMinutes(1)));
            allowed = bucket.tryConsume(1);
        } else if (path.contains("/auth/register") || path.contains("/auth/student/register")) {
            Bucket bucket = registerBuckets.computeIfAbsent(ip, k -> createNewBucket(50, 50, Duration.ofMinutes(1)));
            allowed = bucket.tryConsume(1);
        } else if (path.contains("/admin/")) {
            String authHeader = request.getHeader("Authorization");
            String key = (authHeader != null && !authHeader.isEmpty()) ? authHeader : ip;
            Bucket bucket = adminBuckets.computeIfAbsent(key, k -> createNewBucket(120, 120, Duration.ofMinutes(1)));
            allowed = bucket.tryConsume(1);
        } else {
            Bucket bucket = globalBuckets.computeIfAbsent(ip, k -> createNewBucket(200, 200, Duration.ofMinutes(1)));
            allowed = bucket.tryConsume(1);
        }

        if (!allowed) {
            String origin = request.getHeader("Origin");
            if (origin != null) {
                response.setHeader("Access-Control-Allow-Origin", origin);
                response.setHeader("Access-Control-Allow-Credentials", "true");
            }
            response.setStatus(429); // HTTP 429 Too Many Requests
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":false,\"message\":\"Too Many Requests - Rate limit exceeded. Please try again later.\",\"status\":429}");
            return;
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }
}
