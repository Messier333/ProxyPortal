package com.messier333.proxyportal.security.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class LoginControllerTest {

    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new LoginController()).build();

    @Test
    void showLoginPage_shouldRedirectToPortalWhenAlreadyAuthenticated() throws Exception {
        mockMvc.perform(get("/login").principal(() -> "alice"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/portal"));
    }

    @Test
    void showLoginPage_shouldShowErrorMessageWhenErrorParamExists() throws Exception {
        mockMvc.perform(get("/login").param("error", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"))
                .andExpect(model().attribute("loginMessage", "아이디 또는 비밀번호가 올바르지 않습니다."));
    }

    @Test
    void showLoginPage_shouldShowLogoutMessageWhenLogoutParamExists() throws Exception {
        mockMvc.perform(get("/login").param("logout", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"))
                .andExpect(model().attribute("loginMessage", "로그아웃되었습니다."));
    }

    @Test
    void showLoginPage_shouldRenderLoginViewWithoutMessage() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"))
                .andExpect(model().attributeDoesNotExist("loginMessage"));
    }
}
