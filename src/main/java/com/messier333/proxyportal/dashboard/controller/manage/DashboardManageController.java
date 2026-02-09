package com.messier333.proxyportal.dashboard.controller.manage;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
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
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.messier333.proxyportal.portal.dto.request.CategoryCreateRequest;
import com.messier333.proxyportal.portal.dto.request.TabCreateRequest;
import com.messier333.proxyportal.portal.dto.response.CategoryResponse;
import com.messier333.proxyportal.portal.dto.response.LinkResponse;
import com.messier333.proxyportal.portal.service.PortalService;
import com.messier333.proxyportal.proxygetter.service.ProxyGetterService;

import lombok.RequiredArgsConstructor;


@Controller
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardManageController {
    private final PortalService portalService;
    private final ProxyGetterService proxyGetterService;
    
    @SuppressWarnings("null")
    @GetMapping("/manage")
    public String manageView(Authentication auth, Model model){
        String username = Objects.requireNonNull(auth).getName();
        var tabs = portalService.getPortalTabs(username).tabs();
        var categories = tabs.stream()
                .flatMap(tab -> tab.categories().stream())
                .collect(Collectors.toList());
        var links = categories.stream()
                .flatMap(category -> category.links().stream())
                .collect(Collectors.toList());
        model.addAttribute("tabs", tabs);
        model.addAttribute("categories", categories);
        model.addAttribute("links", links);
        model.addAttribute("npmLinks", proxyGetterService.getProxyHostsList());
        return "dashboard/manage";
    }

    @PostMapping("/manage/tabs")
    public String createTab(
            @RequestParam("name") String name,
            @RequestParam(name = "sortOrder", required = false) Integer sortOrder,
            @RequestParam(name = "backgroundImage", required = false) MultipartFile backgroundImage,
            @AuthenticationPrincipal User user,
            RedirectAttributes redirectAttributes
    ){
        try {
            var tab = portalService.createTab(user.getUsername(), new TabCreateRequest(name, sortOrder, null));
            portalService.uploadTabBackground(user.getUsername(), tab.id(), backgroundImage);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("toastMessage", e.getMessage());
        }
        
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

    @PostMapping("/tabs/{tabId}/delete")
    public String deleteTab(
            @PathVariable("tabId") Long tabId,
            @AuthenticationPrincipal User user,
            RedirectAttributes redirectAttributes
    ) {
        try {
            portalService.deleteTab(user.getUsername(), tabId);
            redirectAttributes.addFlashAttribute("toastMessage", "탭을 삭제했습니다.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("toastMessage", e.getMessage());
        }
        return "redirect:/dashboard/manage";
    }

    @PostMapping("/manage/categories")
    public String postMethodName(
            @RequestParam("name") String name,
            @RequestParam("tabId") Long tabId,
            @RequestParam(name = "sortOrder", required = false) Integer sortOrder,
            @AuthenticationPrincipal User user,
            RedirectAttributes redirectAttributes
    ) {
        try {
            CategoryResponse categoryResponse = portalService.createCategory(user.getUsername(), tabId, new CategoryCreateRequest(name, null, sortOrder));
            portalService.addCategorytoTab(user.getUsername(), tabId, categoryResponse.id());
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("toastMessage", e.getMessage());
        }
        return "redirect:/dashboard/manage";
    }

    @PostMapping("/categories/{categoryId}/delete")
    public String deleteCategory(
            @PathVariable("categoryId") Long categoryId,
            @AuthenticationPrincipal User user,
            RedirectAttributes redirectAttributes
    ) {
        try {
            portalService.deleteCategory(user.getUsername(), categoryId);
            redirectAttributes.addFlashAttribute("toastMessage", "카테고리를 삭제했습니다.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("toastMessage", e.getMessage());
        }
        return "redirect:/dashboard/manage";
    }

    @PostMapping("/links")
    public String createLink(
            @RequestParam("name") String name,
            @RequestParam("url") String url,
            @RequestParam("categoryId") Long categoryId,
            @RequestParam(name = "sortOrder", required = false) Integer sortOrder,
            @RequestParam(name = "icon", required = false) String icon,
            @RequestParam(name = "iconColor", required = false) String iconColor,
            @AuthenticationPrincipal User user,
            RedirectAttributes redirectAttributes
    ) {
        try {
            LinkResponse response = portalService.createLink(user.getUsername(), categoryId, name, url, icon, iconColor, sortOrder);
            Objects.requireNonNull(response, "link response must not be null");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("toastMessage", e.getMessage());
        }
        return "redirect:/dashboard/manage";
    }

    @PostMapping("/links/{linkId}/delete")
    public String deleteLink(
            @PathVariable("linkId") Long linkId,
            @AuthenticationPrincipal User user,
            RedirectAttributes redirectAttributes
    ) {
        try {
            portalService.deleteLink(user.getUsername(), linkId);
            redirectAttributes.addFlashAttribute("toastMessage", "링크를 삭제했습니다.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("toastMessage", e.getMessage());
        }
        return "redirect:/dashboard/manage";
    }

    @PostMapping("/manage/tabs/{tabId}/sort")
    public String updateTabSort(
            @PathVariable("tabId") Long tabId,
            @RequestParam("sortOrder") Integer sortOrder,
            @AuthenticationPrincipal User user,
            RedirectAttributes redirectAttributes
    ) {
        try {
            portalService.updateTabSortOrder(user.getUsername(), tabId, sortOrder);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("toastMessage", e.getMessage());
        }
        return "redirect:/dashboard/manage";
    }

    @PostMapping("/manage/categories/{categoryId}/sort")
    public String updateCategorySort(
            @PathVariable("categoryId") Long categoryId,
            @RequestParam("sortOrder") Integer sortOrder,
            @AuthenticationPrincipal User user,
            RedirectAttributes redirectAttributes
    ) {
        try {
            portalService.updateCategorySortOrder(user.getUsername(), categoryId, sortOrder);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("toastMessage", e.getMessage());
        }
        return "redirect:/dashboard/manage";
    }

    @PostMapping("/manage/links/{linkId}/sort")
    public String updateLinkSort(
            @PathVariable("linkId") Long linkId,
            @RequestParam("sortOrder") Integer sortOrder,
            @AuthenticationPrincipal User user,
            RedirectAttributes redirectAttributes
    ) {
        try {
            portalService.updateLinkSortOrder(user.getUsername(), linkId, sortOrder);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("toastMessage", e.getMessage());
        }
        return "redirect:/dashboard/manage";
    }

    @ResponseBody
    @PostMapping("/manage/reorder/tabs")
    public ResponseEntity<Void> reorderTabs(
            @RequestParam("ids") String ids,
            @AuthenticationPrincipal User user
    ) {
        try {
            portalService.reorderTabsByIds(user.getUsername(), parseOrderedIds(ids));
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @ResponseBody
    @PostMapping("/manage/reorder/categories")
    public ResponseEntity<Void> reorderCategories(
            @RequestParam("parentId") Long tabId,
            @RequestParam("ids") String ids,
            @AuthenticationPrincipal User user
    ) {
        try {
            portalService.reorderCategoriesByIds(user.getUsername(), tabId, parseOrderedIds(ids));
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @ResponseBody
    @PostMapping("/manage/reorder/links")
    public ResponseEntity<Void> reorderLinks(
            @RequestParam("parentId") Long categoryId,
            @RequestParam("ids") String ids,
            @AuthenticationPrincipal User user
    ) {
        try {
            portalService.reorderLinksByIds(user.getUsername(), categoryId, parseOrderedIds(ids));
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    private List<Long> parseOrderedIds(String rawIds) {
        if (rawIds == null || rawIds.isBlank()) {
            throw new IllegalArgumentException("ids must not be blank");
        }
        try {
            return Arrays.stream(rawIds.split(","))
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .map(Long::parseLong)
                    .toList();
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("invalid ids format", e);
        }
    }
}
