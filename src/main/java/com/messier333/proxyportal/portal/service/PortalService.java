package com.messier333.proxyportal.portal.service;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.messier333.proxyportal.common.exception.BadRequestException;
import com.messier333.proxyportal.common.exception.ConflictException;
import com.messier333.proxyportal.common.exception.NotFoundException;
import com.messier333.proxyportal.common.util.ImageType;
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


    @Transactional
    public TabResponse createTab(String username, TabCreateRequest tabCreateRequest){
        String tabName = normalizeName(tabCreateRequest.name(), "tab name");
        if (portalTabRepository.existsByUserUsernameAndNameIgnoreCase(username, tabName)) {
            throw new ConflictException("이미 같은 이름의 탭이 있습니다.");
        }
        int nextSortOrder = nextTabSortOrder(username);
        PortalTab portalTab = PortalTab.createTab(
                userRepository.findByUsername(username).orElseThrow(
                        () -> new NotFoundException("User not found")
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
                .orElseThrow(() -> new NotFoundException("tab not found or user not matched"));

        String safeUsername = username.replaceAll("[^a-zA-Z0-9_-]", "_");
        String safeTabName = sanitizeFolderName(tab.getName(), "tab-" + tab.getId());
        Path uploadDir = resolveUploadRoot()
                .resolve("portal-tabs")
                .resolve(safeUsername)
                .resolve(safeTabName)
                .normalize();
        String originalFilename = backgroundImage.getOriginalFilename();
        String filename = sanitizeOriginalFilename(originalFilename);

        Path target;
        String storedFilename;
        try (BufferedInputStream input = new BufferedInputStream(backgroundImage.getInputStream())) {
            ImageType detected = validateUploadImage(filename, input);
            String resolvedFilename = ensureFilenameExtension(filename, detected);
            target = resolveUniqueUploadPath(uploadDir, resolvedFilename);
            storedFilename = target.getFileName().toString();

            if (!target.startsWith(uploadDir)) {
                throw new BadRequestException("Invalid file path");
            }

            Files.createDirectories(uploadDir);
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store uploaded image", e);
        }

        deleteBackgroundIfManaged(tab.getBackgroundUrl());
        tab.setBackgroundUrl(TAB_UPLOAD_PATH + safeUsername + "/" + safeTabName + "/" + storedFilename);
        portalTabRepository.save(tab);
    }

    @Transactional
    public void clearTabBackground(String username, Long tabId) {
        PortalTab tab = portalTabRepository.findByIdAndUserUsername(tabId, username)
                .orElseThrow(() -> new NotFoundException("tab not found or user not matched"));

        deleteBackgroundIfManaged(tab.getBackgroundUrl());
        tab.setBackgroundUrl(null);
        portalTabRepository.save(tab);
    }

    @Transactional
    public void renameTab(String username, Long tabId, String newName) {
        if (newName == null || newName.isBlank()) {
            throw new BadRequestException("tab name must not be blank");
        }

        PortalTab tab = portalTabRepository.findByIdAndUserUsername(tabId, username)
                .orElseThrow(() -> new NotFoundException("tab not found or user not matched"));

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

    private ImageType validateUploadImage(String filename, BufferedInputStream input) throws IOException {
        String extension = ImageType.extractExtension(filename);
        input.mark(ImageType.maxHeaderLength());
        byte[] header = new byte[ImageType.maxHeaderLength()];
        int read = input.read(header);
        input.reset();
        if (read <= 0) {
            throw new BadRequestException("Only image files are allowed");
        }
        ImageType detected = ImageType.detect(header, read);
        if (detected == null) {
            throw new BadRequestException("Only image files are allowed");
        }
        if (extension != null && !detected.matchesExtension(extension)) {
            throw new BadRequestException("Only image files are allowed");
        }
        return detected;
    }

    private String ensureFilenameExtension(String filename, ImageType detected) {
        if (ImageType.extractExtension(filename) != null) {
            return filename;
        }
        return filename + "." + detected.defaultExtension();
    }

    private Path resolveUniqueUploadPath(Path uploadDir, String filename) {
        Path target = uploadDir.resolve(filename).normalize();
        if (!Files.exists(target)) {
            return target;
        }
        String baseName = filename;
        String extension = "";
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < filename.length() - 1) {
            baseName = filename.substring(0, dotIndex);
            extension = filename.substring(dotIndex);
        }
        for (int i = 1; i <= 1000; i++) {
            String candidate = baseName + "-" + i + extension;
            Path candidatePath = uploadDir.resolve(candidate).normalize();
            if (!Files.exists(candidatePath)) {
                return candidatePath;
            }
        }
        throw new IllegalStateException("Failed to resolve unique upload path");
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
                () -> new NotFoundException("tab not found or user not matched")
        );
        String categoryName = normalizeName(categoryCreateRequest.name(), "category name");
        if (portalCategoryRepository.existsByTabIdAndNameIgnoreCase(tabId, categoryName)) {
            throw new ConflictException("이미 같은 이름의 카테고리가 있습니다.");
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
    public LinkResponse createLink(String username, Long categoryId, LinkCreateRequest linkCreateRequest) {
        PortalCategory portalCategory = portalCategoryRepository.findByIdAndTabUserUsername(categoryId, username).orElseThrow(
                () -> new NotFoundException("category not found or user not matched")
        );
        String linkName = normalizeName(linkCreateRequest.name(), "link name");
        String linkUrl = normalizeUrl(linkCreateRequest.url());
        String normalizedIcon = normalizeIcon(linkCreateRequest.icon());
        String normalizedIconColor = normalizeIconColor(linkCreateRequest.iconColor());
        if (portalLinkRepository.existsByCategoryIdAndNameIgnoreCase(categoryId, linkName)) {
            throw new ConflictException("이미 같은 이름의 링크가 있습니다.");
        }
        if (portalLinkRepository.existsByCategoryIdAndUrlIgnoreCase(categoryId, linkUrl)) {
            throw new ConflictException("이미 같은 URL 링크가 있습니다.");
        }
        int nextSortOrder = nextLinkSortOrder(categoryId);
        PortalLink portalLink = PortalLink.create(
                portalCategory,
                linkName,
                linkUrl,
                normalizedIcon,
                normalizedIconColor,
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
    public LinkResponse createLink(String username, Long categoryId, String name, String url, String icon, String iconColor, Integer sortOrder) {
        String normalizedUrl = normalizeUrl(url);
        LinkCreateRequest request = new LinkCreateRequest(
                name,
                normalizedUrl,
                normalizeIcon(icon),
                normalizeIconColor(iconColor),
                sortOrder
        );
        return createLink(username, categoryId, request);
    }

    @Transactional
    public LinkResponse createLink(String username, Long categoryId, String name, String url) {
        return createLink(username, categoryId, name, url, null, null, null);
    }

    @Transactional
    public LinkResponse createLink(String username, Long categoryId, String name, String url, String icon, String iconColor) {
        return createLink(username, categoryId, name, url, icon, iconColor, null);
    }

    @SuppressWarnings("null")
    @Transactional
    public CategoryResponse addCategorytoTab(String username, Long tabId, Long categoryId){
        PortalCategory portalCategory = portalCategoryRepository.findByIdAndTabUserUsername(categoryId,username).orElseThrow(
                () -> new NotFoundException("category not found or user not matched")
        );
        portalCategory.setCategoryTab(portalTabRepository.findByIdAndUserUsername(tabId, username).orElseThrow(
                () -> new NotFoundException("tab not found or user not matched")
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
                .orElseThrow(() -> new NotFoundException("tab not found or user not matched"));

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
                .orElseThrow(() -> new NotFoundException("category not found or user not matched"));
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
                .orElseThrow(() -> new NotFoundException("link not found or user not matched"));
        Long categoryId = target.getCategory().getId();

        portalLinkRepository.delete(target);
        normalizeLinkSortOrders(categoryId);
    }

    @Transactional
    public void updateTabSortOrder(String username, Long tabId, Integer sortOrder) {
        int requestedSort = normalizePositiveSort(sortOrder, "tab sortOrder");
        PortalTab target = portalTabRepository.findByIdAndUserUsername(tabId, username)
                .orElseThrow(() -> new NotFoundException("tab not found or user not matched"));
        List<PortalTab> siblings = new ArrayList<>(portalTabRepository.findAllByUserUsernameOrderBySortOrderAscIdAsc(username));
        reorderTabs(siblings, target, requestedSort);
    }

    @Transactional
    public void updateCategorySortOrder(String username, Long categoryId, Integer sortOrder) {
        int requestedSort = normalizePositiveSort(sortOrder, "category sortOrder");
        PortalCategory target = portalCategoryRepository.findByIdAndTabUserUsername(categoryId, username)
                .orElseThrow(() -> new NotFoundException("category not found or user not matched"));
        List<PortalCategory> siblings = new ArrayList<>(portalCategoryRepository.findAllByTabIdOrderBySortOrderAscIdAsc(target.getTab().getId()));
        reorderCategories(siblings, target, requestedSort);
    }

    @Transactional
    public void updateLinkSortOrder(String username, Long linkId, Integer sortOrder) {
        int requestedSort = normalizePositiveSort(sortOrder, "link sortOrder");
        PortalLink target = portalLinkRepository.findByIdAndCategoryTabUserUsername(linkId, username)
                .orElseThrow(() -> new NotFoundException("link not found or user not matched"));
        List<PortalLink> siblings = new ArrayList<>(portalLinkRepository.findAllByCategoryIdOrderBySortOrderAscIdAsc(target.getCategory().getId()));
        reorderLinks(siblings, target, requestedSort);
    }

    @Transactional
    public void reorderTabsByIds(String username, List<Long> orderedTabIds) {
        List<PortalTab> siblings = new ArrayList<>(portalTabRepository.findAllByUserUsernameOrderBySortOrderAscIdAsc(username));
        validateReorderIds(siblings.stream().map(PortalTab::getId).toList(), orderedTabIds, "tabs");
        HashMap<Long, PortalTab> siblingsById = new HashMap<>();
        for (PortalTab tab : siblings) {
            siblingsById.put(tab.getId(), tab);
        }
        for (int i = 0; i < orderedTabIds.size(); i++) {
            Long targetId = orderedTabIds.get(i);
            PortalTab target = siblingsById.get(targetId);
            if (target == null) {
                throw new NotFoundException("tab not found");
            }
            target.setSortOrder(i + 1);
        }
    }

    @Transactional
    public void reorderCategoriesByIds(String username, Long tabId, List<Long> orderedCategoryIds) {
        portalTabRepository.findByIdAndUserUsername(tabId, username)
                .orElseThrow(() -> new NotFoundException("tab not found or user not matched"));
        List<PortalCategory> siblings = new ArrayList<>(portalCategoryRepository.findAllByTabIdOrderBySortOrderAscIdAsc(tabId));
        validateReorderIds(siblings.stream().map(PortalCategory::getId).toList(), orderedCategoryIds, "categories");
        HashMap<Long, PortalCategory> siblingsById = new HashMap<>();
        for (PortalCategory category : siblings) {
            siblingsById.put(category.getId(), category);
        }
        for (int i = 0; i < orderedCategoryIds.size(); i++) {
            Long targetId = orderedCategoryIds.get(i);
            PortalCategory target = siblingsById.get(targetId);
            if (target == null) {
                throw new NotFoundException("category not found");
            }
            target.setSortOrder(i + 1);
        }
    }

    @Transactional
    public void reorderLinksByIds(String username, Long categoryId, List<Long> orderedLinkIds) {
        portalCategoryRepository.findByIdAndTabUserUsername(categoryId, username)
                .orElseThrow(() -> new NotFoundException("category not found or user not matched"));
        List<PortalLink> siblings = new ArrayList<>(portalLinkRepository.findAllByCategoryIdOrderBySortOrderAscIdAsc(categoryId));
        validateReorderIds(siblings.stream().map(PortalLink::getId).toList(), orderedLinkIds, "links");
        HashMap<Long, PortalLink> siblingsById = new HashMap<>();
        for (PortalLink link : siblings) {
            siblingsById.put(link.getId(), link);
        }
        for (int i = 0; i < orderedLinkIds.size(); i++) {
            Long targetId = orderedLinkIds.get(i);
            PortalLink target = siblingsById.get(targetId);
            if (target == null) {
                throw new NotFoundException("link not found");
            }
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
            throw new BadRequestException(targetName + " ids must not be empty");
        }
        if (existingIds.size() != orderedIds.size()) {
            throw new BadRequestException(targetName + " ids size mismatch");
        }
        Set<Long> existingSet = new LinkedHashSet<>(existingIds);
        Set<Long> orderedSet = new LinkedHashSet<>(orderedIds);
        if (existingSet.size() != existingIds.size()) {
            throw new BadRequestException("existing " + targetName + " contains duplicate ids");
        }
        if (orderedSet.size() != orderedIds.size()) {
            throw new BadRequestException("requested " + targetName + " contains duplicate ids");
        }
        if (!existingSet.equals(orderedSet)) {
            throw new BadRequestException("requested " + targetName + " mismatch");
        }
    }

    private int normalizePositiveSort(Integer sortOrder, String fieldName) {
        if (sortOrder == null) {
            throw new BadRequestException(fieldName + " must not be null");
        }
        if (sortOrder < 1) {
            throw new BadRequestException(fieldName + " must be greater than 0");
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
            throw new BadRequestException("url must not be null");
        }
        String trimmed = url.trim();
        if (trimmed.isBlank()) {
            throw new BadRequestException("url must not be blank");
        }
        if (trimmed.startsWith("//")) {
            return "https:" + trimmed;
        }
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (lower.startsWith("javascript:") || lower.startsWith("data:") || lower.startsWith("vbscript:")) {
            throw new BadRequestException("invalid url scheme");
        }
        boolean hasScheme = trimmed.matches("^[a-zA-Z][a-zA-Z0-9+.-]*:.*");
        if (hasScheme) {
            if (lower.startsWith("http://") || lower.startsWith("https://")) {
                return trimmed;
            }
            throw new BadRequestException("only http/https urls are allowed");
        }
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }
        return "https://" + trimmed;
    }

    private String normalizeName(String value, String fieldName) {
        if (value == null) {
            throw new BadRequestException(fieldName + " must not be null");
        }
        String trimmed = value.trim();
        if (trimmed.isBlank()) {
            throw new BadRequestException(fieldName + " must not be blank");
        }
        return trimmed;
    }

    private String normalizeIcon(String icon) {
        if (icon == null) return PortalService.DEFAULT_LINK_ICON;
        String trimmed = icon.trim();
        return trimmed.isBlank() ? PortalService.DEFAULT_LINK_ICON : trimmed;
    }

    private String normalizeIconColor(String iconColor) {
        if (iconColor == null) return PortalService.DEFAULT_LINK_ICON_COLOR;
        String trimmed = iconColor.trim();
        return trimmed.isBlank() ? PortalService.DEFAULT_LINK_ICON_COLOR : trimmed;
    }

}
