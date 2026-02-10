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
import org.springframework.ui.ExtendedModelMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.messier333.proxyportal.portal.dto.response.PortalTabsResponse;
import com.messier333.proxyportal.portal.dto.response.TabResponse;
import com.messier333.proxyportal.portal.service.PortalService;

@ExtendWith(MockitoExtension.class)
class PortalControllerTest {

    @Mock
    private PortalService portalService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private PortalController portalController;

    @Test
    void home_shouldRedirectToPortal() {
        assertThat(portalController.home()).isEqualTo("redirect:/portal");
    }

    @Test
    void showPortal_shouldAddSerializedPortalConfigToModel() throws Exception {
        ExtendedModelMap model = new ExtendedModelMap();
        Principal principal = () -> "alice";
        PortalTabsResponse tabs = new PortalTabsResponse(List.of(new TabResponse(1L, "Main", 1, null, List.of())));
        when(portalService.getPortalTabs("alice")).thenReturn(tabs);
        when(objectMapper.writeValueAsString(tabs)).thenReturn("{\"tabs\":[]}");

        String view = portalController.showPortal(model, principal);

        assertThat(view).isEqualTo("portal/index");
        assertThat(model.get("portalConfigJson")).isEqualTo("{\"tabs\":[]}");
    }

    @Test
    void showPortal_shouldPropagateJsonError() throws Exception {
        ExtendedModelMap model = new ExtendedModelMap();
        Principal principal = () -> "alice";
        PortalTabsResponse tabs = new PortalTabsResponse(List.of());
        when(portalService.getPortalTabs("alice")).thenReturn(tabs);
        when(objectMapper.writeValueAsString(tabs)).thenThrow(new JsonProcessingException("boom") {
            private static final long serialVersionUID = 1L;
        });

        assertThatThrownBy(() -> portalController.showPortal(model, principal))
                .isInstanceOf(JsonProcessingException.class);
    }
}
