package com.messier333.proxyportal.portal.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotBlank;

public record TabCreateRequest(
        @NotBlank String name,
        Integer sortOrder,
        List<CategoryCreateRequest> categories
) {
}
