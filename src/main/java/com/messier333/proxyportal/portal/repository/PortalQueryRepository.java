package com.messier333.proxyportal.portal.repository;

import com.messier333.proxyportal.portal.dto.PortalConfigResponse;

public interface PortalQueryRepository {
    PortalConfigResponse findPortalConfig(Long userId);
}
