package com.jurisfacil.shared.security;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.rate-limit")
public class RateLimitConfig {

    private List<String> paths = List.of();
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public List<String> getPaths() {
        return paths;
    }

    public void setPaths(List<String> paths) {
        this.paths = paths == null ? List.of() : List.copyOf(paths);
    }

    public Bucket bucketForIp(String ipAddress) {
        return buckets.computeIfAbsent(ipAddress, ignored -> newBucket());
    }

    private Bucket newBucket() {
        Bandwidth limit = Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }
}
