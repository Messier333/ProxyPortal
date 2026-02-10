package com.messier333.proxyportal.dashboard.controller.manage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import com.messier333.proxyportal.common.exception.BadRequestException;
import com.messier333.proxyportal.portal.dto.request.CategoryCreateRequest;
import com.messier333.proxyportal.portal.dto.request.TabCreateRequest;
import com.messier333.proxyportal.portal.dto.response.CategoryResponse;
import com.messier333.proxyportal.portal.dto.response.LinkResponse;
import com.messier333.proxyportal.portal.dto.response.PortalTabsResponse;
import com.messier333.proxyportal.portal.dto.response.TabResponse;
import com.messier333.proxyportal.portal.service.PortalService;
import com.messier333.proxyportal.proxygetter.service.ProxyGetterService;

@ExtendWith(MockitoExtension.class)
class DashboardManageControllerTest {

    @Mock
    private PortalService portalService;

    @Mock
    private ProxyGetterService proxyGetterService;

    @InjectMocks
    private DashboardManageController dashboardManageController;

    @Test
    void manageView_shouldIncludeNpmLinksForAdmin() {
        Authentication auth = adminAuth("admin");
        ExtendedModelMap model = new ExtendedModelMap();
        LinkResponse link = new LinkResponse(3L, "Google", "https://google.com", "link", "#fff", 1);
        CategoryResponse category = new CategoryResponse(2L, "Search", 1, List.of(link));
        TabResponse tab = new TabResponse(1L, "Main", 1, null, List.of(category));
        when(portalService.getPortalTabs("admin")).thenReturn(new PortalTabsResponse(List.of(tab)));
        when(proxyGetterService.getProxyHostsList()).thenReturn(List.of("a.example.com"));

        String view = dashboardManageController.manageView(auth, model);

        assertThat(view).isEqualTo("dashboard/manage");
        assertThat(model.get("tabs")).isEqualTo(List.of(tab));
        assertThat(model.get("categories")).isEqualTo(List.of(category));
        assertThat(model.get("links")).isEqualTo(List.of(link));
        assertThat(model.get("isAdmin")).isEqualTo(true);
        assertThat(model.get("npmLinks")).isEqualTo(List.of("a.example.com"));
    }

    @Test
    void manageView_shouldSkipNpmLinksForNonAdmin() {
        Authentication auth = userAuth("alice");
        ExtendedModelMap model = new ExtendedModelMap();
        when(portalService.getPortalTabs("alice")).thenReturn(new PortalTabsResponse(List.of()));

        String view = dashboardManageController.manageView(auth, model);

        assertThat(view).isEqualTo("dashboard/manage");
        assertThat(model.get("isAdmin")).isEqualTo(false);
        assertThat(model.containsAttribute("npmLinks")).isFalse();
        verifyNoInteractions(proxyGetterService);
    }

    @Test
    void createTab_shouldCreateAndUploadBackground() {
        org.springframework.security.core.userdetails.User user = principal("alice");
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        MockMultipartFile image = new MockMultipartFile("backgroundImage", "bg.png", "image/png", "bin".getBytes());
        when(portalService.createTab("alice", new TabCreateRequest("Main", 1, null)))
                .thenReturn(new TabResponse(10L, "Main", 1, null, List.of()));

        String view = dashboardManageController.createTab("Main", 1, image, user, redirect);

        assertThat(view).isEqualTo("redirect:/dashboard/manage");
        verify(portalService).createTab("alice", new TabCreateRequest("Main", 1, null));
        verify(portalService).uploadTabBackground("alice", 10L, image);
    }

    @Test
    void createTab_shouldCaptureBusinessExceptionMessage() {
        org.springframework.security.core.userdetails.User user = principal("alice");
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        when(portalService.createTab("alice", new TabCreateRequest("Main", null, null)))
                .thenThrow(new BadRequestException("bad tab"));

        String view = dashboardManageController.createTab("Main", null, null, user, redirect);

        assertThat(view).isEqualTo("redirect:/dashboard/manage");
        assertThat(redirect.getFlashAttributes().get("toastMessage")).isEqualTo("bad tab");
    }

    @Test
    void removeAndUploadAndRenameBackground_shouldDelegate() {
        org.springframework.security.core.userdetails.User user = principal("alice");
        MockMultipartFile image = new MockMultipartFile("backgroundImage", "bg.png", "image/png", "bin".getBytes());

        assertThat(dashboardManageController.removeTabBackground(1L, user)).isEqualTo("redirect:/dashboard/manage");
        assertThat(dashboardManageController.uploadTabBackground(1L, image, user)).isEqualTo("redirect:/dashboard/manage");
        assertThat(dashboardManageController.renameTab(1L, "New", user)).isEqualTo("redirect:/dashboard/manage");

        verify(portalService).clearTabBackground("alice", 1L);
        verify(portalService).uploadTabBackground("alice", 1L, image);
        verify(portalService).renameTab("alice", 1L, "New");
    }

