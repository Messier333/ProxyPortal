package com.messier333.proxyportal.portal.repository;

import com.messier333.proxyportal.portal.dto.response.PortalTabsResponse;

public interface PortalQueryRepository {
    PortalTabsResponse findTabsByUsername(String username);
}
