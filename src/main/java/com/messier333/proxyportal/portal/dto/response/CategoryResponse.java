package com.messier333.proxyportal.portal.dto.response;

import java.util.List;

public record CategoryResponse(
        Long id,
        String name,
        Integer sortOrder,
        List<LinkResponse> links
) {}