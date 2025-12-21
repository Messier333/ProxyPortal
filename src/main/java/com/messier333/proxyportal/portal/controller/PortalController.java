package com.messier333.proxyportal.portal.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.messier333.proxyportal.portal.dto.PortalConfigResponse;
import com.messier333.proxyportal.portal.service.PortalResponseService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class PortalController {
    private final PortalResponseService portalResponseService;
    private final ObjectMapper objectMapper;

    @GetMapping("/")
    public String home() {
        return "redirect:/portal";
    }
    @GetMapping("/portal")
    public String showPortal(Model model) throws JsonProcessingException {
        PortalConfigResponse config = portalResponseService.getPortalCategories();
        String portalConfigJson = objectMapper.writeValueAsString(config);
        System.out.println(portalConfigJson);
        model.addAttribute("portalConfigJson", portalConfigJson);
        return "portal/index";
    }
}
