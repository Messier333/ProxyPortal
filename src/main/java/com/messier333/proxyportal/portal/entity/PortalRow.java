package com.messier333.proxyportal.portal.entity;

public record PortalRow(
        Long tabId, String tabName, String tabBg, Integer tabSort,
        Long categoryId, String categoryName, Integer categorySort,
        Long linkId, String linkName, String linkUrl, String linkIcon, String linkIconColor, Integer linkSort
) {}