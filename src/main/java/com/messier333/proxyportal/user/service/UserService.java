package com.messier333.proxyportal.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
    @Transactional
    public void changePassword(Long userId, String raw) {}
}