    @Test
    void deleteTab_shouldAddSuccessToast() {
        org.springframework.security.core.userdetails.User user = principal("alice");
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        String view = dashboardManageController.deleteTab(1L, user, redirect);

        assertThat(view).isEqualTo("redirect:/dashboard/manage");
        assertThat(redirect.getFlashAttributes().get("toastMessage")).isEqualTo("탭을 삭제했습니다.");
        verify(portalService).deleteTab("alice", 1L);
    }

    @Test
    void deleteTab_shouldAddErrorToastOnBusinessException() {
        org.springframework.security.core.userdetails.User user = principal("alice");
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        doThrow(new BadRequestException("cannot delete")).when(portalService).deleteTab("alice", 1L);

        String view = dashboardManageController.deleteTab(1L, user, redirect);

        assertThat(view).isEqualTo("redirect:/dashboard/manage");
        assertThat(redirect.getFlashAttributes().get("toastMessage")).isEqualTo("cannot delete");
    }

    @Test
    void createCategory_shouldCreateAndAttachCategory() {
        org.springframework.security.core.userdetails.User user = principal("alice");
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        when(portalService.createCategory("alice", 10L, new CategoryCreateRequest("News", null, 2)))
                .thenReturn(new CategoryResponse(99L, "News", 2, List.of()));

        String view = dashboardManageController.createCategory("News", 10L, 2, user, redirect);

        assertThat(view).isEqualTo("redirect:/dashboard/manage");
        verify(portalService).addCategorytoTab("alice", 10L, 99L);
    }

    @Test
    void createCategory_shouldHandleBusinessException() {
        org.springframework.security.core.userdetails.User user = principal("alice");
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        when(portalService.createCategory("alice", 10L, new CategoryCreateRequest("News", null, 2)))
                .thenThrow(new BadRequestException("bad category"));

        String view = dashboardManageController.createCategory("News", 10L, 2, user, redirect);

        assertThat(view).isEqualTo("redirect:/dashboard/manage");
        assertThat(redirect.getFlashAttributes().get("toastMessage")).isEqualTo("bad category");
    }

    @Test
    void deleteCategory_shouldAddSuccessToast() {
        org.springframework.security.core.userdetails.User user = principal("alice");
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        String view = dashboardManageController.deleteCategory(9L, user, redirect);

        assertThat(view).isEqualTo("redirect:/dashboard/manage");
        verify(portalService).deleteCategory("alice", 9L);
        assertThat(redirect.getFlashAttributes().get("toastMessage")).isEqualTo("카테고리를 삭제했습니다.");
    }

    @Test
    void deleteCategory_shouldAddErrorToastOnBusinessException() {
        org.springframework.security.core.userdetails.User user = principal("alice");
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        doThrow(new BadRequestException("cannot delete category")).when(portalService).deleteCategory("alice", 9L);

        String view = dashboardManageController.deleteCategory(9L, user, redirect);

        assertThat(view).isEqualTo("redirect:/dashboard/manage");
        assertThat(redirect.getFlashAttributes().get("toastMessage")).isEqualTo("cannot delete category");
    }

    @Test
    void createAndDeleteLink_shouldDelegateAndHandleErrors() {
        org.springframework.security.core.userdetails.User user = principal("alice");
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        doThrow(new BadRequestException("cannot delete link")).when(portalService).deleteLink("alice", 5L);

        String createView = dashboardManageController.createLink("Google", "https://google.com", 1L, null, "i", "#fff", user, redirect);
        String deleteView = dashboardManageController.deleteLink(5L, user, redirect);

        assertThat(createView).isEqualTo("redirect:/dashboard/manage");
        verify(portalService).createLink("alice", 1L, "Google", "https://google.com", "i", "#fff", null);
        assertThat(deleteView).isEqualTo("redirect:/dashboard/manage");
        assertThat(redirect.getFlashAttributes().get("toastMessage")).isEqualTo("cannot delete link");
    }

    @Test
    void createLink_shouldHandleBusinessException() {
        org.springframework.security.core.userdetails.User user = principal("alice");
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        when(portalService.createLink("alice", 1L, "Google", "https://google.com", "i", "#fff", null))
                .thenThrow(new BadRequestException("bad link"));

        String view = dashboardManageController.createLink("Google", "https://google.com", 1L, null, "i", "#fff", user, redirect);

        assertThat(view).isEqualTo("redirect:/dashboard/manage");
        assertThat(redirect.getFlashAttributes().get("toastMessage")).isEqualTo("bad link");
    }

    @Test
    void deleteLink_shouldAddSuccessToast() {
        org.springframework.security.core.userdetails.User user = principal("alice");
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        String view = dashboardManageController.deleteLink(5L, user, redirect);

        assertThat(view).isEqualTo("redirect:/dashboard/manage");
        verify(portalService).deleteLink("alice", 5L);
        assertThat(redirect.getFlashAttributes().get("toastMessage")).isEqualTo("링크를 삭제했습니다.");
    }

