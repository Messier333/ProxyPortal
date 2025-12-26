package com.messier333.proxyportal.portal.controller;

import com.messier333.proxyportal.portal.dto.response.TabResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.messier333.proxyportal.portal.entity.PortalCategory;
import com.messier333.proxyportal.portal.entity.PortalLink;
import com.messier333.proxyportal.portal.entity.PortalTab;
import com.messier333.proxyportal.portal.service.PortalService;

import lombok.RequiredArgsConstructor;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/portal/")
@RequiredArgsConstructor
public class PortalRestController {
    private final PortalService portalService;

    @GetMapping("tabs")
    public List<TabResponse> getPortalTabs(Principal principal) {
        return portalService.getPortalTabs(principal.getName()).tabs();
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
