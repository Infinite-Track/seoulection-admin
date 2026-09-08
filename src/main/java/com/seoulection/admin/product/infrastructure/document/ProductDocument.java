package com.seoulection.admin.product.infrastructure.document;

import com.seoulection.admin.product.domain.entity.Product;
import com.seoulection.admin.product.domain.enums.ProductFunctionalCategory;
import com.seoulection.admin.product.domain.enums.ProductStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
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

    /** 수집 원본(아마존)의 제품 식별자. 같은 제품을 두 번 넣지 않기 위한 자연키다. */
    @Indexed(unique = true, sparse = true)
    private String asin;

    private String name;
    @Field("name_ko")
    private String nameKo;
    private String brand;
    private String category;
    private String description;

    @Field(name = "price", targetType = FieldType.DECIMAL128)
    private BigDecimal price;

    @Field("thumbnail_url")
    private String thumbnailUrl;

    @Field("product_url")
    private String productUrl;

    @Field("mention_count")
    private long mentionCount;

    @Field(name = "ad_ratio", targetType = FieldType.DECIMAL128)
    private BigDecimal adRatio;

    @Field(name = "ad_likelihood_sum", targetType = FieldType.DECIMAL128)
    private BigDecimal adLikelihoodSum;

    /** 성분을 어디서 얻었는지. 어드민이 직접 입력하면 ADMIN, 파이프라인은 자기 값을 넣는다. */
    @Field("ingredient_source")
    private String ingredientSource;

    private List<String> ingredients;

    @Field("inciapi_raw_data")
    private Map<String, Object> inciapiRawData;

    @Field("analyzed_at")
    private Instant analyzedAt;

    /** 식약처 기능성 유형. 비어 있으면 "검수했으나 기능성 아님"이다 — 검수 여부는 status가 안다. */
    private List<ProductFunctionalCategory> function;

    @Indexed
    private ProductStatus status;

    protected ProductDocument() {
    }

    private ProductDocument(Product product) {
        this.id = product.id();
        this.asin = product.asin();
        this.name = product.name();
        this.nameKo = product.nameKo();
        this.brand = product.brand();
        this.category = product.category();
        this.description = product.description();
        this.price = product.price();
        this.thumbnailUrl = product.thumbnailUrl();
        this.productUrl = product.productUrl();
        this.mentionCount = product.mentionCount();
        this.adRatio = product.adRatio();
        this.adLikelihoodSum = product.adLikelihoodSum();
        this.ingredientSource = product.ingredientSource();
        this.ingredients = product.ingredients();
        this.inciapiRawData = product.inciapiRawData();
        this.analyzedAt = product.analyzedAt();
        this.function = product.function();
        this.status = product.status();
    }

    public static ProductDocument fromDomain(Product product) {
        return new ProductDocument(product);
    }

    public Product toDomain() {
        return Product.builder()
                .id(id)
                .asin(asin)
                .name(name)
                .nameKo(nameKo)
                .brand(brand)
                .category(category)
                .description(description)
                .price(price)
                .thumbnailUrl(thumbnailUrl)
                .productUrl(productUrl)
                .mentionCount(mentionCount)
                .adRatio(adRatio)
                .adLikelihoodSum(adLikelihoodSum)
                .ingredientSource(ingredientSource)
                .ingredients(ingredients)
                .inciapiRawData(inciapiRawData)
                .analyzedAt(analyzedAt)
                .function(function)
                .status(status == null ? ProductStatus.PENDING : status)
                .build();
    }
}
