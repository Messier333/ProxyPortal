package com.messier333.proxyportal.portal.service;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.messier333.proxyportal.portal.dto.request.CategoryCreateRequest;
import com.messier333.proxyportal.portal.dto.request.LinkCreateRequest;
import com.messier333.proxyportal.portal.dto.request.TabCreateRequest;
import com.messier333.proxyportal.portal.dto.response.CategoryResponse;
import com.messier333.proxyportal.portal.dto.response.LinkResponse;
import com.messier333.proxyportal.portal.dto.response.PortalTabsResponse;
import com.messier333.proxyportal.portal.dto.response.TabResponse;
import com.messier333.proxyportal.portal.entity.PortalCategory;
import com.messier333.proxyportal.portal.entity.PortalLink;
import com.messier333.proxyportal.portal.entity.PortalTab;
import com.messier333.proxyportal.portal.repository.PortalCategoryRepository;
import com.messier333.proxyportal.portal.repository.PortalLinkRepository;
import com.messier333.proxyportal.portal.repository.PortalQueryRepository;
import com.messier333.proxyportal.portal.repository.PortalTabRepository;
import com.messier333.proxyportal.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PortalService {
    private final PortalCategoryRepository portalCategoryRepository;
    private final PortalLinkRepository portalLinkRepository;
    private final PortalTabRepository portalTabRepository;
    private final UserRepository userRepository;
    private final PortalQueryRepository portalQueryRepository;

    public PortalTabsResponse getPortalTabs(String username) {
        return portalQueryRepository.findTabsByUsername(username);
    }

    @Transactional
    public TabResponse createTab(String username, TabCreateRequest tabCreateRequest){
        PortalTab portalTab = PortalTab.createTab(
                userRepository.findByUsername(username).orElseThrow(
                        () -> new IllegalArgumentException("User not found")
                ),
                tabCreateRequest.name(),
                tabCreateRequest.backgroundUrl(),
                tabCreateRequest.sortOrder()
        );
        Objects.requireNonNull(portalTab, "portalTab must not be null");
        PortalTab saved = portalTabRepository.save(portalTab);
        return new TabResponse(
                saved.getId(),
                saved.getName(),
                saved.getBackgroundUrl(),
                saved.getSortOrder(),
                List.of()
        );
    }

    @Transactional
    public CategoryResponse createCategory (String username, Long tabId, CategoryCreateRequest categoryCreateRequest) {
        PortalTab portalTab = portalTabRepository.findByIdAndUserUsername(tabId, username).orElseThrow(
                () -> new IllegalArgumentException("tab not found or user not matched")
        );
        PortalCategory portalCategory = PortalCategory.create(
                portalTab,
                categoryCreateRequest.name(),
                categoryCreateRequest.sortOrder()
        );

        Objects.requireNonNull(portalCategory, "portalCategory must not be null");
        PortalCategory saved = portalCategoryRepository.save(portalCategory);
        return new CategoryResponse(
                saved.getId(),
                saved.getName(),
                List.of(),
                saved.getSortOrder()
        );
    }

    @Transactional
    public LinkResponse createLink (String username, Long categoryId, LinkCreateRequest linkCreateRequest) {
        PortalCategory portalCategory = portalCategoryRepository.findByIdAndTabUserUsername(categoryId, username).orElseThrow(
                () -> new IllegalArgumentException("category not found or user not matched")
        );
        PortalLink portalLink = PortalLink.create(
                portalCategory,
                linkCreateRequest.name(),
                linkCreateRequest.url(),
                linkCreateRequest.icon(),
                linkCreateRequest.iconColor(),
                linkCreateRequest.sortOrder()
        );
        Objects.requireNonNull(portalLink, "portalLink must not be null");
        PortalLink saved = portalLinkRepository.save(portalLink);
        return new LinkResponse(
                saved.getId(),
                saved.getName(),
                saved.getUrl(),
                saved.getIcon(),
                saved.getIconColor(),
                saved.getSortOrder()
        );
    }
}
