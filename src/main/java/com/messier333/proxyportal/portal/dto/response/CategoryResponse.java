package com.messier333.proxyportal.portal.dto.response;

import java.util.List;

public record CategoryResponse(
        Long id,
        String name,
        List<LinkResponse> links,
        Integer sortOrder
) {}