package com.messier333.proxyportal.user.service;

import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.messier333.proxyportal.user.entity.Role;
import com.messier333.proxyportal.user.entity.User;
import com.messier333.proxyportal.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void changePassword(Long userId, String raw) {}

    @SuppressWarnings("null")
    @Transactional
    public Long createUser(String username, String rawPassword, Role role) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("이미 존재하는 username 입니다: " + username);
        }

        User user = User.createUser(username, passwordEncoder.encode(rawPassword), role);
  
        userRepository.save(user);
        return user.getId();
    }
}
