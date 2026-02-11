package com.messier333.proxyportal.security.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.messier333.proxyportal.user.entity.Role;
import com.messier333.proxyportal.user.repository.UserRepository;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginControllerTest {

    @Mock
    private UserRepository userRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(new LoginController(userRepository)).build();
    }

    @Test
    void showLoginPage_shouldRedirectToPortalWhenAlreadyAuthenticated() throws Exception {
        mockMvc.perform(get("/login").principal(() -> "alice"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/portal"));
    }

    @Test
    void showLoginPage_shouldShowErrorMessageWhenErrorParamExists() throws Exception {
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(1L);
        mockMvc.perform(get("/login").param("error", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"))
                .andExpect(model().attribute("loginMessage", "아이디 또는 비밀번호가 올바르지 않습니다."));
    }

    @Test
    void showLoginPage_shouldShowLogoutMessageWhenLogoutParamExists() throws Exception {
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(1L);
        mockMvc.perform(get("/login").param("logout", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"))
                .andExpect(model().attribute("loginMessage", "로그아웃되었습니다."));
    }

    @Test
    void showLoginPage_shouldShowSetupMessageWhenSetupParamExists() throws Exception {
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(1L);
        mockMvc.perform(get("/login").param("setup", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"))
                .andExpect(model().attribute("loginMessage", "관리자 계정이 생성되었습니다. 로그인하세요."));
    }

    @Test
    void showLoginPage_shouldRenderLoginViewWithoutMessage() throws Exception {
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(1L);
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"))
                .andExpect(model().attributeDoesNotExist("loginMessage"));
    }

    @Test
    void showLoginPage_shouldRedirectToSetupWhenNoAdminExists() throws Exception {
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(0L);

        mockMvc.perform(get("/login"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/setup"));
    }
}
