package com.messier333.proxyportal.user.service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.messier333.proxyportal.common.exception.BadRequestException;
import com.messier333.proxyportal.common.exception.ConflictException;
import com.messier333.proxyportal.common.exception.ForbiddenOperationException;
import com.messier333.proxyportal.common.exception.NotFoundException;
import com.messier333.proxyportal.portal.repository.PortalCategoryRepository;
import com.messier333.proxyportal.portal.repository.PortalLinkRepository;
import com.messier333.proxyportal.portal.repository.PortalTabRepository;
import com.messier333.proxyportal.security.service.RememberMeTokenService;
import com.messier333.proxyportal.security.service.UserSessionService;
import com.messier333.proxyportal.user.entity.Role;
import com.messier333.proxyportal.user.entity.User;
import com.messier333.proxyportal.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private PortalTabRepository portalTabRepository;
    @Mock
    private PortalCategoryRepository portalCategoryRepository;
    @Mock
    private PortalLinkRepository portalLinkRepository;
    @Mock
    private RememberMeTokenService rememberMeTokenService;
    @Mock
    private UserSessionService userSessionService;

    @InjectMocks
    private UserService userService;

    @SuppressWarnings("null")
    @Test
    void createUser_shouldCreateWithTrimmedUsernameAndDefaultRole() {
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(passwordEncoder.encode("pass1234")).thenReturn("ENCODED");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = Objects.requireNonNull(invocation.getArgument(0, User.class));
            ReflectionTestUtils.setField(user, "id", 10L);
            return user;
        });

        long createdId = Objects.requireNonNull(userService.createUser(" alice ", "pass1234", null));

        assertThat(createdId).isEqualTo(10L);
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = Objects.requireNonNull(captor.getValue());
        assertThat(saved.getUsername()).isEqualTo("alice");
        assertThat(saved.getRole()).isEqualTo(Role.USER);
        assertThat(saved.getPassword()).isEqualTo("ENCODED");
    }

    @SuppressWarnings("null")
    @Test
    void createUser_shouldThrowWhenUsernameDuplicated() {
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser("alice", "pass1234", Role.USER))
                .isInstanceOf(ConflictException.class);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createUser_shouldThrowWhenUsernameBlank() {
        assertThatThrownBy(() -> userService.createUser("   ", "pass1234", Role.USER))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void createUser_shouldThrowWhenPasswordTooShort() {
        when(userRepository.existsByUsername("alice")).thenReturn(false);

        assertThatThrownBy(() -> userService.createUser("alice", "123", Role.USER))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void changePasswordById_shouldUpdatePassword() {
        User user = Objects.requireNonNull(User.createUser("alice", "OLD", Role.USER));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newpass")).thenReturn("ENC_NEW");

        userService.changePassword(1L, " newpass ");

        assertThat(user.getPassword()).isEqualTo("ENC_NEW");
        verify(rememberMeTokenService).invalidateAllTokensForUsername("alice");
        verify(userSessionService).invalidateAllSessionsForUsername("alice");
    }

    @Test
    void changePasswordById_shouldThrowWhenIdNull() {
        assertThatThrownBy(() -> userService.changePassword((Long) null, "newpass"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void changePasswordByUsername_shouldUpdatePassword() {
        User user = Objects.requireNonNull(User.createUser("alice", "OLD", Role.USER));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newpass")).thenReturn("ENC_NEW");

        userService.changePassword(" alice ", "newpass");

        assertThat(user.getPassword()).isEqualTo("ENC_NEW");
        verify(rememberMeTokenService).invalidateAllTokensForUsername("alice");
        verify(userSessionService).invalidateAllSessionsForUsername("alice");
    }

    @Test
    void changePasswordByUsername_shouldThrowWhenUserMissing() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.changePassword("ghost", "newpass"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void deleteUser_shouldThrowWhenActorDeletesSelf() {
        assertThatThrownBy(() -> userService.deleteUser("alice", "alice"))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void deleteUser_shouldThrowWhenTargetUserMissing() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteUser("admin", "ghost"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void deleteUser_shouldThrowWhenDeletingLastAdmin() {
        User admin = Objects.requireNonNull(User.createUser("admin2", "PW", Role.ADMIN));
        when(userRepository.findByUsername("admin2")).thenReturn(Optional.of(admin));
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> userService.deleteUser("root", "admin2"))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void deleteUser_shouldDeleteUserAndPortalResources() {
        User target = Objects.requireNonNull(User.createUser("target", "PW", Role.USER));
        when(userRepository.findByUsername("target")).thenReturn(Optional.of(target));

        userService.deleteUser("admin", " target ");

        verify(rememberMeTokenService).invalidateAllTokensForUsername("target");
        verify(userSessionService).invalidateAllSessionsForUsername("target");
        verify(portalLinkRepository).deleteByCategoryTabUserUsername("target");
        verify(portalCategoryRepository).deleteByTabUserUsername("target");
        verify(portalTabRepository).deleteByUserUsername("target");
        verify(userRepository).delete(target);
    }

    @Test
    void findAllUsers_shouldReturnRepositoryResult() {
        User first = Objects.requireNonNull(User.createUser("a", "p", Role.USER));
        User second = Objects.requireNonNull(User.createUser("b", "p", Role.ADMIN));
        when(userRepository.findAllByOrderByIdAsc()).thenReturn(List.of(first, second));

        List<User> result = userService.findAllUsers();

        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(first, second);
    }

    @Test
    void createInitialAdmin_shouldReturnFalseWhenAdminAlreadyExists() {
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(1L);

        boolean created = userService.createInitialAdmin("admin", "pass1234");

        assertThat(created).isFalse();
        verify(userRepository, never()).save(any(User.class));
    }

    @SuppressWarnings("null")
    @Test
    void createInitialAdmin_shouldCreateAdminWhenNoAdminExists() {
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(0L);
        when(userRepository.existsByUsername("admin")).thenReturn(false);
        when(passwordEncoder.encode("pass1234")).thenReturn("ENCODED");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = Objects.requireNonNull(invocation.getArgument(0, User.class));
            ReflectionTestUtils.setField(user, "id", 99L);
            return user;
        });

        boolean created = userService.createInitialAdmin(" admin ", "pass1234");

        assertThat(created).isTrue();
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(Objects.requireNonNull(captor.getValue()).getRole()).isEqualTo(Role.ADMIN);
        assertThat(captor.getValue().getUsername()).isEqualTo("admin");
    }

    @Test
    void forceLogoutAllDevices_shouldInvalidateRememberMeTokens() {
        User user = Objects.requireNonNull(User.createUser("alice", "PW", Role.USER));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        userService.forceLogoutAllDevices(" alice ");

        verify(rememberMeTokenService).invalidateAllTokensForUsername("alice");
        verify(userSessionService).invalidateAllSessionsForUsername("alice");
    }

    @Test
    void forceLogoutAllDevices_shouldThrowWhenUserMissing() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.forceLogoutAllDevices("ghost"))
                .isInstanceOf(NotFoundException.class);
    }
}
