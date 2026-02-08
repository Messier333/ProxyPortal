package com.messier333.proxyportal.portal.repository;

import com.messier333.proxyportal.portal.dto.response.PortalTabsResponse;
import com.messier333.proxyportal.portal.dto.response.PortalCategoriesResponse;

public interface PortalQueryRepository {
    PortalTabsResponse findTabsByUsername(String username);
    PortalCategoriesResponse findCategoriesByUsername(String username);
}
