package com.messier333.proxyportal.dashboard.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import com.messier333.proxyportal.proxygetter.service.ProxyGetterService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class DashboardController {
    private final ProxyGetterService proxyGetterService;
    @GetMapping("/dashboard")
    public String dashboardView(Authentication auth, Model model){
        boolean isAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if(isAdmin){
            model.addAttribute("npmHosts", proxyGetterService.getProxyHostsList());
            return "dashboard/admin/admin";
        }
        return "dashboard/user/user";
    }
}
