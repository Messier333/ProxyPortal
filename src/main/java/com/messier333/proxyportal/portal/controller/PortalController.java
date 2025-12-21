package com.messier333.proxyportal.portal.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PortalController {
    @GetMapping("/")
    public String home() {
        return "redirect:/portal";
    }
    @GetMapping("/portal")
    public String showPortal() {
        return "portal/index";
    }
}
