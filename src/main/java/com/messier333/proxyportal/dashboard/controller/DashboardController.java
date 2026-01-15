package com.messier333.proxyportal.dashboard.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.messier333.proxyportal.portal.dto.request.TabCreateRequest;
import com.messier333.proxyportal.portal.service.PortalService;
import com.messier333.proxyportal.proxygetter.service.ProxyGetterService;

import lombok.RequiredArgsConstructor;


@Controller
@RequiredArgsConstructor
public class DashboardController {
    private final ProxyGetterService proxyGetterService;
    private final PortalService portalService;

    @GetMapping("/dashboard")
    public String dashboardView(){
        return "dashboard/index";
    }

    @SuppressWarnings("null")
    @GetMapping("/dashboard/manage")
    public String manageView(Authentication auth, Model model){
        boolean isAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if(isAdmin){
            model.addAttribute("npmLinks", proxyGetterService.getProxyHostsList());
        }
        model.addAttribute("tabs", portalService.getPortalTabs(auth.getName()).tabs());
        return "dashboard/manage";
    }

    @GetMapping("/dashboard/account/add")
    public String addAccountView(@RequestParam(name="error", required=false) String error, Model model){
        if(error != null){
            model.addAttribute("error", error);
        }
        return "dashboard/account-add";
    }
    
    @PostMapping("/dashboard/tabs")
    public String createTab(@RequestParam("name") String name, @AuthenticationPrincipal User user){ 
        portalService.createTab(user.getUsername(), new TabCreateRequest(name,1, null));
        
        return "redirect:/dashboard/manage";
    }
}
