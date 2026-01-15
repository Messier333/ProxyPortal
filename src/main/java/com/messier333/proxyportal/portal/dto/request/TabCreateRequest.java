package com.messier333.proxyportal.portal.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TabCreateRequest(
        @NotBlank String name,
        @NotNull Integer sortOrder,
        List<CategoryCreateRequest> categories
) {
}
