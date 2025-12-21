package com.messier333.proxyportal.portal.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PortalConfigResponse(
        List<TabDto> tabs
) {
    public record TabDto(
            String name,
            String backgroundUrl,
            Integer sortOrder,
            List<CategoryDto> categories
    ) {}

    public record CategoryDto(
            String name,
            List<LinkDto> links,
            Integer sortOrder
    ) {}

    public record LinkDto(
            String name,
            String url,
            String icon,
            String iconColor,
            Integer sortOrder
    ) {}
}
