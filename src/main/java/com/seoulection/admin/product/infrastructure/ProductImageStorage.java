package com.seoulection.admin.product.infrastructure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.core.sync.RequestBody;
import java.io.IOException;
import java.time.Duration;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;

/** crawlers/thumbnails/s3_uploader.py의 버킷·prefix·CloudFront URL 규칙을 Java 런타임에서 공유한다. */
@Service
public class ProductImageStorage {
    private final String bucket, prefix, cloudfront, region, roleArn;
    public ProductImageStorage(String bucket, String prefix, String cloudfront, String region) {
        this(bucket, prefix, cloudfront, region,
                "arn:aws:iam::091974775936:role/seoulection-product-image-s3-role");
    }
    @Autowired
    public ProductImageStorage(@Value("${S3_BUCKET:}") String bucket,
            @Value("${S3_PREFIX:product-pictures}") String prefix,
            @Value("${CLOUDFRONT_DOMAIN:}") String cloudfront,
            @Value("${AWS_REGION:}") String region,
            @Value("${AWS_ROLE_ARN:arn:aws:iam::091974775936:role/seoulection-product-image-s3-role}") String roleArn) {
        this.bucket = bucket; this.prefix = prefix.replaceAll("^/+|/+$", "");
        this.cloudfront = cloudfront.trim().replaceAll("/+$", ""); this.region = region; this.roleArn = roleArn;
    }
    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("제품 사진을 선택해 주세요.");
        if (file.getSize() > 10 * 1024 * 1024) throw new IllegalArgumentException("제품 사진은 10MB 이하여야 합니다.");
        try (var input = ImageIO.createImageInputStream(file.getInputStream())) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IllegalArgumentException("JPG 또는 PNG 이미지를 선택해 주세요.");
            var reader = readers.next();
            try {
                reader.setInput(input);
                String format = reader.getFormatName();
                if (!("JPEG".equalsIgnoreCase(format) || "PNG".equalsIgnoreCase(format)))
                    throw new IllegalArgumentException("JPG 또는 PNG 이미지를 선택해 주세요.");
                if ((long) reader.getWidth(0) * reader.getHeight(0) > 40_000_000)
                    throw new IllegalArgumentException("이미지는 4천만 픽셀 이하여야 합니다.");
                if (reader.read(0) == null) throw new IllegalArgumentException("이미지 파일을 확인해 주세요.");
            } finally { reader.dispose(); }
        } catch (IOException e) { throw new IllegalArgumentException("이미지 파일을 읽을 수 없습니다.", e); }
    }
    String objectKey(String asin) {
        if (asin == null || !asin.matches("[A-Za-z0-9]+"))
            throw new IllegalArgumentException("이미지 저장에 사용할 ASIN을 확인해 주세요.");
        return (prefix.isEmpty() ? "" : prefix + "/") + asin + ".jpg";
    }
    byte[] jpegBytes(MultipartFile file) throws IOException {
        try (var input = file.getInputStream(); var output = new ByteArrayOutputStream()) {
            BufferedImage source = ImageIO.read(input);
            if (source == null) throw new IOException("이미지를 읽을 수 없습니다.");
            BufferedImage rgb = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
            var graphics = rgb.createGraphics();
            try {
                graphics.setColor(Color.WHITE);
                graphics.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
                graphics.drawImage(source, 0, 0, null);
            } finally { graphics.dispose(); }
            if (!ImageIO.write(rgb, "jpg", output)) throw new IOException("JPG 변환에 실패했습니다.");
            return output.toByteArray();
        }
    }
    public String upload(MultipartFile file, String asin) {
        validate(file);
        if (bucket.isBlank()) throw new IllegalStateException("S3_BUCKET 설정이 필요합니다.");
        try {
            byte[] bytes = jpegBytes(file);
            String key = objectKey(asin);
            var builder = S3Client.builder().overrideConfiguration(c -> c.apiCallTimeout(Duration.ofSeconds(45))
                    .apiCallAttemptTimeout(Duration.ofSeconds(20)));
            if (!region.isBlank()) builder.region(Region.of(region));
            // 환경변수의 원본 자격증명으로 이미지 업로드 Role을 AssumeRole한다.
            try (StsClient sts = StsClient.builder().region(Region.of(region.isBlank() ? "us-east-1" : region)).build();
                 StsAssumeRoleCredentialsProvider credentials = StsAssumeRoleCredentialsProvider.builder()
                         .stsClient(sts)
                         .refreshRequest(AssumeRoleRequest.builder().roleArn(roleArn).roleSessionName("seoulection-admin-image-upload").build())
                         .build();
                 S3Client s3 = builder.credentialsProvider(credentials).build()) {
                s3.putObject(b -> b.bucket(bucket).key(key).contentType("image/jpeg"), RequestBody.fromBytes(bytes));
                return cloudfront.isEmpty()
                        ? s3.utilities().getUrl(b -> b.bucket(bucket).key(key)).toExternalForm()
                        : "https://" + cloudfront + "/" + key;
            }
        } catch (IOException e) { throw new IllegalStateException("이미지 업로드에 실패했습니다.", e); }
    }
}
