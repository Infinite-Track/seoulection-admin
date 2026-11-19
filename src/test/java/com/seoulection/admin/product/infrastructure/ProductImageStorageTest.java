package com.seoulection.admin.product.infrastructure;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import static org.assertj.core.api.Assertions.*;
class ProductImageStorageTest {
    final ProductImageStorage storage = new ProductImageStorage("", "product-pictures", "", "");
    @Test void acceptsRealPng() throws Exception {
        var out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2,2,BufferedImage.TYPE_INT_RGB), "png", out);
        assertThatCode(() -> storage.validate(new MockMultipartFile("image","test.png","image/png",out.toByteArray()))).doesNotThrowAnyException();
    }
    @Test void rejectsDisguisedImageAndMissingImage() {
        assertThatThrownBy(() -> storage.validate(new MockMultipartFile("image","test.png","image/png","<script>".getBytes()))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> storage.validate(null)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void convertsTransparentPngToJpegAndUsesAsinPath() throws Exception {
        var out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2,2,BufferedImage.TYPE_INT_ARGB), "png", out);
        byte[] jpg = storage.jpegBytes(new MockMultipartFile("image","test.png","image/png",out.toByteArray()));
        assertThat(jpg[0]).isEqualTo((byte) 0xff);
        assertThat(jpg[1]).isEqualTo((byte) 0xd8);
        assertThat(ImageIO.read(new java.io.ByteArrayInputStream(jpg)).getRGB(0,0)).isEqualTo(java.awt.Color.WHITE.getRGB());
        assertThat(storage.objectKey("B001")).isEqualTo("product-pictures/B001.jpg");
        assertThatThrownBy(() -> storage.objectKey("../bad")).isInstanceOf(IllegalArgumentException.class);
    }

    private static byte[] png(int w, int h) throws Exception {
        var out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB), "png", out);
        return out.toByteArray();
    }

    private static ProductImageStorage local(java.nio.file.Path dir, String baseUrl) {
        return new ProductImageStorage("", "product-pictures", "", "", "unused-role", dir.toString(), baseUrl);
    }

    @Test void localDirWritesJpegUnderPrefixAndReturnsPublicUrl(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir) throws Exception {
        // 2026-11 Oracle 이전: S3 대신 VM 1 디스크(/srv/images)에 쓰고 Caddy img.<도메인> 주소를 돌려준다.
        var storage = local(dir, "https://img.myseoulection.com/");
        String url = storage.upload(new MockMultipartFile("image", "p.png", "image/png", png(3, 3)), "B0TEST01");

        assertThat(url).isEqualTo("https://img.myseoulection.com/product-pictures/B0TEST01.jpg");
        var saved = dir.resolve("product-pictures/B0TEST01.jpg");
        byte[] bytes = java.nio.file.Files.readAllBytes(saved);
        assertThat(bytes[0]).isEqualTo((byte) 0xff);
        assertThat(bytes[1]).isEqualTo((byte) 0xd8);
        // 임시 파일이 남지 않는다.
        try (var files = java.nio.file.Files.list(saved.getParent())) {
            assertThat(files.map(f -> f.getFileName().toString())).containsExactly("B0TEST01.jpg");
        }
        // Caddy·백업이 다른 사용자로 읽으므로 644 여야 한다.
        assertThat(java.nio.file.attribute.PosixFilePermissions.toString(java.nio.file.Files.getPosixFilePermissions(saved)))
                .isEqualTo("rw-r--r--");
    }

    @Test void localDirOverwritesSameAsin(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir) throws Exception {
        var storage = local(dir, "https://img.myseoulection.com");
        storage.upload(new MockMultipartFile("image", "a.png", "image/png", png(2, 2)), "B0TEST02");
        storage.upload(new MockMultipartFile("image", "b.png", "image/png", png(5, 4)), "B0TEST02");

        var img = ImageIO.read(dir.resolve("product-pictures/B0TEST02.jpg").toFile());
        assertThat(img.getWidth()).isEqualTo(5);
        assertThat(img.getHeight()).isEqualTo(4);
    }

    @Test void localDirRejectsBadAsinAndMissingBaseUrl(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir) throws Exception {
        var file = new MockMultipartFile("image", "p.png", "image/png", png(2, 2));
        assertThatThrownBy(() -> local(dir, "https://img.myseoulection.com").upload(file, "../etc"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> local(dir, "").upload(file, "B0TEST03"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("IMAGE_PUBLIC_BASE_URL");
    }

    @Test void withoutLocalDirKeepsS3PathAndNeedsBucket() throws Exception {
        // IMAGE_LOCAL_DIR 이 비면 예전 S3 방식 그대로다 — 버킷이 없으면 업로드 전에 멈춘다.
        assertThatThrownBy(() -> storage.upload(new MockMultipartFile("image", "p.png", "image/png", png(2, 2)), "B0TEST04"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("S3_BUCKET");
    }
}
