package com.messier333.proxyportal.dashboard.controller;

import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import com.messier333.proxyportal.common.exception.BadRequestException;
import com.messier333.proxyportal.portal.dto.response.CategoryResponse;
import com.messier333.proxyportal.portal.dto.response.LinkResponse;
import com.messier333.proxyportal.portal.dto.response.PortalTabsResponse;
import com.messier333.proxyportal.portal.dto.response.TabResponse;
import com.messier333.proxyportal.portal.service.PortalService;
import com.messier333.proxyportal.user.entity.Role;
import com.messier333.proxyportal.user.entity.User;
import com.messier333.proxyportal.user.service.UserService;

@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

    @Mock
    private PortalService portalService;

    @Mock
    private UserService userService;

    @InjectMocks
    private DashboardController dashboardController;

    @Test
    void dashboardView_shouldPopulateTabsCategoriesLinks() {
        Authentication auth = adminAuth("alice");
        ExtendedModelMap model = new ExtendedModelMap();
        LinkResponse link = new LinkResponse(3L, "Google", "https://google.com", "link", "#fff", 1);
        CategoryResponse category = new CategoryResponse(2L, "Search", 1, List.of(link));
        TabResponse tab = new TabResponse(1L, "Main", 1, null, List.of(category));
        when(portalService.getPortalTabs("alice")).thenReturn(new PortalTabsResponse(List.of(tab)));

        String view = dashboardController.dashboardView(auth, model);

        assertThat(view).isEqualTo("dashboard/index");
        assertThat(model.get("tabs")).isEqualTo(List.of(tab));
        assertThat(model.get("categories")).isEqualTo(List.of(category));
        assertThat(model.get("links")).isEqualTo(List.of(link));
        assertThat(model.get("isAdmin")).isEqualTo(true);
    }

    @Test
    void addAccountView_shouldRedirectWhenNonAdmin() {
        Authentication auth = userAuth("alice");
        ExtendedModelMap model = new ExtendedModelMap();

        String view = dashboardController.addAccountView(auth, model);

        assertThat(view).isEqualTo("redirect:/dashboard?error=admin-only");
        verifyNoInteractions(userService);
    }

    @SuppressWarnings("deprecation")
    @Test
    void addAccountView_shouldRenderViewForAdmin() {
        Authentication auth = adminAuth("admin");
        ExtendedModelMap model = new ExtendedModelMap();
        User user = Objects.requireNonNull(User.createUser("alice", "ENC", Role.USER));
        when(userService.findAllUsers()).thenReturn(List.of(user));

        String view = dashboardController.addAccountView(auth, model);

        assertThat(view).isEqualTo("dashboard/account-add");
        assertThat(model.get("isAdmin")).isEqualTo(true);
        assertThat(model.get("currentUsername")).isEqualTo("admin");
        assertThat(model.get("users")).asList().hasSize(1);
    }

    @Test
    void createAccount_shouldCreateUserWhenAdmin() {
        Authentication auth = adminAuth("admin");
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        String view = dashboardController.createAccount(auth, "new-user", "pass1234", "ADMIN", redirect);

        assertThat(view).isEqualTo("redirect:/dashboard/account/add");
        verify(userService).createUser("new-user", "pass1234", Role.ADMIN);
        assertThat(redirect.getFlashAttributes().get("toastMessage")).isEqualTo("계정을 추가했습니다.");
    }

    @Test
    void createAccount_shouldHandleInvalidRoleAsBusinessException() {
        Authentication auth = adminAuth("admin");
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        String view = dashboardController.createAccount(auth, "new-user", "pass1234", "invalid", redirect);

        assertThat(view).isEqualTo("redirect:/dashboard/account/add");
        assertThat(redirect.getFlashAttributes().get("toastMessage")).isEqualTo(
                "지원하지 않는 권한입니다. USER 또는 ADMIN만 가능합니다.");
    }

    @Test
    void createAccount_shouldBlockNonAdmin() {
        Authentication auth = userAuth("alice");
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        String view = dashboardController.createAccount(auth, "new-user", "pass1234", "USER", redirect);

        assertThat(view).isEqualTo("redirect:/dashboard?error=admin-only");
        verifyNoInteractions(userService);
    }

    @Test
    void changeAccountPassword_shouldDelegateWhenAdmin() {
        Authentication auth = adminAuth("admin");
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        String view = dashboardController.changeAccountPassword(auth, "alice", "new-pass", redirect);

        assertThat(view).isEqualTo("redirect:/dashboard/account/add");
        verify(userService).changePassword("alice", "new-pass");
        assertThat(redirect.getFlashAttributes().get("toastMessage")).isEqualTo("비밀번호를 변경했습니다.");
    }

    @Test
    void deleteAccount_shouldDelegateWhenAdmin() {
        Authentication auth = adminAuth("admin");
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        org.springframework.security.core.userdetails.User principal =
                new org.springframework.security.core.userdetails.User("admin", "pw", List.of(() -> "ROLE_ADMIN"));

        String view = dashboardController.deleteAccount(auth, principal, "alice", redirect);

        assertThat(view).isEqualTo("redirect:/dashboard/account/add");
        verify(userService).deleteUser(eq("admin"), eq("alice"));
        assertThat(redirect.getFlashAttributes().get("toastMessage")).isEqualTo("계정을 삭제했습니다.");
    }

    @Test
    void changeAccountPassword_shouldExposeBusinessErrorMessage() {
        Authentication auth = adminAuth("admin");
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        doThrow(new BadRequestException("bad password"))
                .when(userService).changePassword("ghost", "new-pass");

        String view = dashboardController.changeAccountPassword(auth, "ghost", "new-pass", redirect);

        assertThat(view).isEqualTo("redirect:/dashboard/account/add");
        assertThat(redirect.getFlashAttributes().get("toastMessage")).isEqualTo("bad password");
    }

    private static Authentication adminAuth(String username) {
        return new TestingAuthenticationToken(username, "pw", "ROLE_ADMIN");
    }

    private static Authentication userAuth(String username) {
        return new TestingAuthenticationToken(username, "pw", "ROLE_USER");
    }
}
