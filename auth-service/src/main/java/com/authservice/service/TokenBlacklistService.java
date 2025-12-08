package com.authservice.service;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class TokenBlacklistService {
    private final ConcurrentMap<String, Long> blacklist = new ConcurrentHashMap<>();

    public void blacklist(String jti, Instant expiry) {
        blacklist.put(jti, expiry.toEpochMilli());
    }

    public boolean isBlacklisted(String jti) {
        if (jti == null) return false;
        Long until = blacklist.get(jti);
        if (until == null) return false;
        if (until < Instant.now().toEpochMilli()) {
            blacklist.remove(jti);
            return false;
        }
        return true;
    }
}