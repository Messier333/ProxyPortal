package com.messier333.proxyportal.portal.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.messier333.proxyportal.portal.dto.response.PortalTabsResponse;
import com.messier333.proxyportal.portal.dto.response.TabResponse;
import com.messier333.proxyportal.portal.service.PortalService;

@ExtendWith(MockitoExtension.class)
class PortalRestControllerTest {

    @Mock
    private PortalService portalService;

    @InjectMocks
    private PortalRestController portalRestController;

    @Test
    void getPortalTabs_shouldReturnUserTabs() {
        Principal principal = () -> "alice";
        TabResponse tab = new TabResponse(1L, "Main", 1, null, List.of());
        when(portalService.getPortalTabs("alice")).thenReturn(new PortalTabsResponse(List.of(tab)));

        List<TabResponse> result = portalRestController.getPortalTabs(principal);

        assertThat(result).containsExactly(tab);
    }

    @Test
    void postPortalTab_shouldThrowNotImplemented() {
        assertNotImplemented(portalRestController::postPortalTab);
    }

    @Test
    void postPortalCategory_shouldThrowNotImplemented() {
        assertNotImplemented(portalRestController::postPortalCategory);
    }

    @Test
    void postPortalLink_shouldThrowNotImplemented() {
        assertNotImplemented(portalRestController::postPortalLink);
    }

    private static void assertNotImplemented(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException exception = (ResponseStatusException) ex;
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_IMPLEMENTED);
                });
    }
}
