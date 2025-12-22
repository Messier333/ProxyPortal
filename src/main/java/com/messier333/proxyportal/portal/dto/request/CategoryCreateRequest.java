package com.messier333.proxyportal.portal.dto.request;



import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CategoryCreateRequest(
        @NotBlank String name,
        @NotBlank List<LinkCreateRequest> links,
        @NotNull Integer sortOrder
) {
}
