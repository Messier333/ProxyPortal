package com.messier333.proxyportal.portal.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
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
        PortalTab portalTab = PortalTab.createTab(
                userRepository.findByUsername(username).orElseThrow(
                        () -> new IllegalArgumentException("User not found")
                ),
                tabCreateRequest.name(),
                tabCreateRequest.sortOrder()
        );
        Objects.requireNonNull(portalTab, "portalTab must not be null");
        PortalTab saved = portalTabRepository.save(portalTab);
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
                saved.getSortOrder(),
                List.of()
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

}
