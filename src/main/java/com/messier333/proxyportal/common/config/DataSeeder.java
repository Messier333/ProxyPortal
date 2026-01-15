package com.messier333.proxyportal.common.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.stereotype.Component;

import com.messier333.proxyportal.user.entity.Role;
import com.messier333.proxyportal.user.service.UserService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DataSeeder implements org.springframework.boot.ApplicationRunner {

    private final UserService userService;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        userService.createUser("admin", "changeme", Role.ADMIN);
    }
}
