package com.messier333.proxyportal.portal.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LinkCreateRequest(
        @NotBlank String name,
        @NotBlank String url,
        @NotBlank String icon,
        @NotBlank String iconColor,
        @NotNull Integer sortOrder
) {
}
