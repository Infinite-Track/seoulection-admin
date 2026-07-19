package com.seoulection.admin.product.infrastructure.document;

import com.seoulection.admin.product.domain.entity.Product;
import com.seoulection.admin.product.domain.enums.ProductStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Document(collection = "products")
public class ProductDocument {

    @Id
    private String id;

    private String name;
    private String brand;
    private String category;

    @Field("mention_count")
    private long mentionCount;

    @Field(name = "ad_ratio", targetType = FieldType.DECIMAL128)
    private BigDecimal adRatio;

    private List<String> ingredients;

    @Field("inciapi_raw_data")
    private Map<String, Object> inciapiRawData;

    @Field("analyzed_at")
    private Instant analyzedAt;

    private ProductStatus status;

    protected ProductDocument() {
    }

    private ProductDocument(Product product) {
        this.id = product.id();
        this.name = product.name();
        this.brand = product.brand();
        this.category = product.category();
        this.mentionCount = product.mentionCount();
        this.adRatio = product.adRatio();
        this.ingredients = product.ingredients();
        this.inciapiRawData = product.inciapiRawData();
        this.analyzedAt = product.analyzedAt();
        this.status = product.status();
    }

    public static ProductDocument fromDomain(Product product) {
        return new ProductDocument(product);
    }

    public Product toDomain() {
        return Product.restore(
                id,
                name,
                brand,
                category,
                mentionCount,
                adRatio,
                ingredients,
                inciapiRawData,
                analyzedAt,
                status
        );
    }
}
