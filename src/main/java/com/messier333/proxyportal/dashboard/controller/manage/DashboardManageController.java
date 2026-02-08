package com.messier333.proxyportal.dashboard.controller.manage;

import java.util.Objects;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.messier333.proxyportal.portal.dto.request.CategoryCreateRequest;
import com.messier333.proxyportal.portal.dto.request.TabCreateRequest;
import com.messier333.proxyportal.portal.dto.response.CategoryResponse;
import com.messier333.proxyportal.portal.service.PortalService;
import com.messier333.proxyportal.proxygetter.service.ProxyGetterService;

import lombok.RequiredArgsConstructor;


@Controller
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardManageController {
    private final ProxyGetterService proxyGetterService;
    private final PortalService portalService;
    
    @SuppressWarnings("null")
    @GetMapping("/manage")
    public String manageView(Authentication auth, Model model){
        boolean isAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if(isAdmin){
            model.addAttribute("npmLinks", proxyGetterService.getProxyHostsList());
        }
        model.addAttribute("tabs", portalService.getPortalTabs(Objects.requireNonNull(auth).getName()).tabs());
        model.addAttribute("categories");
        return "dashboard/manage";
    }

    @PostMapping("/manage/tabs")
    public String createTab(
            @RequestParam("name") String name,
            @RequestParam(name = "backgroundImage", required = false) MultipartFile backgroundImage,
            @AuthenticationPrincipal User user
    ){
        var tab = portalService.createTab(user.getUsername(), new TabCreateRequest(name,1, null));
        portalService.uploadTabBackground(user.getUsername(), tab.id(), backgroundImage);
        
        return "redirect:/dashboard/manage";
    }

    @PostMapping("/manage/tabs/{tabId}/background/remove")
    public String removeTabBackground(@PathVariable("tabId") Long tabId, @AuthenticationPrincipal User user) {
        portalService.clearTabBackground(user.getUsername(), tabId);
        return "redirect:/dashboard/manage";
    }

    @PostMapping("/manage/tabs/{tabId}/background")
    public String uploadTabBackground(
            @PathVariable("tabId") Long tabId,
            @RequestParam("backgroundImage") MultipartFile backgroundImage,
            @AuthenticationPrincipal User user
    ) {
        portalService.uploadTabBackground(user.getUsername(), tabId, backgroundImage);
        return "redirect:/dashboard/manage";
    }

    @PostMapping("/manage/tabs/{tabId}/rename")
    public String renameTab(
            @PathVariable("tabId") Long tabId,
            @RequestParam("name") String name,
            @AuthenticationPrincipal User user
    ) {
        portalService.renameTab(user.getUsername(), tabId, name);
        return "redirect:/dashboard/manage";
    }

    @PostMapping("/manage/categories")
    public String postMethodName(@RequestParam("name") String name, @RequestParam("tabId") Long tabId, @AuthenticationPrincipal User user) {
        CategoryResponse categoryResponse = portalService.createCategory(user.getUsername(), tabId, new CategoryCreateRequest(name, null, 1));
        portalService.addCategorytoTab(user.getUsername(), tabId, categoryResponse.id());
        return "redirect:/dashboard/manage";
    }    
}
