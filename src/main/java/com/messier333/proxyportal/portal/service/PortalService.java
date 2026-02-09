package com.messier333.proxyportal.portal.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Objects;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
    private static final String TAB_UPLOAD_PATH = "/uploads/portal-tabs/";
    private static final String DEFAULT_LINK_ICON = "link";
    private static final String DEFAULT_LINK_ICON_COLOR = "#89b4fa";

    private final PortalCategoryRepository portalCategoryRepository;
    private final PortalLinkRepository portalLinkRepository;
    private final PortalTabRepository portalTabRepository;
    private final UserRepository userRepository;
    private final PortalQueryRepository portalQueryRepository;
    @Value("${app.upload.root:uploads}")
    private String uploadRoot;

    public PortalTabsResponse getPortalTabs(String username) {
        return portalQueryRepository.findTabsByUsername(username);
    }

    public void getCategories(){
    }


    @Transactional
    public TabResponse createTab(String username, TabCreateRequest tabCreateRequest){
        String tabName = normalizeName(tabCreateRequest.name(), "tab name");
        if (portalTabRepository.existsByUserUsernameAndNameIgnoreCase(username, tabName)) {
            throw new IllegalArgumentException("이미 같은 이름의 탭이 있습니다.");
        }
        int nextSortOrder = nextTabSortOrder(username);
        PortalTab portalTab = PortalTab.createTab(
                userRepository.findByUsername(username).orElseThrow(
                        () -> new IllegalArgumentException("User not found")
                ),
                tabName,
                nextSortOrder
        );
        Objects.requireNonNull(portalTab, "portalTab must not be null");
        PortalTab saved = portalTabRepository.save(portalTab);
        Integer requestedSort = tabCreateRequest.sortOrder();
        if (requestedSort != null && requestedSort > 0 && requestedSort != nextSortOrder) {
            updateTabSortOrder(username, saved.getId(), requestedSort);
        }
        return new TabResponse(
                saved.getId(),
                saved.getName(),
                saved.getSortOrder(),
                saved.getBackgroundUrl(),
                List.of()
        );
    }

    @Transactional
    public void uploadTabBackground(String username, Long tabId, MultipartFile backgroundImage) {
        if (backgroundImage == null || backgroundImage.isEmpty()) return;

        PortalTab tab = portalTabRepository.findByIdAndUserUsername(tabId, username)
                .orElseThrow(() -> new IllegalArgumentException("tab not found or user not matched"));

        String contentType = backgroundImage.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Only image files are allowed");
        }

        String safeUsername = username.replaceAll("[^a-zA-Z0-9_-]", "_");
        String safeTabName = sanitizeFolderName(tab.getName(), "tab-" + tab.getId());
        Path uploadDir = resolveUploadRoot()
                .resolve("portal-tabs")
                .resolve(safeUsername)
                .resolve(safeTabName)
                .normalize();
        String filename = sanitizeOriginalFilename(backgroundImage.getOriginalFilename());
        Path target = uploadDir.resolve(filename).normalize();

        if (!target.startsWith(uploadDir)) {
            throw new IllegalArgumentException("Invalid file path");
        }

        try {
            Files.createDirectories(uploadDir);
            Files.copy(backgroundImage.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store uploaded image", e);
        }

        deleteBackgroundIfManaged(tab.getBackgroundUrl());
        tab.setBackgroundUrl(TAB_UPLOAD_PATH + safeUsername + "/" + safeTabName + "/" + filename);
        portalTabRepository.save(tab);
    }

    @Transactional
    public void clearTabBackground(String username, Long tabId) {
        PortalTab tab = portalTabRepository.findByIdAndUserUsername(tabId, username)
                .orElseThrow(() -> new IllegalArgumentException("tab not found or user not matched"));

        deleteBackgroundIfManaged(tab.getBackgroundUrl());
        tab.setBackgroundUrl(null);
        portalTabRepository.save(tab);
    }

    @Transactional
    public void renameTab(String username, Long tabId, String newName) {
        if (newName == null || newName.isBlank()) {
            throw new IllegalArgumentException("tab name must not be blank");
        }

        PortalTab tab = portalTabRepository.findByIdAndUserUsername(tabId, username)
                .orElseThrow(() -> new IllegalArgumentException("tab not found or user not matched"));

        String oldName = tab.getName();
        String trimmedNewName = newName.trim();
        if (oldName.equals(trimmedNewName)) return;

        moveTabFolderIfNeeded(username, oldName, trimmedNewName, tab);
        tab.update(trimmedNewName, tab.getSortOrder());
        portalTabRepository.save(tab);
    }

    private Path resolveUploadRoot() {
        return Paths.get(uploadRoot).toAbsolutePath().normalize();
    }

    private void deleteBackgroundIfManaged(String backgroundUrl) {
        if (backgroundUrl == null || !backgroundUrl.startsWith(TAB_UPLOAD_PATH)) return;

        String relativePath = backgroundUrl.substring("/uploads/".length());
        Path target = resolveUploadRoot().resolve(relativePath).normalize();
        if (!target.startsWith(resolveUploadRoot())) return;

        try {
            Files.deleteIfExists(target);
        } catch (IOException ignored) {
        }
    }

    private String sanitizeOriginalFilename(String originalFilename) {
        String fallback = "uploaded-image";
        if (originalFilename == null || originalFilename.isBlank()) return fallback;

        String name = Paths.get(originalFilename).getFileName().toString().trim();
        if (name.isBlank()) return fallback;

        // Keep the original filename as much as possible, only strip path-unsafe characters.
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private String sanitizeFolderName(String name, String fallback) {
        if (name == null || name.isBlank()) return fallback;
        String safe = name.trim().replaceAll("[\\\\/:*?\"<>|]", "_");
        return safe.isBlank() ? fallback : safe;
    }

    private void moveTabFolderIfNeeded(String username, String oldTabName, String newTabName, PortalTab tab) {
        String backgroundUrl = tab.getBackgroundUrl();
        if (backgroundUrl == null || !backgroundUrl.startsWith(TAB_UPLOAD_PATH)) return;

        String safeUsername = username.replaceAll("[^a-zA-Z0-9_-]", "_");
        String oldFolder = sanitizeFolderName(oldTabName, "tab-" + tab.getId());
        String newFolder = sanitizeFolderName(newTabName, "tab-" + tab.getId());
        if (oldFolder.equals(newFolder)) return;

        Path userBase = resolveUploadRoot().resolve("portal-tabs").resolve(safeUsername).normalize();
        Path oldDir = userBase.resolve(oldFolder).normalize();
        Path newDir = userBase.resolve(newFolder).normalize();

        if (!oldDir.startsWith(userBase) || !newDir.startsWith(userBase)) return;
        if (!Files.exists(oldDir)) return;

        try {
            Files.createDirectories(userBase);

            if (Files.exists(newDir)) {
                copyDirectoryContents(oldDir, newDir);
                deleteDirectoryRecursively(oldDir);
            } else {
                Files.move(oldDir, newDir, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to move tab background folder during rename", e);
        }

        String oldPrefix = TAB_UPLOAD_PATH + safeUsername + "/" + oldFolder + "/";
        String newPrefix = TAB_UPLOAD_PATH + safeUsername + "/" + newFolder + "/";
        if (backgroundUrl.startsWith(oldPrefix)) {
            tab.setBackgroundUrl(newPrefix + backgroundUrl.substring(oldPrefix.length()));
        }
    }

    private void copyDirectoryContents(Path from, Path to) throws IOException {
        try (Stream<Path> stream = Files.walk(from)) {
            for (Path source : stream.toList()) {
                Path relative = from.relativize(source);
                Path target = to.resolve(relative).normalize();
                if (Files.isDirectory(source)) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private void deleteDirectoryRecursively(Path root) throws IOException {
        if (!Files.exists(root)) return;
        try (Stream<Path> stream = Files.walk(root)) {
            for (Path path : stream.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    @Transactional
    public CategoryResponse createCategory (String username, Long tabId, CategoryCreateRequest categoryCreateRequest) {
        PortalTab portalTab = portalTabRepository.findByIdAndUserUsername(tabId, username).orElseThrow(
                () -> new IllegalArgumentException("tab not found or user not matched")
        );
        String categoryName = normalizeName(categoryCreateRequest.name(), "category name");
        if (portalCategoryRepository.existsByTabIdAndNameIgnoreCase(tabId, categoryName)) {
            throw new IllegalArgumentException("이미 같은 이름의 카테고리가 있습니다.");
        }
        int nextSortOrder = nextCategorySortOrder(tabId);
        PortalCategory portalCategory = PortalCategory.create(
                portalTab,
                categoryName,
                nextSortOrder
        );

        Objects.requireNonNull(portalCategory, "portalCategory must not be null");
        PortalCategory saved = portalCategoryRepository.save(portalCategory);
        Integer requestedSort = categoryCreateRequest.sortOrder();
        if (requestedSort != null && requestedSort > 0 && requestedSort != nextSortOrder) {
            updateCategorySortOrder(username, saved.getId(), requestedSort);
        }
        return new CategoryResponse(
                saved.getId(),
                saved.getName(),
                saved.getSortOrder(),
                List.of()
        );
    }

    @Transactional
    public LinkResponse createLink (String username, Long categoryId, LinkCreateRequest linkCreateRequest) {
        PortalCategory portalCategory = portalCategoryRepository.findByIdAndTabUserUsername(categoryId, username).orElseThrow(
                () -> new IllegalArgumentException("category not found or user not matched")
        );
        String linkName = normalizeName(linkCreateRequest.name(), "link name");
        String linkUrl = normalizeUrl(linkCreateRequest.url());
        if (portalLinkRepository.existsByCategoryIdAndNameIgnoreCase(categoryId, linkName)) {
            throw new IllegalArgumentException("이미 같은 이름의 링크가 있습니다.");
        }
        if (portalLinkRepository.existsByCategoryIdAndUrlIgnoreCase(categoryId, linkUrl)) {
            throw new IllegalArgumentException("이미 같은 URL 링크가 있습니다.");
        }
        int nextSortOrder = nextLinkSortOrder(categoryId);
        PortalLink portalLink = PortalLink.create(
                portalCategory,
                linkName,
                linkUrl,
                linkCreateRequest.icon(),
                linkCreateRequest.iconColor(),
                nextSortOrder
        );
        Objects.requireNonNull(portalLink, "portalLink must not be null");
        PortalLink saved = portalLinkRepository.save(portalLink);
        Integer requestedSort = linkCreateRequest.sortOrder();
        if (requestedSort != null && requestedSort > 0 && requestedSort != nextSortOrder) {
            updateLinkSortOrder(username, saved.getId(), requestedSort);
        }
        return new LinkResponse(
                saved.getId(),
                saved.getName(),
                saved.getUrl(),
                saved.getIcon(),
                saved.getIconColor(),
                saved.getSortOrder()
        );
    }

    @Transactional
    public LinkResponse createLink(String username, Long categoryId, String name, String url) {
        return createLink(username, categoryId, name, url, null, null, null);
    }

    @Transactional
    public LinkResponse createLink(String username, Long categoryId, String name, String url, String icon, String iconColor) {
        return createLink(username, categoryId, name, url, icon, iconColor, null);
    }

    @Transactional
    public LinkResponse createLink(String username, Long categoryId, String name, String url, String icon, String iconColor, Integer sortOrder) {
        String normalizedUrl = normalizeUrl(url);
        LinkCreateRequest request = new LinkCreateRequest(
                name,
                normalizedUrl,
                normalizeIcon(icon, DEFAULT_LINK_ICON),
                normalizeIconColor(iconColor, DEFAULT_LINK_ICON_COLOR),
                sortOrder
        );
        return createLink(username, categoryId, request);
    }

    @Transactional
    public CategoryResponse addCategorytoTab(String username, Long tabId, Long categoryId){
        PortalCategory portalCategory = portalCategoryRepository.findByIdAndTabUserUsername(categoryId,username).orElseThrow(
                () -> new IllegalArgumentException("category not found or user not matched")
        );
        portalCategory.setCategoryTab(portalTabRepository.findById(tabId).orElseThrow(
                () -> new IllegalArgumentException("tab not found or user not matched")
        ));
        return new CategoryResponse(
                portalCategory.getId(),
                portalCategory.getName(),
                portalCategory.getSortOrder(),
                List.of()
        );
    }

    @Transactional
    public void deleteTab(String username, Long tabId) {
        PortalTab target = portalTabRepository.findByIdAndUserUsername(tabId, username)
                .orElseThrow(() -> new IllegalArgumentException("tab not found or user not matched"));

        List<PortalCategory> categories = portalCategoryRepository.findAllByTabIdOrderBySortOrderAscIdAsc(tabId);
        for (PortalCategory category : categories) {
            List<PortalLink> links = portalLinkRepository.findAllByCategoryIdOrderBySortOrderAscIdAsc(category.getId());
            if (!links.isEmpty()) {
                portalLinkRepository.deleteAll(links);
            }
        }
        if (!categories.isEmpty()) {
            portalCategoryRepository.deleteAll(categories);
        }

        deleteBackgroundIfManaged(target.getBackgroundUrl());
        portalTabRepository.delete(target);
        normalizeTabSortOrders(username);
    }

    @Transactional
    public void deleteCategory(String username, Long categoryId) {
        PortalCategory target = portalCategoryRepository.findByIdAndTabUserUsername(categoryId, username)
                .orElseThrow(() -> new IllegalArgumentException("category not found or user not matched"));
        Long tabId = target.getTab().getId();

        List<PortalLink> links = portalLinkRepository.findAllByCategoryIdOrderBySortOrderAscIdAsc(categoryId);
        if (!links.isEmpty()) {
            portalLinkRepository.deleteAll(links);
        }

        portalCategoryRepository.delete(target);
        normalizeCategorySortOrders(tabId);
    }

    @Transactional
    public void deleteLink(String username, Long linkId) {
        PortalLink target = portalLinkRepository.findByIdAndCategoryTabUserUsername(linkId, username)
                .orElseThrow(() -> new IllegalArgumentException("link not found or user not matched"));
        Long categoryId = target.getCategory().getId();

        portalLinkRepository.delete(target);
        normalizeLinkSortOrders(categoryId);
    }

    @Transactional
    public void updateTabSortOrder(String username, Long tabId, Integer sortOrder) {
        int requestedSort = normalizePositiveSort(sortOrder, "tab sortOrder");
        PortalTab target = portalTabRepository.findByIdAndUserUsername(tabId, username)
                .orElseThrow(() -> new IllegalArgumentException("tab not found or user not matched"));
        List<PortalTab> siblings = new ArrayList<>(portalTabRepository.findAllByUserUsernameOrderBySortOrderAscIdAsc(username));
        reorderTabs(siblings, target, requestedSort);
    }

    @Transactional
    public void updateCategorySortOrder(String username, Long categoryId, Integer sortOrder) {
        int requestedSort = normalizePositiveSort(sortOrder, "category sortOrder");
        PortalCategory target = portalCategoryRepository.findByIdAndTabUserUsername(categoryId, username)
                .orElseThrow(() -> new IllegalArgumentException("category not found or user not matched"));
        List<PortalCategory> siblings = new ArrayList<>(portalCategoryRepository.findAllByTabIdOrderBySortOrderAscIdAsc(target.getTab().getId()));
        reorderCategories(siblings, target, requestedSort);
    }

    @Transactional
    public void updateLinkSortOrder(String username, Long linkId, Integer sortOrder) {
        int requestedSort = normalizePositiveSort(sortOrder, "link sortOrder");
        PortalLink target = portalLinkRepository.findByIdAndCategoryTabUserUsername(linkId, username)
                .orElseThrow(() -> new IllegalArgumentException("link not found or user not matched"));
        List<PortalLink> siblings = new ArrayList<>(portalLinkRepository.findAllByCategoryIdOrderBySortOrderAscIdAsc(target.getCategory().getId()));
        reorderLinks(siblings, target, requestedSort);
    }

    @Transactional
    public void reorderTabsByIds(String username, List<Long> orderedTabIds) {
        List<PortalTab> siblings = new ArrayList<>(portalTabRepository.findAllByUserUsernameOrderBySortOrderAscIdAsc(username));
        validateReorderIds(siblings.stream().map(PortalTab::getId).toList(), orderedTabIds, "tabs");
        for (int i = 0; i < orderedTabIds.size(); i++) {
            Long targetId = orderedTabIds.get(i);
            PortalTab target = siblings.stream()
                    .filter(tab -> tab.getId().equals(targetId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("tab not found"));
            target.setSortOrder(i + 1);
        }
    }

    @Transactional
    public void reorderCategoriesByIds(String username, Long tabId, List<Long> orderedCategoryIds) {
        portalTabRepository.findByIdAndUserUsername(tabId, username)
                .orElseThrow(() -> new IllegalArgumentException("tab not found or user not matched"));
        List<PortalCategory> siblings = new ArrayList<>(portalCategoryRepository.findAllByTabIdOrderBySortOrderAscIdAsc(tabId));
        validateReorderIds(siblings.stream().map(PortalCategory::getId).toList(), orderedCategoryIds, "categories");
        for (int i = 0; i < orderedCategoryIds.size(); i++) {
            Long targetId = orderedCategoryIds.get(i);
            PortalCategory target = siblings.stream()
                    .filter(category -> category.getId().equals(targetId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("category not found"));
            target.setSortOrder(i + 1);
        }
    }

    @Transactional
    public void reorderLinksByIds(String username, Long categoryId, List<Long> orderedLinkIds) {
        portalCategoryRepository.findByIdAndTabUserUsername(categoryId, username)
                .orElseThrow(() -> new IllegalArgumentException("category not found or user not matched"));
        List<PortalLink> siblings = new ArrayList<>(portalLinkRepository.findAllByCategoryIdOrderBySortOrderAscIdAsc(categoryId));
        validateReorderIds(siblings.stream().map(PortalLink::getId).toList(), orderedLinkIds, "links");
        for (int i = 0; i < orderedLinkIds.size(); i++) {
            Long targetId = orderedLinkIds.get(i);
            PortalLink target = siblings.stream()
                    .filter(link -> link.getId().equals(targetId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("link not found"));
            target.setSortOrder(i + 1);
        }
    }

    private int nextTabSortOrder(String username) {
        return portalTabRepository.findAllByUserUsernameOrderBySortOrderAscIdAsc(username).size() + 1;
    }

    private int nextCategorySortOrder(Long tabId) {
        return portalCategoryRepository.findAllByTabIdOrderBySortOrderAscIdAsc(tabId).size() + 1;
    }

    private int nextLinkSortOrder(Long categoryId) {
        return portalLinkRepository.findAllByCategoryIdOrderBySortOrderAscIdAsc(categoryId).size() + 1;
    }

    private void normalizeTabSortOrders(String username) {
        List<PortalTab> tabs = portalTabRepository.findAllByUserUsernameOrderBySortOrderAscIdAsc(username);
        for (int i = 0; i < tabs.size(); i++) {
            int normalizedSort = i + 1;
            PortalTab tab = tabs.get(i);
            if (tab.getSortOrder() != normalizedSort) {
                tab.setSortOrder(normalizedSort);
            }
        }
    }

    private void normalizeCategorySortOrders(Long tabId) {
        List<PortalCategory> categories = portalCategoryRepository.findAllByTabIdOrderBySortOrderAscIdAsc(tabId);
        for (int i = 0; i < categories.size(); i++) {
            int normalizedSort = i + 1;
            PortalCategory category = categories.get(i);
            if (category.getSortOrder() != normalizedSort) {
                category.setSortOrder(normalizedSort);
            }
        }
    }

    private void normalizeLinkSortOrders(Long categoryId) {
        List<PortalLink> links = portalLinkRepository.findAllByCategoryIdOrderBySortOrderAscIdAsc(categoryId);
        for (int i = 0; i < links.size(); i++) {
            int normalizedSort = i + 1;
            PortalLink link = links.get(i);
            if (link.getSortOrder() != normalizedSort) {
                link.setSortOrder(normalizedSort);
            }
        }
    }

    private void validateReorderIds(List<Long> existingIds, List<Long> orderedIds, String targetName) {
        if (existingIds.isEmpty() && orderedIds.isEmpty()) {
            return;
        }
        if (orderedIds == null || orderedIds.isEmpty()) {
            throw new IllegalArgumentException(targetName + " ids must not be empty");
        }
        if (existingIds.size() != orderedIds.size()) {
            throw new IllegalArgumentException(targetName + " ids size mismatch");
        }
        Set<Long> existingSet = new LinkedHashSet<>(existingIds);
        Set<Long> orderedSet = new LinkedHashSet<>(orderedIds);
        if (existingSet.size() != existingIds.size()) {
            throw new IllegalArgumentException("existing " + targetName + " contains duplicate ids");
        }
        if (orderedSet.size() != orderedIds.size()) {
            throw new IllegalArgumentException("requested " + targetName + " contains duplicate ids");
        }
        if (!existingSet.equals(orderedSet)) {
            throw new IllegalArgumentException("requested " + targetName + " mismatch");
        }
    }

    private int normalizePositiveSort(Integer sortOrder, String fieldName) {
        if (sortOrder == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }
        if (sortOrder < 1) {
            throw new IllegalArgumentException(fieldName + " must be greater than 0");
        }
        return sortOrder;
    }

    private void reorderTabs(List<PortalTab> siblings, PortalTab target, int requestedSort) {
        siblings.removeIf(tab -> tab.getId().equals(target.getId()));
        int insertIndex = Math.min(Math.max(requestedSort - 1, 0), siblings.size());
        siblings.add(insertIndex, target);

        for (int i = 0; i < siblings.size(); i++) {
            int normalizedSort = i + 1;
            PortalTab tab = siblings.get(i);
            if (tab.getSortOrder() != normalizedSort) {
                tab.setSortOrder(normalizedSort);
            }
        }
    }

    private void reorderCategories(List<PortalCategory> siblings, PortalCategory target, int requestedSort) {
        siblings.removeIf(category -> category.getId().equals(target.getId()));
        int insertIndex = Math.min(Math.max(requestedSort - 1, 0), siblings.size());
        siblings.add(insertIndex, target);

        for (int i = 0; i < siblings.size(); i++) {
            int normalizedSort = i + 1;
            PortalCategory category = siblings.get(i);
            if (category.getSortOrder() != normalizedSort) {
                category.setSortOrder(normalizedSort);
            }
        }
    }

    private void reorderLinks(List<PortalLink> siblings, PortalLink target, int requestedSort) {
        siblings.removeIf(link -> link.getId().equals(target.getId()));
        int insertIndex = Math.min(Math.max(requestedSort - 1, 0), siblings.size());
        siblings.add(insertIndex, target);

        for (int i = 0; i < siblings.size(); i++) {
            int normalizedSort = i + 1;
            PortalLink link = siblings.get(i);
            if (link.getSortOrder() != normalizedSort) {
                link.setSortOrder(normalizedSort);
            }
        }
    }

    private String normalizeUrl(String url) {
        if (url == null) {
            throw new IllegalArgumentException("url must not be null");
        }
        String trimmed = url.trim();
        if (trimmed.isBlank()) {
            throw new IllegalArgumentException("url must not be blank");
        }
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }
        return "https://" + trimmed;
    }

    private String normalizeName(String value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }
        String trimmed = value.trim();
        if (trimmed.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return trimmed;
    }

    private String normalizeIcon(String icon, String fallback) {
        if (icon == null) return fallback;
        String trimmed = icon.trim();
        return trimmed.isBlank() ? fallback : trimmed;
    }

    private String normalizeIconColor(String iconColor, String fallback) {
        if (iconColor == null) return fallback;
        String trimmed = iconColor.trim();
        return trimmed.isBlank() ? fallback : trimmed;
    }

}
