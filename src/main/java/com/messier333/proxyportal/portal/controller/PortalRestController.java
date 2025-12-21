package com.messier333.proxyportal.portal.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.messier333.proxyportal.portal.dto.PortalConfigResponse;
import com.messier333.proxyportal.portal.service.PortalResponseService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/portal/")
@RequiredArgsConstructor
public class PortalRestController {
    private final PortalResponseService portalResponseService;

    @GetMapping("categories")
    public PortalConfigResponse getPortalConfig() {
        return portalResponseService.getPortalCategories();
    }
}
