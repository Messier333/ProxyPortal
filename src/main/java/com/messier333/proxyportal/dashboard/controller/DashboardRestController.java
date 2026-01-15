package com.messier333.proxyportal.dashboard.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.messier333.proxyportal.portal.dto.request.TabCreateRequest;
import com.messier333.proxyportal.portal.service.PortalService;

import lombok.RequiredArgsConstructor;




@RestController
@RequiredArgsConstructor
@RequestMapping("/dashboard")
public class DashboardRestController {
    private final PortalService portalService;

    
    @PostMapping("/tabs")
    public String createTab(@RequestParam("name") String name, Authentication auth){ 
        portalService.createTab(auth.getName(), new TabCreateRequest(name,1, null));
        
        return name;
    }
    
}
