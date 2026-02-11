package com.messier333.proxyportal.security.service;

import static org.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import com.messier333.proxyportal.security.config.LoginAttemptProperties;

class LoginAttemptServiceTest {

    @Test
    void shouldReturnDefaultsAndIgnoreFailuresWhenDisabled() {
        LoginAttemptProperties properties = defaultProperties();
        properties.setEnabled(false);
        properties.setMaxFailuresPerUserIp(7);
        LoginAttemptService service = new LoginAttemptService(properties);

        service.onAuthenticationFailure("alice", "10.0.0.1");
        service.onAuthenticationSuccess("alice", "10.0.0.1");
        LoginAttemptService.LoginAttemptSnapshot snapshot = service.getSnapshot("alice", "10.0.0.1");

        assertThat(snapshot.blocked()).isFalse();
        assertThat(snapshot.failureCountUserIp()).isZero();
        assertThat(snapshot.remainingAttemptsUserIp()).isEqualTo(7);
        assertThat(snapshot.lockRemainingSeconds()).isZero();
    }

    @Test
    void shouldBlockByUserIpAfterMaxFailures() {
        LoginAttemptProperties properties = defaultProperties();
        properties.setMaxFailuresPerUserIp(3);
        properties.setMaxFailuresPerIp(100);
        properties.setMaxFailuresPerUsername(100);
        LoginAttemptService service = new LoginAttemptService(properties);

        service.onAuthenticationFailure("alice", "10.0.0.1");
        service.onAuthenticationFailure("alice", "10.0.0.1");
        assertThat(service.isBlocked("alice", "10.0.0.1")).isFalse();

        service.onAuthenticationFailure("alice", "10.0.0.1");
        assertThat(service.isBlocked("alice", "10.0.0.1")).isTrue();
    }

    @Test
    void shouldBlockByIpAcrossDifferentUsernames() {
        LoginAttemptProperties properties = defaultProperties();
        properties.setMaxFailuresPerUserIp(100);
        properties.setMaxFailuresPerUsername(100);
        properties.setMaxFailuresPerIp(3);
        LoginAttemptService service = new LoginAttemptService(properties);

        service.onAuthenticationFailure("alice", "10.0.0.9");
        service.onAuthenticationFailure("bob", "10.0.0.9");
        assertThat(service.isBlocked("charlie", "10.0.0.9")).isFalse();

        service.onAuthenticationFailure("charlie", "10.0.0.9");
        assertThat(service.isBlocked("diana", "10.0.0.9")).isTrue();
    }

    @Test
    void shouldBlockByUsernameAcrossDifferentIps() {
        LoginAttemptProperties properties = defaultProperties();
        properties.setMaxFailuresPerUserIp(100);
        properties.setMaxFailuresPerIp(100);
        properties.setMaxFailuresPerUsername(3);
        LoginAttemptService service = new LoginAttemptService(properties);

        service.onAuthenticationFailure("admin", "10.0.0.1");
        service.onAuthenticationFailure("admin", "10.0.0.2");
        assertThat(service.isBlocked("admin", "10.0.0.3")).isFalse();

        service.onAuthenticationFailure("admin", "10.0.0.3");
        assertThat(service.isBlocked("admin", "10.0.0.4")).isTrue();
    }

    @Test
    void shouldResetUsernameAndUserIpCountersOnSuccess() {
        LoginAttemptProperties properties = defaultProperties();
        properties.setMaxFailuresPerUserIp(2);
        properties.setMaxFailuresPerUsername(2);
        properties.setMaxFailuresPerIp(100);
        LoginAttemptService service = new LoginAttemptService(properties);

        service.onAuthenticationFailure("alice", "10.0.0.1");
        service.onAuthenticationSuccess("alice", "10.0.0.1");
        service.onAuthenticationFailure("alice", "10.0.0.1");

        assertThat(service.isBlocked("alice", "10.0.0.1")).isFalse();
    }

    @Test
    void shouldReleaseBlockAfterLockDuration() {
        LoginAttemptProperties properties = defaultProperties();
        properties.setMaxFailuresPerUserIp(2);
        properties.setMaxFailuresPerIp(100);
        properties.setMaxFailuresPerUsername(100);
        properties.setUserIpLock(Duration.ofMillis(120));
        properties.setUserIpWindow(Duration.ofSeconds(1));
        LoginAttemptService service = new LoginAttemptService(properties);

        service.onAuthenticationFailure("alice", "127.0.0.1");
        service.onAuthenticationFailure("alice", "127.0.0.1");
        assertThat(service.isBlocked("alice", "127.0.0.1")).isTrue();

        await()
                .atMost(Duration.ofSeconds(2))
                .untilAsserted(() -> assertThat(service.isBlocked("alice", "127.0.0.1")).isFalse());
    }

    @Test
    void shouldIgnoreEmptyUserOrUserIpKeys() {
        LoginAttemptProperties properties = defaultProperties();
        properties.setMaxFailuresPerUserIp(1);
        properties.setMaxFailuresPerUsername(1);
        properties.setMaxFailuresPerIp(2);
        LoginAttemptService service = new LoginAttemptService(properties);

        service.onAuthenticationFailure(" ", "10.0.0.8");
        LoginAttemptService.LoginAttemptSnapshot first = service.getSnapshot(" ", "10.0.0.8");
        assertThat(first.blocked()).isFalse();
        assertThat(first.failureCountUserIp()).isZero();
        assertThat(first.remainingAttemptsUserIp()).isEqualTo(1);

        service.onAuthenticationFailure(null, "10.0.0.8");
        assertThat(service.isBlocked(null, "10.0.0.8")).isTrue();

        service.onAuthenticationSuccess(null, "10.0.0.8");
        LoginAttemptService.LoginAttemptSnapshot afterSuccess = service.getSnapshot("other-user", "10.0.0.8");
        assertThat(afterSuccess.failureCountUserIp()).isZero();
    }

    @Test
    void shouldPruneExpiredFailuresOutsideWindow() {
        LoginAttemptProperties properties = defaultProperties();
        properties.setMaxFailuresPerUserIp(2);
        properties.setMaxFailuresPerIp(100);
        properties.setMaxFailuresPerUsername(100);
        properties.setUserIpWindow(Duration.ofMillis(80));
        properties.setUserIpLock(Duration.ofSeconds(1));
        LoginAttemptService service = new LoginAttemptService(properties);

        service.onAuthenticationFailure("alice", "10.0.0.7");

        await()
                .atMost(Duration.ofSeconds(1))
                .untilAsserted(() ->
                        assertThat(service.getSnapshot("alice", "10.0.0.7").failureCountUserIp()).isZero()
                );

        service.onAuthenticationFailure("alice", "10.0.0.7");

        LoginAttemptService.LoginAttemptSnapshot snapshot = service.getSnapshot("alice", "10.0.0.7");
        assertThat(snapshot.blocked()).isFalse();
        assertThat(snapshot.failureCountUserIp()).isEqualTo(1);
        assertThat(snapshot.remainingAttemptsUserIp()).isEqualTo(1);
    }

    private LoginAttemptProperties defaultProperties() {
        LoginAttemptProperties properties = new LoginAttemptProperties();
        properties.setEnabled(true);
        properties.setUserIpWindow(Duration.ofMinutes(15));
        properties.setUserIpLock(Duration.ofMinutes(15));
        properties.setIpWindow(Duration.ofMinutes(5));
        properties.setIpLock(Duration.ofMinutes(30));
        properties.setUsernameWindow(Duration.ofHours(1));
        properties.setUsernameLock(Duration.ofHours(1));
        return properties;
    }
}
