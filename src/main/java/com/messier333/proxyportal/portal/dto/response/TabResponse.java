package com.messier333.proxyportal.portal.dto.response;

import java.util.List;

public record TabResponse(
        Long id,
        String name,
        Integer sortOrder,
        List<CategoryResponse> categories
) {}