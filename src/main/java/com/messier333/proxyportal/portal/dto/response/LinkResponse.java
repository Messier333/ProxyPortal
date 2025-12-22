package com.messier333.proxyportal.portal.dto.response;

public record LinkResponse(
        Long id,
        String name,
        String url,
        String icon,
        String iconColor,
        Integer sortOrder
) {}