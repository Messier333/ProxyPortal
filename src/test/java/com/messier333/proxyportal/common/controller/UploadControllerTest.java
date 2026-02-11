package com.messier333.proxyportal.common.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.HandlerMapping;

class UploadControllerTest {

    @TempDir
    Path tempDir;

    private UploadController controller;

    @BeforeEach
    void setUp() {
        controller = new UploadController();
        ReflectionTestUtils.setField(controller, "uploadRoot", tempDir.toString());
    }

    @Test
    void serveUpload_shouldReturnUnauthorizedWhenAuthIsNull() throws Exception {
        ResponseEntity<Resource> response = controller.serveUpload(null, requestFor("portal-tabs/alice/a.png"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void serveUpload_shouldReturnNotFoundWhenPathAttributesAreMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/uploads/portal-tabs/alice/a.png");

        ResponseEntity<Resource> response = controller.serveUpload(auth("alice"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void serveUpload_shouldReturnNotFoundWhenRelativePathIsBlank() throws Exception {
        ResponseEntity<Resource> response = controller.serveUpload(auth("alice"), requestFor(""));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void serveUpload_shouldReturnUnauthorizedWhenSanitizedUsernameIsBlank() throws Exception {
        ResponseEntity<Resource> response = controller.serveUpload(auth("   "), requestFor("portal-tabs/alice/a.png"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void serveUpload_shouldReturnForbiddenWhenPathPrefixDoesNotMatchUser() throws Exception {
        ResponseEntity<Resource> response = controller.serveUpload(auth("alice"), requestFor("portal-tabs/bob/a.png"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void serveUpload_shouldThrowWhenUploadRootIsFilesystemRoot() {
        UploadController rootController = new UploadController();
        ReflectionTestUtils.setField(rootController, "uploadRoot", "/");
        Authentication authentication = auth("alice");
        MockHttpServletRequest request = requestFor("portal-tabs/alice/a.png");

        assertThatThrownBy(() -> rootController.serveUpload(authentication, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("upload root must not be filesystem root");
    }

    @Test
    void serveUpload_shouldReturnForbiddenWhenNormalizedTargetEscapesRoot() throws Exception {
        ResponseEntity<Resource> response =
                controller.serveUpload(auth("alice"), requestFor("portal-tabs/alice/../../../outside.png"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void serveUpload_shouldReturnNotFoundWhenTargetDoesNotExist() throws Exception {
        ResponseEntity<Resource> response =
                controller.serveUpload(auth("alice"), requestFor("portal-tabs/alice/missing.png"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void serveUpload_shouldReturnNotFoundWhenTargetIsDirectory() throws Exception {
        Files.createDirectories(tempDir.resolve("portal-tabs/alice/folder"));

        ResponseEntity<Resource> response = controller.serveUpload(auth("alice"), requestFor("portal-tabs/alice/folder"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void serveUpload_shouldReturnNotFoundWhenImageHeaderCannotBeDetected() throws Exception {
        writeFile("portal-tabs/alice/unknown.png", new byte[] {1, 2, 3, 4, 5});

        ResponseEntity<Resource> response = controller.serveUpload(auth("alice"), requestFor("portal-tabs/alice/unknown.png"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void serveUpload_shouldReturnNotFoundWhenDetectedTypeDoesNotMatchExtension() throws Exception {
        writeFile("portal-tabs/alice/mismatch.jpg", pngBytes());

        ResponseEntity<Resource> response = controller.serveUpload(auth("alice"), requestFor("portal-tabs/alice/mismatch.jpg"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void serveUpload_shouldReturnOkWhenFileBelongsToUserAndIsValidImage() throws Exception {
        writeFile("portal-tabs/ali_ce/ok.png", pngBytes());

        ResponseEntity<Resource> response = controller.serveUpload(auth("ali.ce"), requestFor("portal-tabs/ali_ce/ok.png"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.IMAGE_PNG);
        assertThat(response.getHeaders().getFirst("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(response.getHeaders().getCacheControl()).isEqualTo("no-cache");
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().exists()).isTrue();
    }

    @Test
    void headerRecord_shouldSupportEqualsHashCodeAndToString() throws Exception {
        Class<?> headerClass = Class.forName("com.messier333.proxyportal.common.controller.UploadController$Header");
        Constructor<?> constructor = headerClass.getDeclaredConstructor(byte[].class, int.class);
        constructor.setAccessible(true);

        Object header = constructor.newInstance(new byte[] {1, 2, 3}, 3);
        Object same = constructor.newInstance(new byte[] {1, 2, 3}, 3);
        Object different = constructor.newInstance(new byte[] {9, 9, 9}, 3);

        assertThat(header)
                .isEqualTo(header)
                .isEqualTo(same)
                .isNotEqualTo(different)
                .isNotEqualTo("other")
                .hasSameHashCodeAs(same);
        assertThat(header.toString()).contains("Header[length=3");
    }

    private Authentication auth(String username) {
        return new TestingAuthenticationToken(username, "pw", "ROLE_USER");
    }

    private MockHttpServletRequest requestFor(String relativePath) {
        String path = "/uploads/" + relativePath;
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, path);
        request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/uploads/**");
        return request;
    }

    private void writeFile(String relativePath, byte[] bytes) throws Exception {
        Path file = tempDir.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.write(file, bytes);
    }

    private byte[] pngBytes() {
        return new byte[] {
                (byte) 0x89, 0x50, 0x4E, 0x47,
                0x0D, 0x0A, 0x1A, 0x0A,
                0x00
        };
    }
}
