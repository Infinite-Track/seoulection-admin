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
}
