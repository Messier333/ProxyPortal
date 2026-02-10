package com.messier333.proxyportal.common.util;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

public enum ImageType {
    PNG("image/png", "png", Set.of("png")) {
        @Override
        boolean matches(byte[] header, int length) {
            return length >= 8
                    && header[0] == (byte) 0x89
                    && header[1] == 0x50
                    && header[2] == 0x4E
                    && header[3] == 0x47
                    && header[4] == 0x0D
                    && header[5] == 0x0A
                    && header[6] == 0x1A
                    && header[7] == 0x0A;
        }
    },
    JPEG("image/jpeg", "jpg", Set.of("jpg", "jpeg")) {
        @Override
        boolean matches(byte[] header, int length) {
            return length >= 3
                    && header[0] == (byte) 0xFF
                    && header[1] == (byte) 0xD8
                    && header[2] == (byte) 0xFF;
        }
    },
    GIF("image/gif", "gif", Set.of("gif")) {
        @Override
        boolean matches(byte[] header, int length) {
            return length >= 6
                    && header[0] == 'G'
                    && header[1] == 'I'
                    && header[2] == 'F'
                    && header[3] == '8'
                    && (header[4] == '7' || header[4] == '9')
                    && header[5] == 'a';
        }
    },
    WEBP("image/webp", "webp", Set.of("webp")) {
        @Override
        boolean matches(byte[] header, int length) {
            return length >= 12
                    && header[0] == 'R'
                    && header[1] == 'I'
                    && header[2] == 'F'
                    && header[3] == 'F'
                    && header[8] == 'W'
                    && header[9] == 'E'
                    && header[10] == 'B'
                    && header[11] == 'P';
        }
    },
    BMP("image/bmp", "bmp", Set.of("bmp")) {
        @Override
        boolean matches(byte[] header, int length) {
            return length >= 2
                    && header[0] == 'B'
                    && header[1] == 'M';
        }
    },
    ICO("image/x-icon", "ico", Set.of("ico")) {
        @Override
        boolean matches(byte[] header, int length) {
            return length >= 4
                    && header[0] == 0x00
                    && header[1] == 0x00
                    && header[2] == 0x01
                    && header[3] == 0x00;
        }
    };

    private static final int MAX_HEADER_LENGTH = 12;
    private static final Map<String, ImageType> EXTENSION_MAP = Map.ofEntries(
            Map.entry("png", PNG),
            Map.entry("jpg", JPEG),
            Map.entry("jpeg", JPEG),
            Map.entry("gif", GIF),
            Map.entry("webp", WEBP),
            Map.entry("bmp", BMP),
            Map.entry("ico", ICO)
    );

    private final String mediaType;
    private final String defaultExtension;
    private final Set<String> extensions;

    ImageType(String mediaType, String defaultExtension, Set<String> extensions) {
        this.mediaType = mediaType;
        this.defaultExtension = defaultExtension;
        this.extensions = extensions;
    }

    abstract boolean matches(byte[] header, int length);

    public String mediaType() {
        return mediaType;
    }

    public String defaultExtension() {
        return defaultExtension;
    }

    public boolean matchesExtension(String extension) {
        return extension != null && extensions.contains(extension);
    }

    public static int maxHeaderLength() {
        return MAX_HEADER_LENGTH;
    }

    public static ImageType detect(byte[] header, int length) {
        for (ImageType type : values()) {
            if (type.matches(header, length)) {
                return type;
            }
        }
        return null;
    }

    public static ImageType fromExtension(String extension) {
        if (extension == null || extension.isBlank()) {
            return null;
        }
        return EXTENSION_MAP.get(extension.toLowerCase(Locale.ROOT));
    }

    public static String extractExtension(String filename) {
        if (filename == null || filename.isBlank()) {
            return null;
        }
        int dot = filename.lastIndexOf('.');
        if (dot <= 0 || dot == filename.length() - 1) {
            return null;
        }
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
