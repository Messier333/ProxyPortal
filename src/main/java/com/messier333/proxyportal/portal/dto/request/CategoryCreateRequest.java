package com.messier333.proxyportal.portal.dto.request;



import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record CategoryCreateRequest(
        @NotBlank String name,
        List<LinkCreateRequest> links,
        Integer sortOrder
) {
}
