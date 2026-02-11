package com.messier333.proxyportal.portal.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LinkCreateRequest(
        @NotBlank String name,
        @NotBlank String url,
        @NotBlank String icon,
        @NotBlank String iconColor,
        Integer sortOrder
) {
}
