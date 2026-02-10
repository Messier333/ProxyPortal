package com.messier333.proxyportal.security.controller;

import java.security.Principal;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.ui.Model;

import com.messier333.proxyportal.user.entity.Role;
import com.messier333.proxyportal.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class LoginController {
    private final UserRepository userRepository;

    @GetMapping("/login")
    public String showLoginPage(
            Principal principal,
            @RequestParam(name = "error", required = false) String error,
            @RequestParam(name = "logout", required = false) String logout,
            @RequestParam(name = "setup", required = false) String setup,
            Model model
    ) {
        if(principal != null) {
            return "redirect:/portal";
        }
        if (userRepository.countByRole(Role.ADMIN) == 0) {
            return "redirect:/setup";
        }
        if (error != null) {
            model.addAttribute("loginMessage", "아이디 또는 비밀번호가 올바르지 않습니다.");
        } else if (setup != null) {
            model.addAttribute("loginMessage", "관리자 계정이 생성되었습니다. 로그인하세요.");
        } else if (logout != null) {
            model.addAttribute("loginMessage", "로그아웃되었습니다.");
        }
        return "auth/login";
    }
}
