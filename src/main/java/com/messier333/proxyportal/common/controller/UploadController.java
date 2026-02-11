package com.messier333.proxyportal.common.controller;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;

import com.messier333.proxyportal.common.util.ImageType;

@RestController
public class UploadController {
    @Value("${app.upload.root:uploads}")
    private String uploadRoot;

    @GetMapping("/uploads/**")
    public ResponseEntity<Resource> serveUpload(Authentication auth, HttpServletRequest request) throws IOException {
        if (auth == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String relativePath = extractRelativePath(request);
        if (relativePath == null || relativePath.isBlank()) {
            return ResponseEntity.notFound().build();
        }

        String safeUsername = sanitizeUsername(auth.getName());
        if (safeUsername.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String expectedPrefix = "portal-tabs/" + safeUsername + "/";
        if (!relativePath.startsWith(expectedPrefix)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Path root = Paths.get(uploadRoot).toAbsolutePath().normalize();
        if (root.getNameCount() == 0) {
            throw new IllegalStateException("upload root must not be filesystem root");
        }

        Path target = root.resolve(relativePath).normalize();
        if (!target.startsWith(root)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (!Files.exists(target) || Files.isDirectory(target)) {
            return ResponseEntity.notFound().build();
        }
        Header header = readHeader(target);
        ImageType detected = ImageType.detect(header.bytes(), header.length());
        String extension = ImageType.extractExtension(target.getFileName().toString());
        if (detected == null) {
            return ResponseEntity.notFound().build();
        }
        if (extension != null && !detected.matchesExtension(extension)) {
            return ResponseEntity.notFound().build();
        }

        MediaType mediaType = MediaType.parseMediaType(detected.mediaType());
        Resource resource = new UrlResource(target.toUri());

        return ResponseEntity.ok()
                .contentType(mediaType)
                .cacheControl(CacheControl.noCache())
                .header("X-Content-Type-Options", "nosniff")
                .body(resource);
    }

    private String extractRelativePath(HttpServletRequest request) {
        String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        String bestMatchPattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (path == null || bestMatchPattern == null) {
            return null;
        }
        return new AntPathMatcher().extractPathWithinPattern(bestMatchPattern, path);
    }

    private Header readHeader(Path target) throws IOException {
        byte[] header = new byte[ImageType.maxHeaderLength()];
        int read;
        try (InputStream input = Files.newInputStream(target)) {
            read = input.read(header);
        }
        if (read < 0) {
            read = 0;
        }
        return new Header(header, read);
    }

    private String sanitizeUsername(String username) {
        if (username == null || username.isBlank()) {
            return "";
        }
        return username.replaceAll("[^a-zA-Z0-9_-]", "_");
    }

    private record Header(byte[] bytes, int length) {
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof Header(byte[] otherBytes, int otherLength))) {
                return false;
            }
            return length == otherLength && Arrays.equals(bytes, otherBytes);
        }

        @Override
        public int hashCode() {
            int result = Integer.hashCode(length);
            result = 31 * result + Arrays.hashCode(bytes);
            return result;
        }

        @Override
        public String toString() {
            return "Header[length=" + length + ", bytes=" + Arrays.toString(bytes) + "]";
        }
    }
}
