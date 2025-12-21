package com.messier333.proxyportal.portal.repository.impl;

import org.springframework.stereotype.Repository;

import com.messier333.proxyportal.portal.dto.PortalConfigResponse;
import com.messier333.proxyportal.portal.repository.PortalQueryRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PortalQueryRepositoryImpl implements PortalQueryRepository {

    @Override
    public PortalConfigResponse findPortalConfig(Long userId) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
