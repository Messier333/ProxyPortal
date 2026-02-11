package com.messier333.proxyportal.portal.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import com.messier333.proxyportal.common.exception.BadRequestException;
import com.messier333.proxyportal.common.exception.ConflictException;
import com.messier333.proxyportal.common.exception.NotFoundException;
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
import com.messier333.proxyportal.user.entity.Role;
import com.messier333.proxyportal.user.entity.User;
import com.messier333.proxyportal.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class PortalServiceTest {

    @Mock
    private PortalCategoryRepository portalCategoryRepository;
    @Mock
    private PortalLinkRepository portalLinkRepository;
    @Mock
    private PortalTabRepository portalTabRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PortalQueryRepository portalQueryRepository;

    @InjectMocks
    private PortalService portalService;

    @Test
    void getPortalTabs_shouldDelegateToQueryRepository() {
        PortalTabsResponse response = new PortalTabsResponse(List.of());
        when(portalQueryRepository.findTabsByUsername("alice")).thenReturn(response);

        PortalTabsResponse result = portalService.getPortalTabs("alice");

        assertThat(result).isSameAs(response);
    }

    @SuppressWarnings("null")
    @Test
    void createTab_shouldCreateWithTrimmedNameAndNextSort() {
        User user = user(1L, "alice");
        when(portalTabRepository.existsByUserUsernameAndNameIgnoreCase("alice", "Work")).thenReturn(false);
        when(portalTabRepository.findAllByUserUsernameOrderBySortOrderAscIdAsc("alice"))
                .thenReturn(List.of(tab(10L, user, "Old", 1)));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(portalTabRepository.save(any(PortalTab.class))).thenAnswer(invocation -> {
            PortalTab saved = invocation.getArgument(0, PortalTab.class);
            ReflectionTestUtils.setField(saved, "id", 20L);
            return saved;
        });

        TabResponse result = portalService.createTab("alice", new TabCreateRequest("  Work  ", null, List.of()));

        assertThat(result.id()).isEqualTo(20L);
        assertThat(result.name()).isEqualTo("Work");
        assertThat(result.sortOrder()).isEqualTo(2);
    }

    @Test
    void createTab_shouldThrowWhenDuplicateNameExists() {
        when(portalTabRepository.existsByUserUsernameAndNameIgnoreCase("alice", "Work")).thenReturn(true);

        assertThatThrownBy(() -> portalService.createTab("alice", new TabCreateRequest("Work", null, List.of())))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void createTab_shouldThrowWhenNameBlank() {
        assertThatThrownBy(() -> portalService.createTab("alice", new TabCreateRequest("   ", null, List.of())))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void createTab_shouldThrowWhenNameNull() {
        assertThatThrownBy(() -> portalService.createTab("alice", new TabCreateRequest(null, null, List.of())))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void createTab_shouldThrowWhenUserMissing() {
        when(portalTabRepository.existsByUserUsernameAndNameIgnoreCase("alice", "Work")).thenReturn(false);
        when(portalTabRepository.findAllByUserUsernameOrderBySortOrderAscIdAsc("alice")).thenReturn(List.of());
        when(userRepository.findByUsername("alice")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> portalService.createTab("alice", new TabCreateRequest("Work", null, List.of())))
                .isInstanceOf(NotFoundException.class);
    }

    @SuppressWarnings("null")
    @Test
    void createTab_shouldApplyRequestedSortOrder() {
        User user = user(1L, "alice");
        PortalTab first = tab(10L, user, "First", 1);
        when(portalTabRepository.existsByUserUsernameAndNameIgnoreCase("alice", "Work")).thenReturn(false);
        when(portalTabRepository.findAllByUserUsernameOrderBySortOrderAscIdAsc("alice"))
                .thenReturn(List.of(first))
                .thenReturn(List.of(first, tab(20L, user, "Work", 2)));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(portalTabRepository.save(any(PortalTab.class))).thenAnswer(invocation -> {
            PortalTab saved = invocation.getArgument(0, PortalTab.class);
            ReflectionTestUtils.setField(saved, "id", 20L);
            return saved;
        });
        when(portalTabRepository.findByIdAndUserUsername(20L, "alice"))
                .thenReturn(Optional.of(tab(20L, user, "Work", 2)));

        TabResponse result = portalService.createTab("alice", new TabCreateRequest("Work", 1, List.of()));

        assertThat(result.id()).isEqualTo(20L);
        assertThat(result.sortOrder()).isEqualTo(2);
    }

    @SuppressWarnings("null")
    @Test
    void createCategory_shouldCreateWithNextSort() {
        User user = user(1L, "alice");
        PortalTab tab = tab(100L, user, "Tab", 1);
        when(portalTabRepository.findByIdAndUserUsername(100L, "alice")).thenReturn(Optional.of(tab));
        when(portalCategoryRepository.existsByTabIdAndNameIgnoreCase(100L, "News")).thenReturn(false);
        when(portalCategoryRepository.findAllByTabIdOrderBySortOrderAscIdAsc(100L))
                .thenReturn(List.of(category(200L, tab, "Old", 1)));
        when(portalCategoryRepository.save(any(PortalCategory.class))).thenAnswer(invocation -> {
            PortalCategory saved = invocation.getArgument(0, PortalCategory.class);
            ReflectionTestUtils.setField(saved, "id", 201L);
            return saved;
        });

        CategoryResponse result = portalService.createCategory("alice", 100L, new CategoryCreateRequest(" News ", List.of(), null));

        assertThat(result.id()).isEqualTo(201L);
        assertThat(result.name()).isEqualTo("News");
        assertThat(result.sortOrder()).isEqualTo(2);
    }

    @SuppressWarnings("null")
    @Test
    void createLink_shouldNormalizeUrlAndFallbackIconValues() {
        User user = user(1L, "alice");
        PortalTab tab = tab(10L, user, "Tab", 1);
        PortalCategory category = category(20L, tab, "Cat", 1);
        when(portalCategoryRepository.findByIdAndTabUserUsername(20L, "alice")).thenReturn(Optional.of(category));
        when(portalLinkRepository.existsByCategoryIdAndNameIgnoreCase(20L, "Google")).thenReturn(false);
        when(portalLinkRepository.existsByCategoryIdAndUrlIgnoreCase(20L, "https://example.com")).thenReturn(false);
        when(portalLinkRepository.findAllByCategoryIdOrderBySortOrderAscIdAsc(20L))
                .thenReturn(List.of(link(30L, category, "Old", "https://old.com", "x", "#000", 1)));
        when(portalLinkRepository.save(any(PortalLink.class))).thenAnswer(invocation -> {
            PortalLink saved = invocation.getArgument(0, PortalLink.class);
            ReflectionTestUtils.setField(saved, "id", 31L);
            return saved;
        });

        LinkResponse result = portalService.createLink("alice", 20L, "  Google ", "example.com", " ", " ", null);

        assertThat(result.id()).isEqualTo(31L);
        assertThat(result.name()).isEqualTo("Google");
        assertThat(result.url()).isEqualTo("https://example.com");
        assertThat(result.icon()).isEqualTo("link");
        assertThat(result.iconColor()).isEqualTo("#89b4fa");
        assertThat(result.sortOrder()).isEqualTo(2);
    }

    @Test
    void createLink_shouldThrowWhenDuplicateUrlExists() {
        User user = user(1L, "alice");
        PortalTab tab = tab(10L, user, "Tab", 1);
        PortalCategory category = category(20L, tab, "Cat", 1);
        when(portalCategoryRepository.findByIdAndTabUserUsername(20L, "alice")).thenReturn(Optional.of(category));
        when(portalLinkRepository.existsByCategoryIdAndNameIgnoreCase(20L, "Google")).thenReturn(false);
        when(portalLinkRepository.existsByCategoryIdAndUrlIgnoreCase(20L, "https://example.com")).thenReturn(true);

        assertThatThrownBy(() -> portalService.createLink("alice", 20L, new LinkCreateRequest(
                "Google", "https://example.com", "link", "#000", null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void createLink_shouldThrowWhenDuplicateNameExists() {
        User user = user(1L, "alice");
        PortalTab tab = tab(10L, user, "Tab", 1);
        PortalCategory category = category(20L, tab, "Cat", 1);
        when(portalCategoryRepository.findByIdAndTabUserUsername(20L, "alice")).thenReturn(Optional.of(category));
        when(portalLinkRepository.existsByCategoryIdAndNameIgnoreCase(20L, "Google")).thenReturn(true);

        assertThatThrownBy(() -> portalService.createLink("alice", 20L, new LinkCreateRequest(
                "Google", "https://example.com", "link", "#000", null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void createLink_shouldThrowWhenUrlNull() {
        User user = user(1L, "alice");
        PortalTab tab = tab(10L, user, "Tab", 1);
        PortalCategory category = category(20L, tab, "Cat", 1);
        when(portalCategoryRepository.findByIdAndTabUserUsername(20L, "alice")).thenReturn(Optional.of(category));

        assertThatThrownBy(() -> portalService.createLink("alice", 20L, new LinkCreateRequest(
                "Google", null, "link", "#000", null)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void createLink_shouldThrowWhenUrlBlank() {
        User user = user(1L, "alice");
        PortalTab tab = tab(10L, user, "Tab", 1);
        PortalCategory category = category(20L, tab, "Cat", 1);
        when(portalCategoryRepository.findByIdAndTabUserUsername(20L, "alice")).thenReturn(Optional.of(category));

        assertThatThrownBy(() -> portalService.createLink("alice", 20L, new LinkCreateRequest(
                "Google", "   ", "link", "#000", null)))
                .isInstanceOf(BadRequestException.class);
    }

    @SuppressWarnings("null")
    @Test
    void createLink_shouldApplyRequestedSortOrder() {
        User user = user(1L, "alice");
        PortalTab tab = tab(10L, user, "Tab", 1);
        PortalCategory category = category(20L, tab, "Cat", 1);
        PortalLink old = link(30L, category, "Old", "https://old.com", "i", "#000", 1);
        PortalLink savedLink = link(31L, category, "Google", "https://example.com", "i", "#000", 2);
        when(portalCategoryRepository.findByIdAndTabUserUsername(20L, "alice")).thenReturn(Optional.of(category));
        when(portalLinkRepository.existsByCategoryIdAndNameIgnoreCase(20L, "Google")).thenReturn(false);
        when(portalLinkRepository.existsByCategoryIdAndUrlIgnoreCase(20L, "https://example.com")).thenReturn(false);
        when(portalLinkRepository.findAllByCategoryIdOrderBySortOrderAscIdAsc(20L))
                .thenReturn(List.of(old))
                .thenReturn(List.of(old, savedLink));
        when(portalLinkRepository.save(any(PortalLink.class))).thenAnswer(invocation -> {
            PortalLink saved = invocation.getArgument(0, PortalLink.class);
            ReflectionTestUtils.setField(saved, "id", 31L);
            return saved;
        });
        when(portalLinkRepository.findByIdAndCategoryTabUserUsername(31L, "alice")).thenReturn(Optional.of(savedLink));

        LinkResponse result = portalService.createLink("alice", 20L, new LinkCreateRequest(
                "Google", "example.com", "i", "#000", 1));

        assertThat(result.id()).isEqualTo(31L);
        assertThat(result.sortOrder()).isEqualTo(2);
    }

    @Test
    void createCategory_shouldThrowWhenDuplicateNameExists() {
        User user = user(1L, "alice");
        PortalTab tab = tab(100L, user, "Tab", 1);
        when(portalTabRepository.findByIdAndUserUsername(100L, "alice")).thenReturn(Optional.of(tab));
        when(portalCategoryRepository.existsByTabIdAndNameIgnoreCase(100L, "News")).thenReturn(true);

        assertThatThrownBy(() -> portalService.createCategory("alice", 100L, new CategoryCreateRequest("News", List.of(), null)))
                .isInstanceOf(ConflictException.class);
    }

    @SuppressWarnings("null")
    @Test
    void createCategory_shouldApplyRequestedSortOrder() {
        User user = user(1L, "alice");
        PortalTab tab = tab(100L, user, "Tab", 1);
        PortalCategory existing = category(200L, tab, "Old", 1);
        PortalCategory savedCategory = category(201L, tab, "News", 2);
        when(portalTabRepository.findByIdAndUserUsername(100L, "alice")).thenReturn(Optional.of(tab));
        when(portalCategoryRepository.existsByTabIdAndNameIgnoreCase(100L, "News")).thenReturn(false);
        when(portalCategoryRepository.findAllByTabIdOrderBySortOrderAscIdAsc(100L))
                .thenReturn(List.of(existing))
                .thenReturn(List.of(existing, savedCategory));
        when(portalCategoryRepository.save(any(PortalCategory.class))).thenAnswer(invocation -> {
            PortalCategory saved = invocation.getArgument(0, PortalCategory.class);
            ReflectionTestUtils.setField(saved, "id", 201L);
            return saved;
        });
        when(portalCategoryRepository.findByIdAndTabUserUsername(201L, "alice")).thenReturn(Optional.of(savedCategory));

        CategoryResponse result = portalService.createCategory("alice", 100L, new CategoryCreateRequest("News", List.of(), 1));

        assertThat(result.id()).isEqualTo(201L);
        assertThat(result.sortOrder()).isEqualTo(2);
    }

    @SuppressWarnings("null")
    @Test
    void uploadTabBackground_shouldStoreImageAndUpdateBackgroundUrl() throws Exception {
        Path uploadRoot = Files.createTempDirectory("portal-upload-test");
        ReflectionTestUtils.setField(portalService, "uploadRoot", uploadRoot.toString());

        User user = user(1L, "alice");
        PortalTab tab = tab(100L, user, "My Tab", 1);
        when(portalTabRepository.findByIdAndUserUsername(100L, "alice")).thenReturn(Optional.of(tab));
        MockMultipartFile file = new MockMultipartFile(
                "backgroundImage",
                "wallpaper.png",
                "image/png",
                pngBytes());

        portalService.uploadTabBackground("alice", 100L, file);

        assertThat(tab.getBackgroundUrl()).isEqualTo("/uploads/portal-tabs/alice/My Tab/wallpaper.png");
        Path stored = uploadRoot.resolve("portal-tabs/alice/My Tab/wallpaper.png");
        assertThat(Files.exists(stored)).isTrue();
        verify(portalTabRepository).save(tab);
    }

    @Test
    void uploadTabBackground_shouldThrowWhenFileIsNotImage() throws Exception {
        Path uploadRoot = Files.createTempDirectory("portal-upload-test");
        ReflectionTestUtils.setField(portalService, "uploadRoot", uploadRoot.toString());

        User user = user(1L, "alice");
        PortalTab tab = tab(100L, user, "My Tab", 1);
        when(portalTabRepository.findByIdAndUserUsername(100L, "alice")).thenReturn(Optional.of(tab));
        MockMultipartFile file = new MockMultipartFile(
                "backgroundImage",
                "notes.txt",
                "text/plain",
                "hello".getBytes());

        assertThatThrownBy(() -> portalService.uploadTabBackground("alice", 100L, file))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void uploadTabBackground_shouldReturnWhenFileIsNullOrEmpty() {
        MockMultipartFile emptyFile = new MockMultipartFile("backgroundImage", "empty.png", "image/png", new byte[0]);

        portalService.uploadTabBackground("alice", 100L, null);
        portalService.uploadTabBackground("alice", 100L, emptyFile);

        verifyNoInteractions(portalTabRepository);
    }

    @Test
    void uploadTabBackground_shouldThrowWhenPathTraversalDetected() throws Exception {
        Path uploadRoot = Files.createTempDirectory("portal-path-traversal-test");
        ReflectionTestUtils.setField(portalService, "uploadRoot", uploadRoot.toString());

        User user = user(1L, "alice");
        PortalTab tab = tab(100L, user, "My Tab", 1);
        when(portalTabRepository.findByIdAndUserUsername(100L, "alice")).thenReturn(Optional.of(tab));
        MockMultipartFile file = new MockMultipartFile(
                "backgroundImage",
                "..",
                "image/png",
                "bin".getBytes());

        assertThatThrownBy(() -> portalService.uploadTabBackground("alice", 100L, file))
                .isInstanceOf(BadRequestException.class);
    }

    @SuppressWarnings("null")
    @Test
    void uploadTabBackground_shouldThrowWhenStoreFails() throws Exception {
        Path uploadRoot = Files.createTempDirectory("portal-upload-io-fail");
        ReflectionTestUtils.setField(portalService, "uploadRoot", uploadRoot.toString());

        User user = user(1L, "alice");
        PortalTab tab = tab(100L, user, "My Tab", 1);
        when(portalTabRepository.findByIdAndUserUsername(100L, "alice")).thenReturn(Optional.of(tab));
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("a.png");
        when(file.getInputStream()).thenThrow(new IOException("fail"));

        assertThatThrownBy(() -> portalService.uploadTabBackground("alice", 100L, file))
                .isInstanceOf(IllegalStateException.class);
    }

    @SuppressWarnings("null")
    @Test
    void uploadTabBackground_shouldUseFallbackFilenameWhenOriginalBlank() throws Exception {
        Path uploadRoot = Files.createTempDirectory("portal-upload-fallback-name");
        ReflectionTestUtils.setField(portalService, "uploadRoot", uploadRoot.toString());

        User user = user(1L, "alice");
        PortalTab tab = tab(100L, user, "My Tab", 1);
        when(portalTabRepository.findByIdAndUserUsername(100L, "alice")).thenReturn(Optional.of(tab));
        MockMultipartFile file = new MockMultipartFile(
                "backgroundImage",
                "",
                "image/png",
                pngBytes());

        portalService.uploadTabBackground("alice", 100L, file);

        assertThat(tab.getBackgroundUrl()).endsWith("/uploaded-image.png");
    }

    @SuppressWarnings("null")
    @Test
    void clearTabBackground_shouldDeleteManagedFileAndSetNull() throws Exception {
        Path uploadRoot = Files.createTempDirectory("portal-clear-bg-test");
        ReflectionTestUtils.setField(portalService, "uploadRoot", uploadRoot.toString());

        User user = user(1L, "alice");
        PortalTab tab = tab(100L, user, "My Tab", 1);
        tab.setBackgroundUrl("/uploads/portal-tabs/alice/My Tab/wallpaper.png");
        Path target = uploadRoot.resolve("portal-tabs/alice/My Tab/wallpaper.png");
        Files.createDirectories(Objects.requireNonNull(target.getParent()));
        Files.writeString(target, "old");
        when(portalTabRepository.findByIdAndUserUsername(100L, "alice")).thenReturn(Optional.of(tab));

        portalService.clearTabBackground("alice", 100L);

        assertThat(tab.getBackgroundUrl()).isNull();
        assertThat(Files.exists(target)).isFalse();
        verify(portalTabRepository).save(tab);
    }

    @SuppressWarnings("null")
    @Test
    void clearTabBackground_shouldIgnoreDeleteIOException() throws Exception {
        Path uploadRoot = Files.createTempDirectory("portal-clear-bg-io");
        ReflectionTestUtils.setField(portalService, "uploadRoot", uploadRoot.toString());

        User user = user(1L, "alice");
        PortalTab tab = tab(100L, user, "My Tab", 1);
        tab.setBackgroundUrl("/uploads/portal-tabs/alice/My Tab/wallpaper.png");
        Path target = uploadRoot.resolve("portal-tabs/alice/My Tab/wallpaper.png");
        Files.createDirectories(target);
        Files.writeString(target.resolve("child.txt"), "child");
        when(portalTabRepository.findByIdAndUserUsername(100L, "alice")).thenReturn(Optional.of(tab));

        portalService.clearTabBackground("alice", 100L);

        assertThat(tab.getBackgroundUrl()).isNull();
        verify(portalTabRepository).save(tab);
    }

    @Test
    void renameTab_shouldThrowWhenNameBlank() {
        assertThatThrownBy(() -> portalService.renameTab("alice", 1L, "  "))
                .isInstanceOf(BadRequestException.class);
    }

    @SuppressWarnings("null")
    @Test
    void renameTab_shouldReturnWithoutSaveWhenNameUnchanged() {
        User user = user(1L, "alice");
        PortalTab tab = tab(1L, user, "Same", 1);
        when(portalTabRepository.findByIdAndUserUsername(1L, "alice")).thenReturn(Optional.of(tab));

        portalService.renameTab("alice", 1L, "Same");

        verify(portalTabRepository, never()).save(any(PortalTab.class));
    }

    @SuppressWarnings("null")
    @Test
    void renameTab_shouldMoveFolderAndUpdateBackgroundUrl() throws Exception {
        Path uploadRoot = Files.createTempDirectory("portal-rename-bg-test");
        ReflectionTestUtils.setField(portalService, "uploadRoot", uploadRoot.toString());

        User user = user(1L, "alice");
        PortalTab tab = tab(1L, user, "Old", 1);
        tab.setBackgroundUrl("/uploads/portal-tabs/alice/Old/bg.png");
        Path oldFile = uploadRoot.resolve("portal-tabs/alice/Old/bg.png");
        Files.createDirectories(Objects.requireNonNull(oldFile.getParent()));
        Files.writeString(oldFile, "old");
        when(portalTabRepository.findByIdAndUserUsername(1L, "alice")).thenReturn(Optional.of(tab));

        portalService.renameTab("alice", 1L, "New");

        assertThat(tab.getName()).isEqualTo("New");
        assertThat(tab.getBackgroundUrl()).isEqualTo("/uploads/portal-tabs/alice/New/bg.png");
        assertThat(Files.exists(uploadRoot.resolve("portal-tabs/alice/New/bg.png"))).isTrue();
        assertThat(Files.exists(uploadRoot.resolve("portal-tabs/alice/Old/bg.png"))).isFalse();
        verify(portalTabRepository).save(tab);
    }

    @SuppressWarnings("null")
    @Test
    void renameTab_shouldCopyToExistingFolderAndDeleteOldFolder() throws Exception {
        Path uploadRoot = Files.createTempDirectory("portal-rename-bg-existing");
        ReflectionTestUtils.setField(portalService, "uploadRoot", uploadRoot.toString());

        User user = user(1L, "alice");
        PortalTab tab = tab(1L, user, "Old", 1);
        tab.setBackgroundUrl("/uploads/portal-tabs/alice/Old/bg.png");
        Path oldFile = uploadRoot.resolve("portal-tabs/alice/Old/bg.png");
        Path newDir = uploadRoot.resolve("portal-tabs/alice/New");
        Files.createDirectories(Objects.requireNonNull(oldFile.getParent()));
        Files.createDirectories(newDir);
        Files.writeString(oldFile, "old");
        Files.writeString(newDir.resolve("keep.txt"), "new");
        when(portalTabRepository.findByIdAndUserUsername(1L, "alice")).thenReturn(Optional.of(tab));

        portalService.renameTab("alice", 1L, "New");

        assertThat(Files.exists(uploadRoot.resolve("portal-tabs/alice/New/bg.png"))).isTrue();
        assertThat(Files.exists(uploadRoot.resolve("portal-tabs/alice/Old"))).isFalse();
        assertThat(tab.getBackgroundUrl()).isEqualTo("/uploads/portal-tabs/alice/New/bg.png");
    }

    @SuppressWarnings("null")
    @Test
    void renameTab_shouldSkipMovingWhenBackgroundNotManaged() {
        User user = user(1L, "alice");
        PortalTab tab = tab(1L, user, "Old", 1);
        tab.setBackgroundUrl("https://external/image.png");
        when(portalTabRepository.findByIdAndUserUsername(1L, "alice")).thenReturn(Optional.of(tab));

        portalService.renameTab("alice", 1L, "New");

        assertThat(tab.getName()).isEqualTo("New");
        assertThat(tab.getBackgroundUrl()).isEqualTo("https://external/image.png");
        verify(portalTabRepository).save(tab);
    }

    @SuppressWarnings("null")
    @Test
    void renameTab_shouldKeepBackgroundWhenOldFolderMissing() throws Exception {
        Path uploadRoot = Files.createTempDirectory("portal-rename-missing-old");
        ReflectionTestUtils.setField(portalService, "uploadRoot", uploadRoot.toString());

        User user = user(1L, "alice");
        PortalTab tab = tab(1L, user, "Old", 1);
        tab.setBackgroundUrl("/uploads/portal-tabs/alice/Old/bg.png");
        when(portalTabRepository.findByIdAndUserUsername(1L, "alice")).thenReturn(Optional.of(tab));

        portalService.renameTab("alice", 1L, "New");

        assertThat(tab.getName()).isEqualTo("New");
        assertThat(tab.getBackgroundUrl()).isEqualTo("/uploads/portal-tabs/alice/Old/bg.png");
    }

    @SuppressWarnings("null")
    @Test
    void deleteLink_shouldDeleteAndNormalizeSortOrders() {
        User user = user(1L, "alice");
        PortalTab tab = tab(10L, user, "Tab", 1);
        PortalCategory category = category(20L, tab, "Cat", 1);
        PortalLink target = link(1L, category, "A", "https://a.com", "x", "#000", 1);
        PortalLink remain = link(2L, category, "B", "https://b.com", "x", "#000", 3);
        when(portalLinkRepository.findByIdAndCategoryTabUserUsername(1L, "alice")).thenReturn(Optional.of(target));
        when(portalLinkRepository.findAllByCategoryIdOrderBySortOrderAscIdAsc(20L)).thenReturn(List.of(remain));

        portalService.deleteLink("alice", 1L);

        verify(portalLinkRepository).delete(target);
        assertThat(remain.getSortOrder()).isEqualTo(1);
    }

    @SuppressWarnings("null")
    @Test
    void deleteLink_shouldKeepSortWhenAlreadyNormalized() {
        User user = user(1L, "alice");
        PortalTab tab = tab(10L, user, "Tab", 1);
        PortalCategory category = category(20L, tab, "Cat", 1);
        PortalLink target = link(1L, category, "A", "https://a.com", "x", "#000", 1);
        PortalLink remain = link(2L, category, "B", "https://b.com", "x", "#000", 1);
        when(portalLinkRepository.findByIdAndCategoryTabUserUsername(1L, "alice")).thenReturn(Optional.of(target));
        when(portalLinkRepository.findAllByCategoryIdOrderBySortOrderAscIdAsc(20L)).thenReturn(List.of(remain));

        portalService.deleteLink("alice", 1L);

        assertThat(remain.getSortOrder()).isEqualTo(1);
    }

    @SuppressWarnings("null")
    @Test
    void deleteCategory_shouldDeleteLinksAndNormalizeCategorySortOrders() {
        User user = user(1L, "alice");
        PortalTab tab = tab(10L, user, "Tab", 1);
        PortalCategory target = category(1L, tab, "A", 1);
        PortalCategory remain = category(2L, tab, "B", 4);
        PortalLink child = link(100L, target, "L", "https://a.com", "x", "#000", 1);
        when(portalCategoryRepository.findByIdAndTabUserUsername(1L, "alice")).thenReturn(Optional.of(target));
        when(portalLinkRepository.findAllByCategoryIdOrderBySortOrderAscIdAsc(1L)).thenReturn(List.of(child));
        when(portalCategoryRepository.findAllByTabIdOrderBySortOrderAscIdAsc(10L)).thenReturn(List.of(remain));

        portalService.deleteCategory("alice", 1L);

        verify(portalLinkRepository).deleteAll(List.of(child));
        verify(portalCategoryRepository).delete(target);
        assertThat(remain.getSortOrder()).isEqualTo(1);
    }

    @SuppressWarnings("null")
    @Test
    void deleteCategory_shouldHandleWhenNoLinks() {
        User user = user(1L, "alice");
        PortalTab tab = tab(10L, user, "Tab", 1);
        PortalCategory target = category(1L, tab, "A", 1);
        PortalCategory remain = category(2L, tab, "B", 1);
        when(portalCategoryRepository.findByIdAndTabUserUsername(1L, "alice")).thenReturn(Optional.of(target));
        when(portalLinkRepository.findAllByCategoryIdOrderBySortOrderAscIdAsc(1L)).thenReturn(List.of());
        when(portalCategoryRepository.findAllByTabIdOrderBySortOrderAscIdAsc(10L)).thenReturn(List.of(remain));

        portalService.deleteCategory("alice", 1L);

        verify(portalCategoryRepository).delete(target);
        assertThat(remain.getSortOrder()).isEqualTo(1);
    }

    @SuppressWarnings("null")
    @Test
    void deleteTab_shouldDeleteChildrenAndNormalizeTabSortOrders() {
        User user = user(1L, "alice");
        PortalTab target = tab(10L, user, "A", 1);
        PortalCategory cat1 = category(1L, target, "C1", 1);
        PortalCategory cat2 = category(2L, target, "C2", 2);
        PortalLink link1 = link(11L, cat1, "L1", "https://1.com", "x", "#000", 1);
        PortalLink link2 = link(12L, cat2, "L2", "https://2.com", "x", "#000", 1);
        PortalTab remain = tab(20L, user, "B", 5);

        when(portalTabRepository.findByIdAndUserUsername(10L, "alice")).thenReturn(Optional.of(target));
        when(portalCategoryRepository.findAllByTabIdOrderBySortOrderAscIdAsc(10L)).thenReturn(List.of(cat1, cat2));
        when(portalLinkRepository.findAllByCategoryIdOrderBySortOrderAscIdAsc(1L)).thenReturn(List.of(link1));
        when(portalLinkRepository.findAllByCategoryIdOrderBySortOrderAscIdAsc(2L)).thenReturn(List.of(link2));
        when(portalTabRepository.findAllByUserUsernameOrderBySortOrderAscIdAsc("alice")).thenReturn(List.of(remain));

        portalService.deleteTab("alice", 10L);

        verify(portalLinkRepository).deleteAll(List.of(link1));
        verify(portalLinkRepository).deleteAll(List.of(link2));
        verify(portalCategoryRepository).deleteAll(List.of(cat1, cat2));
        verify(portalTabRepository).delete(target);
        assertThat(remain.getSortOrder()).isEqualTo(1);
    }

    @SuppressWarnings("null")
    @Test
    void deleteTab_shouldHandleWhenNoCategories() {
        User user = user(1L, "alice");
        PortalTab target = tab(10L, user, "A", 1);
        PortalTab remain = tab(20L, user, "B", 1);

        when(portalTabRepository.findByIdAndUserUsername(10L, "alice")).thenReturn(Optional.of(target));
        when(portalCategoryRepository.findAllByTabIdOrderBySortOrderAscIdAsc(10L)).thenReturn(List.of());
        when(portalTabRepository.findAllByUserUsernameOrderBySortOrderAscIdAsc("alice")).thenReturn(List.of(remain));

        portalService.deleteTab("alice", 10L);

        verify(portalTabRepository).delete(target);
        assertThat(remain.getSortOrder()).isEqualTo(1);
    }

    @Test
    void updateTabSortOrder_shouldReorderSiblings() {
        User user = user(1L, "alice");
        PortalTab first = tab(1L, user, "First", 1);
        PortalTab target = tab(2L, user, "Second", 2);
        when(portalTabRepository.findByIdAndUserUsername(2L, "alice")).thenReturn(Optional.of(target));
        when(portalTabRepository.findAllByUserUsernameOrderBySortOrderAscIdAsc("alice")).thenReturn(List.of(first, target));

        portalService.updateTabSortOrder("alice", 2L, 1);

        assertThat(target.getSortOrder()).isEqualTo(1);
        assertThat(first.getSortOrder()).isEqualTo(2);
    }

    @Test
    void updateTabSortOrder_shouldThrowWhenSortOrderIsNull() {
        assertThatThrownBy(() -> portalService.updateTabSortOrder("alice", 1L, null))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void updateTabSortOrder_shouldThrowWhenSortOrderIsZero() {
        assertThatThrownBy(() -> portalService.updateTabSortOrder("alice", 1L, 0))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void updateTabSortOrder_shouldThrowWhenTargetNotFound() {
        when(portalTabRepository.findByIdAndUserUsername(2L, "alice")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> portalService.updateTabSortOrder("alice", 2L, 1))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateCategorySortOrder_shouldReorderSiblings() {
        User user = user(1L, "alice");
        PortalTab tab = tab(10L, user, "Tab", 1);
        PortalCategory first = category(1L, tab, "First", 1);
        PortalCategory target = category(2L, tab, "Second", 2);
        when(portalCategoryRepository.findByIdAndTabUserUsername(2L, "alice")).thenReturn(Optional.of(target));
        when(portalCategoryRepository.findAllByTabIdOrderBySortOrderAscIdAsc(10L)).thenReturn(List.of(first, target));

        portalService.updateCategorySortOrder("alice", 2L, 1);

        assertThat(target.getSortOrder()).isEqualTo(1);
        assertThat(first.getSortOrder()).isEqualTo(2);
    }

    @Test
    void updateCategorySortOrder_shouldThrowWhenTargetNotFound() {
        when(portalCategoryRepository.findByIdAndTabUserUsername(2L, "alice")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> portalService.updateCategorySortOrder("alice", 2L, 1))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateLinkSortOrder_shouldReorderSiblings() {
        User user = user(1L, "alice");
        PortalTab tab = tab(10L, user, "Tab", 1);
        PortalCategory category = category(20L, tab, "Cat", 1);
        PortalLink first = link(1L, category, "First", "https://a.com", "i", "#000", 1);
        PortalLink target = link(2L, category, "Second", "https://b.com", "i", "#000", 2);
        when(portalLinkRepository.findByIdAndCategoryTabUserUsername(2L, "alice")).thenReturn(Optional.of(target));
        when(portalLinkRepository.findAllByCategoryIdOrderBySortOrderAscIdAsc(20L)).thenReturn(List.of(first, target));

        portalService.updateLinkSortOrder("alice", 2L, 1);

        assertThat(target.getSortOrder()).isEqualTo(1);
        assertThat(first.getSortOrder()).isEqualTo(2);
    }

    @Test
    void updateLinkSortOrder_shouldThrowWhenTargetNotFound() {
        when(portalLinkRepository.findByIdAndCategoryTabUserUsername(2L, "alice")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> portalService.updateLinkSortOrder("alice", 2L, 1))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void reorderTabsByIds_shouldApplyGivenOrder() {
        User user = user(1L, "alice");
        PortalTab first = tab(1L, user, "First", 1);
        PortalTab second = tab(2L, user, "Second", 2);
        when(portalTabRepository.findAllByUserUsernameOrderBySortOrderAscIdAsc("alice"))
                .thenReturn(List.of(first, second));

        portalService.reorderTabsByIds("alice", List.of(2L, 1L));

        assertThat(second.getSortOrder()).isEqualTo(1);
        assertThat(first.getSortOrder()).isEqualTo(2);
    }

    @Test
    void reorderTabsByIds_shouldThrowWhenIdsMismatch() {
        User user = user(1L, "alice");
        PortalTab first = tab(1L, user, "First", 1);
        PortalTab second = tab(2L, user, "Second", 2);
        when(portalTabRepository.findAllByUserUsernameOrderBySortOrderAscIdAsc("alice"))
                .thenReturn(List.of(first, second));

        assertThatThrownBy(() -> portalService.reorderTabsByIds("alice", List.of(1L)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void reorderTabsByIds_shouldAllowBothEmpty() {
        when(portalTabRepository.findAllByUserUsernameOrderBySortOrderAscIdAsc("alice")).thenReturn(List.of());

        portalService.reorderTabsByIds("alice", List.of());
    }

    @Test
    void reorderTabsByIds_shouldThrowWhenRequestedIdsNull() {
        User user = user(1L, "alice");
        PortalTab first = tab(1L, user, "First", 1);
        when(portalTabRepository.findAllByUserUsernameOrderBySortOrderAscIdAsc("alice"))
                .thenReturn(List.of(first));

        assertThatThrownBy(() -> portalService.reorderTabsByIds("alice", null))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void reorderTabsByIds_shouldThrowWhenRequestedContainsDuplicateIds() {
        User user = user(1L, "alice");
        PortalTab first = tab(1L, user, "First", 1);
        PortalTab second = tab(2L, user, "Second", 2);
        when(portalTabRepository.findAllByUserUsernameOrderBySortOrderAscIdAsc("alice"))
                .thenReturn(List.of(first, second));

        assertThatThrownBy(() -> portalService.reorderTabsByIds("alice", List.of(1L, 1L)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void reorderTabsByIds_shouldThrowWhenExistingContainsDuplicateIds() {
        User user = user(1L, "alice");
        PortalTab first = tab(1L, user, "First", 1);
        PortalTab duplicated = tab(1L, user, "Duplicated", 2);
        when(portalTabRepository.findAllByUserUsernameOrderBySortOrderAscIdAsc("alice"))
                .thenReturn(List.of(first, duplicated));

        assertThatThrownBy(() -> portalService.reorderTabsByIds("alice", List.of(1L, 2L)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void reorderTabsByIds_shouldThrowWhenRequestedSetDoesNotMatch() {
        User user = user(1L, "alice");
        PortalTab first = tab(1L, user, "First", 1);
        PortalTab second = tab(2L, user, "Second", 2);
        when(portalTabRepository.findAllByUserUsernameOrderBySortOrderAscIdAsc("alice"))
                .thenReturn(List.of(first, second));

        assertThatThrownBy(() -> portalService.reorderTabsByIds("alice", List.of(1L, 3L)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void reorderCategoriesByIds_shouldApplyOrderWhenTabOwnedByUser() {
        User user = user(1L, "alice");
        PortalTab tab = tab(10L, user, "Tab", 1);
        PortalCategory first = category(1L, tab, "A", 1);
        PortalCategory second = category(2L, tab, "B", 2);
        when(portalTabRepository.findByIdAndUserUsername(10L, "alice")).thenReturn(Optional.of(tab));
        when(portalCategoryRepository.findAllByTabIdOrderBySortOrderAscIdAsc(10L)).thenReturn(List.of(first, second));

        portalService.reorderCategoriesByIds("alice", 10L, List.of(2L, 1L));

        assertThat(second.getSortOrder()).isEqualTo(1);
        assertThat(first.getSortOrder()).isEqualTo(2);
    }

    @Test
    void reorderCategoriesByIds_shouldThrowWhenTabNotFound() {
        when(portalTabRepository.findByIdAndUserUsername(10L, "alice")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> portalService.reorderCategoriesByIds("alice", 10L, List.of(1L)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void reorderLinksByIds_shouldApplyOrderWhenCategoryOwnedByUser() {
        User user = user(1L, "alice");
        PortalTab tab = tab(10L, user, "Tab", 1);
        PortalCategory category = category(20L, tab, "Cat", 1);
        PortalLink first = link(1L, category, "A", "https://a.com", "x", "#000", 1);
        PortalLink second = link(2L, category, "B", "https://b.com", "x", "#000", 2);
        when(portalCategoryRepository.findByIdAndTabUserUsername(20L, "alice")).thenReturn(Optional.of(category));
        when(portalLinkRepository.findAllByCategoryIdOrderBySortOrderAscIdAsc(20L)).thenReturn(List.of(first, second));

        portalService.reorderLinksByIds("alice", 20L, List.of(2L, 1L));

        assertThat(second.getSortOrder()).isEqualTo(1);
        assertThat(first.getSortOrder()).isEqualTo(2);
    }

    @Test
    void reorderLinksByIds_shouldThrowWhenCategoryNotFound() {
        when(portalCategoryRepository.findByIdAndTabUserUsername(20L, "alice")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> portalService.reorderLinksByIds("alice", 20L, List.of(1L)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void addCategorytoTab_shouldMoveCategoryToTargetTab() {
        User user = user(1L, "alice");
        PortalTab oldTab = tab(10L, user, "OldTab", 1);
        PortalTab newTab = tab(20L, user, "NewTab", 2);
        PortalCategory category = category(30L, oldTab, "Cat", 1);
        when(portalCategoryRepository.findByIdAndTabUserUsername(30L, "alice")).thenReturn(Optional.of(category));
        when(portalTabRepository.findByIdAndUserUsername(20L, "alice")).thenReturn(Optional.of(newTab));

        CategoryResponse result = portalService.addCategorytoTab("alice", 20L, 30L);

        assertThat(result.id()).isEqualTo(30L);
        assertThat(category.getTab().getId()).isEqualTo(20L);
    }

    @Test
    void addCategorytoTab_shouldThrowWhenCategoryMissing() {
        when(portalCategoryRepository.findByIdAndTabUserUsername(30L, "alice")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> portalService.addCategorytoTab("alice", 20L, 30L))
                .isInstanceOf(NotFoundException.class);
    }

    @SuppressWarnings("null")
    @Test
    void addCategorytoTab_shouldThrowWhenTabMissing() {
        User user = user(1L, "alice");
        PortalTab oldTab = tab(10L, user, "OldTab", 1);
        PortalCategory category = category(30L, oldTab, "Cat", 1);
        when(portalCategoryRepository.findByIdAndTabUserUsername(30L, "alice")).thenReturn(Optional.of(category));
        when(portalTabRepository.findByIdAndUserUsername(20L, "alice")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> portalService.addCategorytoTab("alice", 20L, 30L))
                .isInstanceOf(NotFoundException.class);
    }

    @SuppressWarnings("null")
    @Test
    void createLinkRequestBased_shouldPassNormalizedValuesToSave() {
        User user = user(1L, "alice");
        PortalTab tab = tab(10L, user, "Tab", 1);
        PortalCategory category = category(20L, tab, "Cat", 1);
        when(portalCategoryRepository.findByIdAndTabUserUsername(20L, "alice")).thenReturn(Optional.of(category));
        when(portalLinkRepository.existsByCategoryIdAndNameIgnoreCase(20L, "Site")).thenReturn(false);
        when(portalLinkRepository.existsByCategoryIdAndUrlIgnoreCase(20L, "https://site.com")).thenReturn(false);
        when(portalLinkRepository.findAllByCategoryIdOrderBySortOrderAscIdAsc(20L)).thenReturn(List.of());
        when(portalLinkRepository.save(any(PortalLink.class))).thenAnswer(invocation -> {
            PortalLink saved = invocation.getArgument(0, PortalLink.class);
            ReflectionTestUtils.setField(saved, "id", 99L);
            return saved;
        });

        portalService.createLink("alice", 20L, new LinkCreateRequest(" Site ", "site.com", "ico", "#fff", null));

        ArgumentCaptor<PortalLink> captor = ArgumentCaptor.forClass(PortalLink.class);
        verify(portalLinkRepository).save(captor.capture());
        PortalLink saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("Site");
        assertThat(saved.getUrl()).isEqualTo("https://site.com");
    }

    @SuppressWarnings("null")
    @Test
    void createLinkOverload_shouldUseDefaultIconAndColor() {
        User user = user(1L, "alice");
        PortalTab tab = tab(10L, user, "Tab", 1);
        PortalCategory category = category(20L, tab, "Cat", 1);
        when(portalCategoryRepository.findByIdAndTabUserUsername(20L, "alice")).thenReturn(Optional.of(category));
        when(portalLinkRepository.existsByCategoryIdAndNameIgnoreCase(20L, "Site")).thenReturn(false);
        when(portalLinkRepository.existsByCategoryIdAndUrlIgnoreCase(20L, "https://site.com")).thenReturn(false);
        when(portalLinkRepository.findAllByCategoryIdOrderBySortOrderAscIdAsc(20L)).thenReturn(List.of());
        when(portalLinkRepository.save(any(PortalLink.class))).thenAnswer(invocation -> {
            PortalLink saved = invocation.getArgument(0, PortalLink.class);
            ReflectionTestUtils.setField(saved, "id", 777L);
            return saved;
        });

        LinkResponse result = portalService.createLink("alice", 20L, "Site", "site.com");

        assertThat(result.icon()).isEqualTo("link");
        assertThat(result.iconColor()).isEqualTo("#89b4fa");
    }

    @SuppressWarnings("null")
    @Test
    void createLinkOverloadWithIcon_shouldRespectProvidedValues() {
        User user = user(1L, "alice");
        PortalTab tab = tab(10L, user, "Tab", 1);
        PortalCategory category = category(20L, tab, "Cat", 1);
        when(portalCategoryRepository.findByIdAndTabUserUsername(20L, "alice")).thenReturn(Optional.of(category));
        when(portalLinkRepository.existsByCategoryIdAndNameIgnoreCase(20L, "Site")).thenReturn(false);
        when(portalLinkRepository.existsByCategoryIdAndUrlIgnoreCase(20L, "https://site.com")).thenReturn(false);
        when(portalLinkRepository.findAllByCategoryIdOrderBySortOrderAscIdAsc(20L)).thenReturn(List.of());
        when(portalLinkRepository.save(any(PortalLink.class))).thenAnswer(invocation -> {
            PortalLink saved = invocation.getArgument(0, PortalLink.class);
            ReflectionTestUtils.setField(saved, "id", 888L);
            return saved;
        });

        LinkResponse result = portalService.createLink("alice", 20L, "Site", "site.com", "rocket", "#fff");

        assertThat(result.icon()).isEqualTo("rocket");
        assertThat(result.iconColor()).isEqualTo("#fff");
    }

    @Test
    void createCategory_shouldThrowWhenTabNotFound() {
        when(portalTabRepository.findByIdAndUserUsername(100L, "alice")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> portalService.createCategory("alice", 100L, new CategoryCreateRequest("News", List.of(), 1)))
                .isInstanceOf(NotFoundException.class);
    }

    @SuppressWarnings("null")
    @Test
    void createCategory_shouldThrowWhenNameNull() {
        User user = user(1L, "alice");
        PortalTab tab = tab(100L, user, "Tab", 1);
        when(portalTabRepository.findByIdAndUserUsername(100L, "alice")).thenReturn(Optional.of(tab));

        assertThatThrownBy(() -> portalService.createCategory("alice", 100L, new CategoryCreateRequest(null, List.of(), 1)))
                .isInstanceOf(BadRequestException.class);
    }

    private static User user(Long id, String username) {
        User user = Objects.requireNonNull(User.createUser(username, "ENC", Role.USER));
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private static PortalTab tab(Long id, User user, String name, int sortOrder) {
        PortalTab tab = Objects.requireNonNull(PortalTab.createTab(user, name, sortOrder));
        ReflectionTestUtils.setField(tab, "id", id);
        return tab;
    }

    private static PortalCategory category(Long id, PortalTab tab, String name, int sortOrder) {
        PortalCategory category = Objects.requireNonNull(PortalCategory.create(tab, name, sortOrder));
        ReflectionTestUtils.setField(category, "id", id);
        return category;
    }

    private static PortalLink link(
            Long id,
            PortalCategory category,
            String name,
            String url,
            String icon,
            String iconColor,
            int sortOrder
    ) {
        PortalLink link = Objects.requireNonNull(PortalLink.create(category, name, url, icon, iconColor, sortOrder));
        ReflectionTestUtils.setField(link, "id", id);
        return link;
    }

    private static byte[] pngBytes() {
        return new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    }
}
