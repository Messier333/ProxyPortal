package com.messier333.proxyportal.security.controller;

import java.security.Principal;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.ui.Model;

import com.messier333.proxyportal.security.service.LoginAttemptMessages;
import com.messier333.proxyportal.user.entity.Role;
import com.messier333.proxyportal.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class LoginController {
    private static final String LOGIN_MESSAGE_ATTRIBUTE = "loginMessage";

    private final UserRepository userRepository;

    @GetMapping("/login")
    public String showLoginPage(
            Principal principal,
            @RequestParam(name = "error", required = false) String error,
            @RequestParam(name = "logout", required = false) String logout,
            @RequestParam(name = "setup", required = false) String setup,
            HttpServletRequest request,
            Model model
    ) {
        if (principal != null) {
            return "redirect:/portal";
        }
        if (userRepository.countByRole(Role.ADMIN) == 0) {
            return "redirect:/setup";
        }
        if (error != null) {
            Object sessionMessage = request.getSession(false) == null
                    ? null
                    : request.getSession(false).getAttribute(LoginAttemptMessages.SESSION_KEY);
            if (request.getSession(false) != null) {
                request.getSession(false).removeAttribute(LoginAttemptMessages.SESSION_KEY);
            }
            String loginMessage = sessionMessage instanceof String message && !message.isBlank()
                    ? message
                    : "아이디 또는 비밀번호가 올바르지 않습니다.";
            model.addAttribute(LOGIN_MESSAGE_ATTRIBUTE, loginMessage);
        } else if (setup != null) {
            model.addAttribute(LOGIN_MESSAGE_ATTRIBUTE, "관리자 계정이 생성되었습니다. 로그인하세요.");
        } else if (logout != null) {
            model.addAttribute(LOGIN_MESSAGE_ATTRIBUTE, "로그아웃되었습니다.");
        }
        return "auth/login";
    }
}
