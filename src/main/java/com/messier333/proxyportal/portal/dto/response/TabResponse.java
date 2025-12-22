package com.messier333.proxyportal.portal.dto.response;

import java.util.List;

public record TabResponse(
        Long id,
        String name,
        String backgroundUrl,
        Integer sortOrder,
        List<CategoryResponse> categories
) {}