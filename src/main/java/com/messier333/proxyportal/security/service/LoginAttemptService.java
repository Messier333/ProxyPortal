package com.messier333.proxyportal.security.service;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.messier333.proxyportal.security.config.LoginAttemptProperties;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LoginAttemptService {
    private static final String SEPARATOR = "|";

    private final LoginAttemptProperties properties;

    private final Map<String, AttemptState> userIpAttempts = new ConcurrentHashMap<>();
    private final Map<String, AttemptState> ipAttempts = new ConcurrentHashMap<>();
    private final Map<String, AttemptState> usernameAttempts = new ConcurrentHashMap<>();

    public synchronized boolean isBlocked(String username, String remoteAddress) {
        return getSnapshot(username, remoteAddress).blocked();
    }

    public synchronized LoginAttemptSnapshot getSnapshot(String username, String remoteAddress) {
        if (!properties.isEnabled()) {
            return new LoginAttemptSnapshot(false, 0, properties.getMaxFailuresPerUserIp(), 0);
        }
        long now = System.currentTimeMillis();
        ScopeSnapshot userIp = inspectScope(
                userIpAttempts,
                userIpKey(username, remoteAddress),
                now,
                properties.getUserIpWindow()
        );
        ScopeSnapshot ip = inspectScope(
                ipAttempts,
                normalize(remoteAddress),
                now,
                properties.getIpWindow()
        );
        ScopeSnapshot user = inspectScope(
                usernameAttempts,
                normalize(username),
                now,
                properties.getUsernameWindow()
        );

        boolean blocked = userIp.blocked() || ip.blocked() || user.blocked();
        long remainingLockMillis = Math.max(userIp.remainingLockMillis(), Math.max(ip.remainingLockMillis(), user.remainingLockMillis()));
        long remainingLockSeconds = blocked ? Math.max(1L, (remainingLockMillis + 999L) / 1000L) : 0L;
        int remainingAttempts = Math.max(0, properties.getMaxFailuresPerUserIp() - userIp.failureCount());
        cleanup(now);
        return new LoginAttemptSnapshot(blocked, userIp.failureCount(), remainingAttempts, remainingLockSeconds);
    }

    public synchronized void onAuthenticationFailure(String username, String remoteAddress) {
        if (!properties.isEnabled()) {
            return;
        }
        long now = System.currentTimeMillis();
        recordFailure(
                userIpAttempts,
                userIpKey(username, remoteAddress),
                now,
                properties.getUserIpWindow(),
                properties.getMaxFailuresPerUserIp(),
                properties.getUserIpLock()
        );
        recordFailure(
                ipAttempts,
                normalize(remoteAddress),
                now,
                properties.getIpWindow(),
                properties.getMaxFailuresPerIp(),
                properties.getIpLock()
        );
        recordFailure(
                usernameAttempts,
                normalize(username),
                now,
                properties.getUsernameWindow(),
                properties.getMaxFailuresPerUsername(),
                properties.getUsernameLock()
        );
        cleanup(now);
    }

    public synchronized void onAuthenticationSuccess(String username, String remoteAddress) {
        if (!properties.isEnabled()) {
            return;
        }
        reset(userIpAttempts, userIpKey(username, remoteAddress));
        reset(usernameAttempts, normalize(username));
        cleanup(System.currentTimeMillis());
    }

    private String userIpKey(String username, String remoteAddress) {
        String normalizedUsername = normalize(username);
        String normalizedRemoteAddress = normalize(remoteAddress);
        if (normalizedUsername.isEmpty() || normalizedRemoteAddress.isEmpty()) {
            return "";
        }
        return normalizedUsername + SEPARATOR + normalizedRemoteAddress;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    private ScopeSnapshot inspectScope(Map<String, AttemptState> attemptsByKey, String key, long now, Duration window) {
        if (key.isEmpty()) {
            return ScopeSnapshot.EMPTY;
        }
        AttemptState state = attemptsByKey.get(key);
        if (state == null) {
            return ScopeSnapshot.EMPTY;
        }
        pruneAttempts(state, now, window);
        state.lastTouchedAt = now;
        long remainingLockMillis = Math.max(0L, state.lockedUntilEpochMs - now);
        boolean blocked = remainingLockMillis > 0L;
        if (!blocked && state.lockedUntilEpochMs != 0L) {
            state.lockedUntilEpochMs = 0L;
        }
        int failureCount = state.failureEpochMs.size();
        if (state.failureEpochMs.isEmpty()) {
            attemptsByKey.remove(key);
        }
        return new ScopeSnapshot(blocked, failureCount, remainingLockMillis);
    }

    private void recordFailure(
            Map<String, AttemptState> attemptsByKey,
            String key,
            long now,
            Duration window,
            int maxFailures,
            Duration lockDuration
    ) {
        if (key.isEmpty()) {
            return;
        }
        AttemptState state = attemptsByKey.computeIfAbsent(key, ignored -> new AttemptState());
        pruneAttempts(state, now, window);
        state.failureEpochMs.addLast(now);
        state.lastTouchedAt = now;
        if (state.failureEpochMs.size() >= maxFailures) {
            state.lockedUntilEpochMs = now + lockDuration.toMillis();
            state.failureEpochMs.clear();
        }
    }

    private void reset(Map<String, AttemptState> attemptsByKey, String key) {
        if (key.isEmpty()) {
            return;
        }
        attemptsByKey.remove(key);
    }

    private void pruneAttempts(AttemptState state, long now, Duration window) {
        long threshold = now - window.toMillis();
        while (!state.failureEpochMs.isEmpty() && state.failureEpochMs.peekFirst() < threshold) {
            state.failureEpochMs.removeFirst();
        }
    }

    private void cleanup(long now) {
        cleanupMap(userIpAttempts, now, properties.getUserIpWindow(), properties.getUserIpLock());
        cleanupMap(ipAttempts, now, properties.getIpWindow(), properties.getIpLock());
        cleanupMap(usernameAttempts, now, properties.getUsernameWindow(), properties.getUsernameLock());
    }

    private void cleanupMap(Map<String, AttemptState> attemptsByKey, long now, Duration window, Duration lock) {
        long expiryThreshold = now - Math.max(window.toMillis(), lock.toMillis());
        attemptsByKey.entrySet().removeIf(entry -> {
            AttemptState state = entry.getValue();
            pruneAttempts(state, now, window);
            return state.lockedUntilEpochMs <= now
                    && state.failureEpochMs.isEmpty()
                    && state.lastTouchedAt <= expiryThreshold;
        });
    }

    private static class AttemptState {
        private final Deque<Long> failureEpochMs = new ArrayDeque<>();
        private long lockedUntilEpochMs;
        private long lastTouchedAt;
    }

    private record ScopeSnapshot(boolean blocked, int failureCount, long remainingLockMillis) {
        private static final ScopeSnapshot EMPTY = new ScopeSnapshot(false, 0, 0L);
    }

    public record LoginAttemptSnapshot(
            boolean blocked,
            int failureCountUserIp,
            int remainingAttemptsUserIp,
            long lockRemainingSeconds
    ) {
    }
}
