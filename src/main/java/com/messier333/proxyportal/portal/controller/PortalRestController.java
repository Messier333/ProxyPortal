package com.messier333.proxyportal.portal.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.messier333.proxyportal.portal.dto.response.TabResponse;
import com.messier333.proxyportal.portal.service.PortalService;

import lombok.RequiredArgsConstructor;

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
    public void postPortalTab() {
        throw notImplemented();
    }

    @PostMapping("categories")
    public void postPortalCategory() {
        throw notImplemented();
    }

    @PostMapping("links")
    public void postPortalLink() {
        throw notImplemented();
    }

    private ResponseStatusException notImplemented() {
        return new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Not implemented yet");
    }
}
