package com.messier333.proxyportal.security.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.security.login-attempt")
public class LoginAttemptProperties {
    private boolean enabled = true;

    @Min(1)
    private int maxFailuresPerUserIp = 5;

    @NotNull
    private Duration userIpWindow = Duration.ofMinutes(15);

    @NotNull
    private Duration userIpLock = Duration.ofMinutes(15);

    @Min(1)
    private int maxFailuresPerIp = 30;

    @NotNull
    private Duration ipWindow = Duration.ofMinutes(5);

    @NotNull
    private Duration ipLock = Duration.ofMinutes(30);

    @Min(1)
    private int maxFailuresPerUsername = 20;

    @NotNull
    private Duration usernameWindow = Duration.ofHours(1);

    @NotNull
    private Duration usernameLock = Duration.ofHours(1);
}
