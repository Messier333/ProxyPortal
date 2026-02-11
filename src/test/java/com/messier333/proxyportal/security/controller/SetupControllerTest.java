package com.messier333.proxyportal.security.controller;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

import com.messier333.proxyportal.common.exception.BadRequestException;
import com.messier333.proxyportal.user.entity.Role;
import com.messier333.proxyportal.user.repository.UserRepository;
import com.messier333.proxyportal.user.service.UserService;

@ExtendWith(MockitoExtension.class)
class SetupControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new SetupController(userRepository, userService)).build();
    }

    @Test
    void setupView_shouldRedirectLoginWhenAdminAlreadyExists() throws Exception {
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(1L);

        mockMvc.perform(get("/setup"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void setupView_shouldRenderSetupPageWhenNoAdminExists() throws Exception {
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(0L);

        mockMvc.perform(get("/setup"))
                .andExpect(status().isOk())
                .andExpect(view().name("setup/index"));
    }

    @Test
    void setupSubmit_shouldRedirectLoginWhenAdminAlreadyExists() throws Exception {
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(1L);

        mockMvc.perform(post("/setup")
                        .param("username", "admin")
                        .param("password", "secret")
                        .param("confirmPassword", "secret"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void setupSubmit_shouldShowErrorWhenPasswordDoesNotMatch() throws Exception {
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(0L);

        mockMvc.perform(post("/setup")
                        .param("username", "admin")
                        .param("password", "secret1")
                        .param("confirmPassword", "secret2"))
                .andExpect(status().isOk())
                .andExpect(view().name("setup/index"))
                .andExpect(model().attribute("errorMessage", "비밀번호가 일치하지 않습니다."));
    }

    @Test
    void setupSubmit_shouldRedirectLoginWithSetupFlagWhenAdminCreated() throws Exception {
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(0L);
        when(userService.createInitialAdmin("admin", "secret")).thenReturn(true);

        mockMvc.perform(post("/setup")
                        .param("username", "admin")
                        .param("password", "secret")
                        .param("confirmPassword", "secret"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?setup=1"));
    }

    @Test
    void setupSubmit_shouldRedirectLoginWhenCreateInitialAdminReturnsFalse() throws Exception {
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(0L);
        when(userService.createInitialAdmin("admin", "secret")).thenReturn(false);

        mockMvc.perform(post("/setup")
                        .param("username", "admin")
                        .param("password", "secret")
                        .param("confirmPassword", "secret"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void setupSubmit_shouldExposeBusinessErrorMessage() throws Exception {
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(0L);
        doThrow(new BadRequestException("invalid username"))
                .when(userService).createInitialAdmin("admin", "secret");

        mockMvc.perform(post("/setup")
                        .param("username", "admin")
                        .param("password", "secret")
                        .param("confirmPassword", "secret"))
                .andExpect(status().isOk())
                .andExpect(view().name("setup/index"))
                .andExpect(model().attribute("errorMessage", "invalid username"));
    }
}
