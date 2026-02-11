package com.messier333.proxyportal.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

class ImageTypeTest {

    @Test
    void pngMatches_shouldCoverAllDecisions() {
        byte[] header = png();

        assertThat(ImageType.PNG.matches(header, 7)).isFalse();
        assertThat(ImageType.PNG.matches(mutated(header, 0), 8)).isFalse();
        assertThat(ImageType.PNG.matches(mutated(header, 1), 8)).isFalse();
        assertThat(ImageType.PNG.matches(mutated(header, 2), 8)).isFalse();
        assertThat(ImageType.PNG.matches(mutated(header, 3), 8)).isFalse();
        assertThat(ImageType.PNG.matches(mutated(header, 4), 8)).isFalse();
        assertThat(ImageType.PNG.matches(mutated(header, 5), 8)).isFalse();
        assertThat(ImageType.PNG.matches(mutated(header, 6), 8)).isFalse();
        assertThat(ImageType.PNG.matches(mutated(header, 7), 8)).isFalse();
        assertThat(ImageType.PNG.matches(header, 8)).isTrue();
    }

    @Test
    void jpegMatches_shouldCoverAllDecisions() {
        byte[] header = jpeg();

        assertThat(ImageType.JPEG.matches(header, 2)).isFalse();
        assertThat(ImageType.JPEG.matches(mutated(header, 0), 3)).isFalse();
        assertThat(ImageType.JPEG.matches(mutated(header, 1), 3)).isFalse();
        assertThat(ImageType.JPEG.matches(mutated(header, 2), 3)).isFalse();
        assertThat(ImageType.JPEG.matches(header, 3)).isTrue();
    }

    @Test
    void gifMatches_shouldCoverAllDecisions() {
        byte[] header = gif('7');
        byte[] gifWith9 = gif('9');
        byte[] gifWithInvalidVersion = gif('8');

        assertThat(ImageType.GIF.matches(header, 5)).isFalse();
        assertThat(ImageType.GIF.matches(mutated(header, 0), 6)).isFalse();
        assertThat(ImageType.GIF.matches(mutated(header, 1), 6)).isFalse();
        assertThat(ImageType.GIF.matches(mutated(header, 2), 6)).isFalse();
        assertThat(ImageType.GIF.matches(mutated(header, 3), 6)).isFalse();
        assertThat(ImageType.GIF.matches(gifWithInvalidVersion, 6)).isFalse();
        assertThat(ImageType.GIF.matches(mutated(header, 5), 6)).isFalse();
        assertThat(ImageType.GIF.matches(header, 6)).isTrue();
        assertThat(ImageType.GIF.matches(gifWith9, 6)).isTrue();
    }

    @Test
    void webpMatches_shouldCoverAllDecisions() {
        byte[] header = webp();

        assertThat(ImageType.WEBP.matches(header, 11)).isFalse();
        assertThat(ImageType.WEBP.matches(mutated(header, 0), 12)).isFalse();
        assertThat(ImageType.WEBP.matches(mutated(header, 1), 12)).isFalse();
        assertThat(ImageType.WEBP.matches(mutated(header, 2), 12)).isFalse();
        assertThat(ImageType.WEBP.matches(mutated(header, 3), 12)).isFalse();
        assertThat(ImageType.WEBP.matches(mutated(header, 8), 12)).isFalse();
        assertThat(ImageType.WEBP.matches(mutated(header, 9), 12)).isFalse();
        assertThat(ImageType.WEBP.matches(mutated(header, 10), 12)).isFalse();
        assertThat(ImageType.WEBP.matches(mutated(header, 11), 12)).isFalse();
        assertThat(ImageType.WEBP.matches(header, 12)).isTrue();
    }

    @Test
    void bmpMatches_shouldCoverAllDecisions() {
        byte[] header = bmp();

        assertThat(ImageType.BMP.matches(header, 1)).isFalse();
        assertThat(ImageType.BMP.matches(mutated(header, 0), 2)).isFalse();
        assertThat(ImageType.BMP.matches(mutated(header, 1), 2)).isFalse();
        assertThat(ImageType.BMP.matches(header, 2)).isTrue();
    }

    @Test
    void icoMatches_shouldCoverAllDecisions() {
        byte[] header = ico();

        assertThat(ImageType.ICO.matches(header, 3)).isFalse();
        assertThat(ImageType.ICO.matches(mutated(header, 0), 4)).isFalse();
        assertThat(ImageType.ICO.matches(mutated(header, 1), 4)).isFalse();
        assertThat(ImageType.ICO.matches(mutated(header, 2), 4)).isFalse();
        assertThat(ImageType.ICO.matches(mutated(header, 3), 4)).isFalse();
        assertThat(ImageType.ICO.matches(header, 4)).isTrue();
    }

    @Test
    void detect_shouldReturnMatchedTypeOrNull() {
        assertThat(ImageType.detect(png(), 8)).isEqualTo(ImageType.PNG);
        assertThat(ImageType.detect(jpeg(), 3)).isEqualTo(ImageType.JPEG);
        assertThat(ImageType.detect(gif('7'), 6)).isEqualTo(ImageType.GIF);
        assertThat(ImageType.detect(webp(), 12)).isEqualTo(ImageType.WEBP);
        assertThat(ImageType.detect(bmp(), 2)).isEqualTo(ImageType.BMP);
        assertThat(ImageType.detect(ico(), 4)).isEqualTo(ImageType.ICO);
        assertThat(ImageType.detect(new byte[] {1, 2, 3, 4}, 4)).isNull();
    }

    @Test
    void extensionMethods_shouldHandleAllBranches() {
        assertThat(ImageType.maxHeaderLength()).isEqualTo(12);
        assertThat(ImageType.PNG.mediaType()).isEqualTo("image/png");
        assertThat(ImageType.PNG.defaultExtension()).isEqualTo("png");

        assertThat(ImageType.PNG.matchesExtension(null)).isFalse();
        assertThat(ImageType.PNG.matchesExtension("jpg")).isFalse();
        assertThat(ImageType.PNG.matchesExtension("png")).isTrue();

        assertThat(ImageType.fromExtension(null)).isNull();
        assertThat(ImageType.fromExtension("")).isNull();
        assertThat(ImageType.fromExtension("   ")).isNull();
        assertThat(ImageType.fromExtension("PnG")).isEqualTo(ImageType.PNG);
        assertThat(ImageType.fromExtension("unknown")).isNull();

        assertThat(ImageType.extractExtension(null)).isNull();
        assertThat(ImageType.extractExtension("")).isNull();
        assertThat(ImageType.extractExtension("  ")).isNull();
        assertThat(ImageType.extractExtension("nodot")).isNull();
        assertThat(ImageType.extractExtension(".hidden")).isNull();
        assertThat(ImageType.extractExtension("name.")).isNull();
        assertThat(ImageType.extractExtension("photo.JPEG")).isEqualTo("jpeg");
    }

    private byte[] mutated(byte[] original, int index) {
        byte[] copy = Arrays.copyOf(original, original.length);
        copy[index] = (byte) (copy[index] + 1);
        return copy;
    }

    private byte[] png() {
        return new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    }

    private byte[] jpeg() {
        return new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    }

    private byte[] gif(char versionDigit) {
        return new byte[] {'G', 'I', 'F', '8', (byte) versionDigit, 'a'};
    }

    private byte[] webp() {
        return new byte[] {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'};
    }

    private byte[] bmp() {
        return new byte[] {'B', 'M'};
    }

    private byte[] ico() {
        return new byte[] {0x00, 0x00, 0x01, 0x00};
    }
}
