package com.messier333.proxyportal.auth;

import java.security.Principal;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;


@Controller
public class LoginController {

    @GetMapping("/login")
    public String showLoginPage(Principal principal) {
        if(principal != null) {
            return "redirect:/portal";
        }
        return "auth/login";
    }
}
