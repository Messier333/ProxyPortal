package com.messier333.proxyportal.security.service;

import com.messier333.proxyportal.security.config.LoginAttemptProperties;

public final class LoginAttemptMessages {
    public static final String SESSION_KEY = "LOGIN_ERROR_MESSAGE";

    private LoginAttemptMessages() {
    }

    public static String blockedMessage(LoginAttemptService.LoginAttemptSnapshot snapshot) {
        long minutes = toMinutes(snapshot.lockRemainingSeconds());
        return "로그인 시도 횟수를 초과하여 약 " + minutes + "분간 로그인이 제한됩니다.";
    }

    public static String failureMessage(
            LoginAttemptProperties properties,
            LoginAttemptService.LoginAttemptSnapshot snapshot
    ) {
        long lockMinutes = toMinutes(properties.getUserIpLock().toSeconds());
        return "로그인 실패 " + snapshot.failureCountUserIp()
                + "회 (남은 시도 " + snapshot.remainingAttemptsUserIp()
                + "회). " + properties.getMaxFailuresPerUserIp()
                + "회 실패 시 약 " + lockMinutes + "분간 로그인이 제한됩니다.";
    }

    private static long toMinutes(long seconds) {
        long rounded = (long) Math.ceil(seconds / 60.0d);
        return Math.max(1L, rounded);
    }
}
