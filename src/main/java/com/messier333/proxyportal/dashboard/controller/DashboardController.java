package com.messier333.proxyportal.dashboard.controller;

import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.messier333.proxyportal.common.exception.BadRequestException;
import com.messier333.proxyportal.common.exception.BusinessException;
import com.messier333.proxyportal.dashboard.dto.UserDashboardVM;
import com.messier333.proxyportal.portal.service.PortalService;
import com.messier333.proxyportal.user.entity.Role;
import com.messier333.proxyportal.user.service.UserService;

import lombok.RequiredArgsConstructor;


@Controller
@RequiredArgsConstructor
public class DashboardController {
    private static final String REDIRECT_DASHBOARD = "redirect:/dashboard";
    private static final String REDIRECT_ACCOUNT_PAGE = "redirect:/dashboard/account/add";
    private static final String REDIRECT_ADMIN_ONLY = REDIRECT_DASHBOARD + "?error=admin-only";

    private final PortalService portalService;
    private final UserService userService;

    @GetMapping("/dashboard")
    public String dashboardView(Authentication auth, Model model){
        String username = Objects.requireNonNull(auth).getName();
        boolean isAdmin = isAdmin(auth);
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
        model.addAttribute("isAdmin", isAdmin);
        return "dashboard/index";
    }


    @GetMapping("/dashboard/account/add")
    public String addAccountView(
            Authentication auth,
            Model model
    ){
        if (!isAdmin(auth)) {
            return REDIRECT_ADMIN_ONLY;
        }
        model.addAttribute("isAdmin", true);

        var users = userService.findAllUsers().stream()
                .map(user -> new UserDashboardVM(user.getId(), user.getUsername(), user.getRole()))
                .collect(Collectors.toList());
        model.addAttribute("users", users);
        model.addAttribute("currentUsername", Objects.requireNonNull(auth).getName());
        return "dashboard/account-add";
    }

    @PostMapping("/dashboard/account/users")
    public String createAccount(
            Authentication auth,
            @RequestParam("username") String username,
            @RequestParam("password") String password,
            @RequestParam("role") String role,
            RedirectAttributes redirectAttributes
    ) {
        return handleAdminAction(
                auth,
                redirectAttributes,
                () -> userService.createUser(username, password, parseRole(role)),
                "계정을 추가했습니다."
        );
    }

    @PostMapping("/dashboard/account/password")
    public String changeAccountPassword(
            Authentication auth,
            @RequestParam("username") String username,
            @RequestParam("newPassword") String newPassword,
            RedirectAttributes redirectAttributes
    ) {
        return handleAdminAction(
                auth,
                redirectAttributes,
                () -> userService.changePassword(username, newPassword),
                "비밀번호를 변경했습니다."
        );
    }

    @PostMapping("/dashboard/account/delete")
    public String deleteAccount(
            Authentication auth,
            @AuthenticationPrincipal User user,
            @RequestParam("username") String username,
            RedirectAttributes redirectAttributes
    ) {
        return handleAdminAction(
                auth,
                redirectAttributes,
                () -> userService.deleteUser(Objects.requireNonNull(user).getUsername(), username),
                "계정을 삭제했습니다."
        );
    }

    @PostMapping("/dashboard/account/logout-all")
    public String forceLogoutAllDevices(
            Authentication auth,
            @RequestParam("username") String username,
            RedirectAttributes redirectAttributes
    ) {
        return handleAdminAction(
                auth,
                redirectAttributes,
                () -> userService.forceLogoutAllDevices(username),
                "모든 기기 로그아웃을 처리했습니다."
        );
    }

    private boolean isAdmin(Authentication auth) {
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    private Role parseRole(String role) {
        if (role == null || role.isBlank()) {
            return Role.USER;
        }
        try {
            return Role.valueOf(role.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("지원하지 않는 권한입니다. USER 또는 ADMIN만 가능합니다.");
        }
    }

    private String handleAdminAction(
            Authentication auth,
            RedirectAttributes redirectAttributes,
            Runnable action,
            String successMessage
    ) {
        if (!isAdmin(auth)) {
            return REDIRECT_ADMIN_ONLY;
        }
        try {
            action.run();
            redirectAttributes.addFlashAttribute("toastMessage", successMessage);
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("toastMessage", e.getMessage());
        }
        return REDIRECT_ACCOUNT_PAGE;
    }
}
