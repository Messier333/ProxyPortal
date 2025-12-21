package com.messier333.proxyportal.portal.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.messier333.proxyportal.portal.dto.PortalConfigResponse;
import com.messier333.proxyportal.portal.entity.PortalCategory;
import com.messier333.proxyportal.portal.entity.PortalLink;
import com.messier333.proxyportal.portal.entity.PortalTab;
import com.messier333.proxyportal.portal.service.PortalService;

import lombok.RequiredArgsConstructor;



@RestController
@RequestMapping("/api/portal/")
@RequiredArgsConstructor
public class PortalRestController {
    private final PortalService portalService;

    @GetMapping("categories")
    public PortalConfigResponse getPortalConfig() {
        return portalService.getPortalCategories();
    }

    @PostMapping("tabs")
    public PortalTab postPortalTab() {
        return null;
    }

    @PostMapping("categories")
    public PortalCategory postPortalCategory() {
        return null;
    }

    @PostMapping("links")
    public PortalLink postPortalLink() {
        return null;
    }
}
