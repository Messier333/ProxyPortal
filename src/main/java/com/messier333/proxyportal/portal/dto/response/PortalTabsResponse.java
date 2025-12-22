package com.messier333.proxyportal.portal.dto.response;

import java.util.List;

public record PortalTabsResponse(
        List<TabResponse> tabs
) {
}
