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
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;

/**
 * 상품 사진을 저장하고 공개 URL 을 돌려준다. 저장 위치는 두 가지다.
 *
 * <ul>
 *   <li><b>로컬 디스크</b> — {@code IMAGE_LOCAL_DIR} 이 설정되면 {@code <디렉터리>/<prefix>/<ASIN>.jpg} 에 쓰고
 *       {@code IMAGE_PUBLIC_BASE_URL/<prefix>/<ASIN>.jpg} 를 돌려준다. 2026-11 Oracle 이전 뒤 운영은 이 방식이다 —
 *       AWS 계정이 없어져 S3·CloudFront 를 쓸 수 없고, VM 1 의 Caddy 가 같은 폴더를 {@code img.<도메인>} 으로 서비스한다.</li>
 *   <li><b>S3</b> — {@code IMAGE_LOCAL_DIR} 이 비어 있으면 예전처럼 crawlers/thumbnails/s3_uploader.py 의
 *       버킷·prefix·CloudFront URL 규칙으로 올린다.</li>
 * </ul>
 */
@Service
public class ProductImageStorage {
    private final String bucket, prefix, cloudfront, region, roleArn, localDir, publicBaseUrl;
    public ProductImageStorage(String bucket, String prefix, String cloudfront, String region) {
        this(bucket, prefix, cloudfront, region,
                "arn:aws:iam::091974775936:role/seoulection-product-image-s3-role", "", "");
    }
    @Autowired
    public ProductImageStorage(@Value("${S3_BUCKET:}") String bucket,
            @Value("${S3_PREFIX:product-pictures}") String prefix,
            @Value("${CLOUDFRONT_DOMAIN:}") String cloudfront,
            @Value("${AWS_REGION:}") String region,
            @Value("${AWS_ROLE_ARN:arn:aws:iam::091974775936:role/seoulection-product-image-s3-role}") String roleArn,
            @Value("${IMAGE_LOCAL_DIR:}") String localDir,
            @Value("${IMAGE_PUBLIC_BASE_URL:}") String publicBaseUrl) {
        this.bucket = bucket; this.prefix = prefix.replaceAll("^/+|/+$", "");
        this.cloudfront = cloudfront.trim().replaceAll("/+$", ""); this.region = region; this.roleArn = roleArn;
        this.localDir = localDir.trim(); this.publicBaseUrl = publicBaseUrl.trim().replaceAll("/+$", "");
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
        if (!localDir.isEmpty()) return uploadToLocalDir(file, asin);
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

    /**
     * 로컬 디스크에 쓴다. 같은 폴더의 임시 파일에 다 쓴 뒤 원래 이름으로 한 번에 바꾼다 — 쓰는 도중에 Caddy 가 이 파일을
     * 내보내도 반쯤 쓰인 이미지가 나가지 않는다. 같은 ASIN 으로 다시 올리면 덮어쓴다(S3 의 putObject 와 같다).
     */
    String uploadToLocalDir(MultipartFile file, String asin) {
        if (publicBaseUrl.isEmpty()) throw new IllegalStateException("IMAGE_PUBLIC_BASE_URL 설정이 필요합니다.");
        String key = objectKey(asin);
        Path target = Path.of(localDir).resolve(key).normalize();
        if (!target.startsWith(Path.of(localDir).normalize()))
            throw new IllegalArgumentException("이미지 저장 경로를 확인해 주세요.");
        try {
            byte[] bytes = jpegBytes(file);
            Files.createDirectories(target.getParent());
            Path temp = Files.createTempFile(target.getParent(), ".upload-", ".tmp");
            try {
                Files.write(temp, bytes);
                // Caddy·백업(다른 사용자)이 읽을 수 있게 644 로 둔다. createTempFile 은 600 으로 만든다.
                try { Files.setPosixFilePermissions(temp, java.nio.file.attribute.PosixFilePermissions.fromString("rw-r--r--")); }
                catch (UnsupportedOperationException ignored) { /* POSIX 가 아닌 파일 시스템(로컬 윈도우 등) */ }
                try { Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
                catch (AtomicMoveNotSupportedException e) { Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING); }
            } finally { Files.deleteIfExists(temp); }
            return publicBaseUrl + "/" + key;
        } catch (IOException e) { throw new IllegalStateException("이미지 업로드에 실패했습니다.", e); }
    }
}