    @Test
    void updateSort_shouldHandleBusinessException() {
        org.springframework.security.core.userdetails.User user = principal("alice");
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        doThrow(new BadRequestException("bad sort")).when(portalService).updateTabSortOrder("alice", 1L, 2);

        String view = dashboardManageController.updateTabSort(1L, 2, user, redirect);

        assertThat(view).isEqualTo("redirect:/dashboard/manage");
        assertThat(redirect.getFlashAttributes().get("toastMessage")).isEqualTo("bad sort");
    }

    @Test
    void updateTabSort_shouldDelegateOnSuccess() {
        org.springframework.security.core.userdetails.User user = principal("alice");
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        String view = dashboardManageController.updateTabSort(1L, 2, user, redirect);

        assertThat(view).isEqualTo("redirect:/dashboard/manage");
        verify(portalService).updateTabSortOrder("alice", 1L, 2);
    }

    @Test
    void updateCategorySort_shouldDelegateAndHandleBusinessException() {
        org.springframework.security.core.userdetails.User user = principal("alice");
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        doThrow(new BadRequestException("bad category sort"))
                .when(portalService).updateCategorySortOrder("alice", 2L, 3);

        String errorView = dashboardManageController.updateCategorySort(2L, 3, user, redirect);
        String successView = dashboardManageController.updateCategorySort(2L, 1, user, new RedirectAttributesModelMap());

        assertThat(errorView).isEqualTo("redirect:/dashboard/manage");
        assertThat(redirect.getFlashAttributes().get("toastMessage")).isEqualTo("bad category sort");
        assertThat(successView).isEqualTo("redirect:/dashboard/manage");
        verify(portalService).updateCategorySortOrder("alice", 2L, 1);
    }

    @Test
    void updateLinkSort_shouldDelegateAndHandleBusinessException() {
        org.springframework.security.core.userdetails.User user = principal("alice");
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        doThrow(new BadRequestException("bad link sort"))
                .when(portalService).updateLinkSortOrder("alice", 4L, 5);

        String errorView = dashboardManageController.updateLinkSort(4L, 5, user, redirect);
        String successView = dashboardManageController.updateLinkSort(4L, 1, user, new RedirectAttributesModelMap());

        assertThat(errorView).isEqualTo("redirect:/dashboard/manage");
        assertThat(redirect.getFlashAttributes().get("toastMessage")).isEqualTo("bad link sort");
        assertThat(successView).isEqualTo("redirect:/dashboard/manage");
        verify(portalService).updateLinkSortOrder("alice", 4L, 1);
    }

    @Test
    void reorderEndpoints_shouldReturnNoContentWhenSuccessful() {
        org.springframework.security.core.userdetails.User user = principal("alice");

        ResponseEntity<Void> tabsResponse = dashboardManageController.reorderTabs("2,1", user);
        ResponseEntity<Void> categoriesResponse = dashboardManageController.reorderCategories(10L, "2,1", user);
        ResponseEntity<Void> linksResponse = dashboardManageController.reorderLinks(20L, "2,1", user);

        assertThat(tabsResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(categoriesResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(linksResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(portalService).reorderTabsByIds("alice", List.of(2L, 1L));
        verify(portalService).reorderCategoriesByIds("alice", 10L, List.of(2L, 1L));
        verify(portalService).reorderLinksByIds("alice", 20L, List.of(2L, 1L));
    }

    @Test
    void reorderTabs_shouldReturnBadRequestWhenIdsInvalid() {
        org.springframework.security.core.userdetails.User user = principal("alice");

        ResponseEntity<Void> response = dashboardManageController.reorderTabs("a,b", user);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void reorderTabs_shouldReturnBadRequestWhenIdsBlankOrNull() {
        org.springframework.security.core.userdetails.User user = principal("alice");

        ResponseEntity<Void> blankResponse = dashboardManageController.reorderTabs("   ", user);
        ResponseEntity<Void> nullResponse = dashboardManageController.reorderTabs(null, user);

        assertThat(blankResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(nullResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void reorderCategoriesAndLinks_shouldReturnBadRequestWhenServiceThrows() {
        org.springframework.security.core.userdetails.User user = principal("alice");
        doThrow(new BadRequestException("bad categories"))
                .when(portalService).reorderCategoriesByIds("alice", 10L, List.of(2L, 1L));
        doThrow(new BadRequestException("bad links"))
                .when(portalService).reorderLinksByIds("alice", 20L, List.of(2L, 1L));

        ResponseEntity<Void> categoriesResponse = dashboardManageController.reorderCategories(10L, "2,1", user);
        ResponseEntity<Void> linksResponse = dashboardManageController.reorderLinks(20L, "2,1", user);

        assertThat(categoriesResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(linksResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private static Authentication adminAuth(String username) {
        return new TestingAuthenticationToken(username, "pw", "ROLE_ADMIN");
    }

    private static Authentication userAuth(String username) {
        return new TestingAuthenticationToken(username, "pw", "ROLE_USER");
    }

    private static org.springframework.security.core.userdetails.User principal(String username) {
        return new org.springframework.security.core.userdetails.User(username, "pw", List.of(() -> "ROLE_USER"));
    }
}
