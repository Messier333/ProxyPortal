package com.messier333.proxyportal.portal.dto.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TabResponse(
        Long id,
        String name,
        Integer sortOrder,
        @JsonProperty("background_url")
        String backgroundUrl,
        List<CategoryResponse> categories
) {}
