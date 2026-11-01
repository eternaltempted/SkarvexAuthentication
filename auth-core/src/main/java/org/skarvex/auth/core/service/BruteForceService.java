package org.skarvex.auth.core.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.skarvex.auth.core.model.RestrictedIp;
import org.skarvex.auth.core.database.repository.RestrictedIpRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public class BruteForceService {

    private final RestrictedIpRepository ipRepository;

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration ATTEMPTS_TTL = Duration.ofMinutes(15);
    private static final Duration BLOCK_DURATION = Duration.ofMinutes(10);

    private final Cache<String, Integer> failedAttemptsCache = Caffeine.newBuilder()
            .expireAfterWrite(ATTEMPTS_TTL)
            .maximumSize(10000)
            .build();

    private final Cache<String, Instant> blockedCache = Caffeine.newBuilder()
            .expireAfterWrite(BLOCK_DURATION)
            .maximumSize(10000)
            .build();

    public BruteForceService(RestrictedIpRepository ipRepository) {
        this.ipRepository = ipRepository;
    }

    public boolean isBlocked(String ip) {
        Instant now = Instant.now();
        Instant blockedUntil = blockedCache.getIfPresent(ip);

        if (blockedUntil != null) {
            if (blockedUntil.isAfter(now)) {
                return true;
            }
            blockedCache.invalidate(ip);
        }

        Optional<RestrictedIp> restrictedIpOptional = ipRepository.findByIp(ip);
        if (restrictedIpOptional.isEmpty()) return false;

        RestrictedIp restrictedIp = restrictedIpOptional.get();

        if (restrictedIp.blockedUntil().isAfter(now)) {
            blockedCache.put(ip, restrictedIp.blockedUntil());
            return true;
        }

        unblock(ip);
        return false;
    }

    public void block(String ip) {
        RestrictedIp restrictedIp = new RestrictedIp(
                ip,
                Instant.now().plus(BLOCK_DURATION)
        );

        ipRepository.block(restrictedIp);

        blockedCache.put(restrictedIp.ip(), restrictedIp.blockedUntil());
        failedAttemptsCache.invalidate(restrictedIp.ip());
    }

    public void unblock(String ip) {
        ipRepository.unblock(ip);
        blockedCache.invalidate(ip);
    }

    public int increment(String ip) {
        return failedAttemptsCache.asMap()
                .merge(ip, 1, Integer::sum);
    }
    public void reset(String ip) {
        failedAttemptsCache.invalidate(ip);
    }

    public int getMaxAttempts() {
        return MAX_ATTEMPTS;
    }

    public Optional<Duration> getRemainingTime(String ip) {

        if(!isBlocked(ip)) return Optional.empty();

        Instant blockedUntil = blockedCache.getIfPresent(ip);
        if (blockedUntil == null) return Optional.empty();

        Duration remaining = Duration.between(Instant.now(), blockedUntil);
        if (remaining.isNegative() || remaining.isZero()) {
            blockedCache.invalidate(ip);
            return Optional.empty();
        }

        return remaining.isNegative() || remaining.isZero() ? Optional.empty() : Optional.of(remaining);
    }

}
