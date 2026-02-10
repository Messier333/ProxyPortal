package com.messier333.proxyportal.user.service;

import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.messier333.proxyportal.portal.entity.PortalCategory;
import com.messier333.proxyportal.portal.entity.PortalTab;
import com.messier333.proxyportal.portal.repository.PortalCategoryRepository;
import com.messier333.proxyportal.portal.repository.PortalLinkRepository;
import com.messier333.proxyportal.portal.repository.PortalTabRepository;
import com.messier333.proxyportal.user.entity.Role;
import com.messier333.proxyportal.user.entity.User;
import com.messier333.proxyportal.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PortalTabRepository portalTabRepository;
    private final PortalCategoryRepository portalCategoryRepository;
    private final PortalLinkRepository portalLinkRepository;

    @Transactional
    public void changePassword(Long userId, String raw) {
        if (userId == null) {
            throw new IllegalArgumentException("userId가 필요합니다.");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
        user.changePassword(passwordEncoder.encode(validatePassword(raw)));
    }

    @Transactional
    public void changePassword(String username, String raw) {
        String normalizedUsername = normalizeUsername(username);
        User user = userRepository.findByUsername(normalizedUsername)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다: " + normalizedUsername));
        user.changePassword(passwordEncoder.encode(validatePassword(raw)));
    }

    @Transactional(readOnly = true)
    public List<User> findAllUsers() {
        return userRepository.findAllByOrderByIdAsc();
    }

    @Transactional
    public void deleteUser(String actorUsername, String targetUsername) {
        String actor = normalizeUsername(actorUsername);
        String target = normalizeUsername(targetUsername);

        if (actor.equals(target)) {
            throw new IllegalArgumentException("현재 로그인한 계정은 삭제할 수 없습니다.");
        }

        User targetUser = userRepository.findByUsername(target)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다: " + target));

        if (targetUser.getRole() == Role.ADMIN && userRepository.countByRole(Role.ADMIN) <= 1) {
            throw new IllegalArgumentException("마지막 관리자 계정은 삭제할 수 없습니다.");
        }

        List<PortalTab> tabs = portalTabRepository.findAllByUserUsernameOrderBySortOrderAscIdAsc(target);
        for (PortalTab tab : tabs) {
            List<PortalCategory> categories = portalCategoryRepository.findAllByTabIdOrderBySortOrderAscIdAsc(tab.getId());
            for (PortalCategory category : categories) {
                portalLinkRepository.deleteAll(portalLinkRepository.findAllByCategoryIdOrderBySortOrderAscIdAsc(category.getId()));
            }
            portalCategoryRepository.deleteAll(categories);
        }
        portalTabRepository.deleteAll(tabs);
        userRepository.delete(targetUser);
    }

    @SuppressWarnings("null")
    @Transactional
    public Long createUser(String username, String rawPassword, Role role) {
        String normalizedUsername = normalizeUsername(username);
        if (userRepository.existsByUsername(normalizedUsername)) {
            throw new IllegalArgumentException("이미 존재하는 username 입니다: " + normalizedUsername);
        }
        Role targetRole = role == null ? Role.USER : role;

        User user = User.createUser(normalizedUsername, passwordEncoder.encode(validatePassword(rawPassword)), targetRole);
  
        userRepository.save(user);
        return user.getId();
    }

    private String normalizeUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username은 비어 있을 수 없습니다.");
        }
        return username.trim();
    }

    private String validatePassword(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("비밀번호는 비어 있을 수 없습니다.");
        }
        String normalized = rawPassword.trim();
        if (normalized.length() < 4) {
            throw new IllegalArgumentException("비밀번호는 최소 4자 이상이어야 합니다.");
        }
        return normalized;
    }
}
