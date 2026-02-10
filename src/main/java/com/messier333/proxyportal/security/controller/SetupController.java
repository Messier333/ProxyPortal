package com.messier333.proxyportal.security.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.messier333.proxyportal.common.exception.BusinessException;
import com.messier333.proxyportal.user.entity.Role;
import com.messier333.proxyportal.user.repository.UserRepository;
import com.messier333.proxyportal.user.service.UserService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class SetupController {
    private final UserRepository userRepository;
    private final UserService userService;

    @GetMapping("/setup")
    public String setupView() {
        if (hasAdmin()) {
            return "redirect:/login";
        }
        return "setup/index";
    }

    @PostMapping("/setup")
    public String setupSubmit(
            @RequestParam("username") String username,
            @RequestParam("password") String password,
            @RequestParam("confirmPassword") String confirmPassword,
            Model model
    ) {
        if (hasAdmin()) {
            return "redirect:/login";
        }
        if (!password.equals(confirmPassword)) {
            model.addAttribute("errorMessage", "비밀번호가 일치하지 않습니다.");
            return "setup/index";
        }
        try {
            boolean created = userService.createInitialAdmin(username, password);
            return created ? "redirect:/login?setup=1" : "redirect:/login";
        } catch (BusinessException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "setup/index";
        }
    }

    private boolean hasAdmin() {
        return userRepository.countByRole(Role.ADMIN) > 0;
    }
}
