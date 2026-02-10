package com.messier333.proxyportal.security.controller;

import java.security.Principal;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.ui.Model;


@Controller
public class LoginController {
    @GetMapping("/login")
    public String showLoginPage(
            Principal principal,
            @RequestParam(name = "error", required = false) String error,
            @RequestParam(name = "logout", required = false) String logout,
            Model model
    ) {
        if(principal != null) {
            return "redirect:/portal";
        }
        if (error != null) {
            model.addAttribute("loginMessage", "아이디 또는 비밀번호가 올바르지 않습니다.");
        } else if (logout != null) {
            model.addAttribute("loginMessage", "로그아웃되었습니다.");
        }
        return "auth/login";
    }
}
