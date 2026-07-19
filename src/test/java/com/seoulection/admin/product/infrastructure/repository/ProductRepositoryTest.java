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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;

import java.math.BigDecimal;

import static com.mongodb.client.model.Filters.eq;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class ProductRepositoryTest {

    @Container
    @ServiceConnection
    static MongoDBContainer mongo = new MongoDBContainer("mongo:8");

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
}
