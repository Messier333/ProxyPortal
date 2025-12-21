package com.messier333.proxyportal.portal.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.messier333.proxyportal.portal.dto.PortalConfigResponse;
import com.messier333.proxyportal.portal.service.PortalService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class PortalController {
    private final PortalService portalService;
    private final ObjectMapper objectMapper;

    @GetMapping("/")
    public String home() {
        return "redirect:/portal";
    }
    @GetMapping("/portal")
    public String showPortal(Model model) throws JsonProcessingException {
        PortalConfigResponse config = portalService.getPortalCategories();
        String portalConfigJson = objectMapper.writeValueAsString(config);
        model.addAttribute("portalConfigJson", portalConfigJson);
        return "portal/index";
    }
}
