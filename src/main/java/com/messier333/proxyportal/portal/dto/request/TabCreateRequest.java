package com.messier333.proxyportal.portal.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record TabCreateRequest(
        @NotBlank String name,
        @NotBlank String backgroundUrl,
        @NotNull Integer sortOrder,
        List<CategoryCreateRequest> categories
) {
}
