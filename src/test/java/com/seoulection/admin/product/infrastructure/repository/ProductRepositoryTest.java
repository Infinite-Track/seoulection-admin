package com.seoulection.admin.product.infrastructure.repository;

import com.seoulection.admin.product.domain.entity.Product;
import com.seoulection.admin.product.domain.repository.ProductRepository;
import com.seoulection.admin.product.infrastructure.document.ProductDocument;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.seoulection.admin.TestcontainersConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.math.BigDecimal;

import static com.mongodb.client.model.Filters.eq;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ProductRepositoryTest {

    @Autowired
    ProductRepository productRepository;

    @Autowired
    MongoTemplate mongoTemplate;

    @BeforeEach
    void setUp() {
        mongoTemplate.remove(new Query(), ProductDocument.class);
    }

    @Test
    @DisplayName("제품은 기본값과 PENDING 상태로 products 컬렉션에 저장한다")
    void savePendingProduct() {
        productRepository.insert(Product.pending("시카 세럼", "서울렉션", "sunscreens"));

        Document stored = mongoTemplate.getCollection("products")
                .find(eq("name", "시카 세럼"))
                .first();

        assertThat(stored).isNotNull();
        assertThat(stored.getString("brand")).isEqualTo("서울렉션");
        assertThat(stored.getString("category")).isEqualTo("sunscreens");
        assertThat(stored.getLong("mention_count")).isZero();
        assertThat(stored.get("ad_ratio", Decimal128.class).bigDecimalValue())
                .isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(stored.get("ingredients")).isNull();
        assertThat(stored.get("inciapi_raw_data")).isNull();
        assertThat(stored.get("analyzed_at")).isNull();
        assertThat(stored.getString("status")).isEqualTo("PENDING");
    }
    @Test
    void readsIncompleteProductAndKeywordAnalysisStatus() {
        mongoTemplate.getCollection("products").insertOne(new Document("_id", "incomplete")
                .append("asin", "B001").append("status", "NEED_PRODUCT_INFO")
                .append("mention_count", 3L).append("ad_ratio", 0.5));
        var product = productRepository.findById("incomplete");
        assertThat(product.name()).isNull();
        assertThat(product.brand()).isNull();
        assertThat(product.category()).isNull();
        assertThat(product.status()).isEqualTo(com.seoulection.admin.product.domain.enums.ProductStatus.NEED_PRODUCT_INFO);
        assertThat(productRepository.countByStatus(null)).containsEntry(product.status(), 1L);
        var analyzed = Product.pending("Product", "Brand", "toners").toBuilder()
                .status(com.seoulection.admin.product.domain.enums.ProductStatus.READY_FOR_KEYWORD_ANALYSIS).build();
        assertThat(productRepository.insert(analyzed).status()).isEqualTo(analyzed.status());
    }

    @Test
    void reviewsMatchSchemaAndAllStatusesRoundTrip() {
        var collection = mongoTemplate.getCollection("reviews");
        var now = java.time.Instant.parse("2026-09-11T00:00:00Z");
        for (var status : com.seoulection.admin.review.infrastructure.document.ReviewStatus.values()) {
            var source = new Document("_id", "schema-review-" + status).append("video_id", "video-1")
                    .append("raw_review_id", "raw-1").append("product_name", "Product")
                    .append("brand", "Brand").append("category", "toners").append("asin", "B001")
                    .append("ad_likelihood", 0.2).append("specificity", 0.8).append("relevance", 0.9)
                    .append("skin_info", new Document("skin_type", "dry"))
                    .append("validator_model", "model").append("validator_prompt_version", "v1")
                    .append("validated_at", java.util.Date.from(now)).append("status", status.name());
            collection.insertOne(source);
            var mapped = mongoTemplate.findById(source.getString("_id"),
                    com.seoulection.admin.review.infrastructure.document.ReviewDocument.class);
            assertThat(mapped.videoId()).isEqualTo("video-1");
            assertThat(mapped.rawReviewId()).isEqualTo("raw-1");
            assertThat(mapped.productName()).isEqualTo("Product");
            assertThat(mapped.validatedAt()).isEqualTo(now);
            assertThat(mapped.status()).isEqualTo(status);
            Document output = new Document();
            mongoTemplate.getConverter().write(mapped, output);
            output.remove("_class");
            assertThat(output).containsExactlyInAnyOrderEntriesOf(source);
        }
    }
}
