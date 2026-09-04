package com.seoulection.admin.product.domain.entity;

import com.seoulection.admin.product.domain.enums.ProductCategory;
import com.seoulection.admin.product.domain.enums.ProductFunctionalCategory;
import com.seoulection.admin.product.domain.enums.ProductStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * products 컬렉션의 제품 한 건.
 *
 * <p>필드는 셋으로 나뉜다 — (1) 수집 단계가 채우는 카탈로그 정보(asin·description·price·
 * thumbnailUrl·productUrl), (2) 분석 파이프라인이 채우는 값(mentionCount·adRatio·
 * adLikelihoodSum·inciapiRawData·analyzedAt), (3) 어드민이 검수로 채우는 값(ingredients·
 * ingredientSource·function). 어드민 화면이 직접 쓰는 건 (3)뿐이고 나머지는 읽기 전용이다.
 *
 * <p>기능성 검수 여부를 담는 별도 필드는 두지 않는다. {@link ProductStatus}가 그 역할을 한다 —
 * INGREDIENTS_ADDED면 아직 안 본 것이고, READY_FOR_INCIAPI 이후면 본 것이다. 그리고 "봤는데
 * 기능성이 아니었다"와 "기능성이 확인됐다"는 {@code function}이 비었는지로 갈린다.
 */
public class Product {

    private final String id;
    private final String asin;
    private final String name;
    private final String brand;
    private final ProductCategory category;
    private final String description;
    private final BigDecimal price;
    private final String thumbnailUrl;
    private final String productUrl;
    private final long mentionCount;
    private final BigDecimal adRatio;
    private final BigDecimal adLikelihoodSum;
    private final String ingredientSource;
    private final List<String> ingredients;
    private final Map<String, Object> inciapiRawData;
    private final Instant analyzedAt;
    private final List<ProductFunctionalCategory> function;
    private final ProductStatus status;

    private Product(Builder builder) {
        this.id = builder.id;
        this.asin = builder.asin;
        this.name = requireText(builder.name, "name");
        this.brand = requireText(builder.brand, "brand");
        this.category = Objects.requireNonNull(builder.category);
        this.description = builder.description;
        this.price = builder.price;
        this.thumbnailUrl = builder.thumbnailUrl;
        this.productUrl = builder.productUrl;
        this.mentionCount = builder.mentionCount;
        this.adRatio = builder.adRatio == null ? BigDecimal.ZERO : builder.adRatio;
        this.adLikelihoodSum = builder.adLikelihoodSum;
        this.ingredientSource = builder.ingredientSource;
        this.ingredients = builder.ingredients;
        this.inciapiRawData = builder.inciapiRawData;
        this.analyzedAt = builder.analyzedAt;
        this.function = builder.function == null ? List.of() : List.copyOf(builder.function);
        this.status = Objects.requireNonNull(builder.status);
    }

    public static Builder builder() {
        return new Builder();
    }

    /** 어드민이 새로 등록하는 제품. 성분을 같이 넣으면 바로 기능성 확인 단계로 간다. */
    public static Product pending(String name, String brand, String category) {
        return pending(name, brand, category, null);
    }

    public static Product pending(String name, String brand, String category, List<String> ingredients) {
        boolean hasIngredients = ingredients != null && !ingredients.isEmpty();
        return builder()
                .name(name)
                .brand(brand)
                .category(ProductCategory.from(category))
                .ingredients(hasIngredients ? ingredients : null)
                .ingredientSource(hasIngredients ? ADMIN_SOURCE : null)
                .status(hasIngredients ? ProductStatus.INGREDIENTS_ADDED : ProductStatus.PENDING)
                .build();
    }

    /** 어드민이 직접 입력한 성분임을 표시하는 값. 파이프라인은 자기 출처 값을 따로 넣는다. */
    public static final String ADMIN_SOURCE = "ADMIN";

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + "은(는) 필수입니다.");
        }
        return value.trim();
    }

    public String id() { return id; }
    public String asin() { return asin; }
    public String name() { return name; }
    public String brand() { return brand; }
    public String category() { return category.value(); }
    public String description() { return description; }
    public BigDecimal price() { return price; }
    public String thumbnailUrl() { return thumbnailUrl; }
    public String productUrl() { return productUrl; }
    public long mentionCount() { return mentionCount; }
    public BigDecimal adRatio() { return adRatio; }
    public BigDecimal adLikelihoodSum() { return adLikelihoodSum; }
    public String ingredientSource() { return ingredientSource; }
    public List<String> ingredients() { return ingredients; }
    public Map<String, Object> inciapiRawData() { return inciapiRawData; }
    public Instant analyzedAt() { return analyzedAt; }
    public List<ProductFunctionalCategory> function() { return function; }
    public ProductStatus status() { return status; }

    /**
     * 1단계 검수 — 전성분만 갱신한다. function은 손대지 않는다.
     *
     * <p>이미 기능성 검수를 지난 제품(READY_FOR_INCIAPI 이후)은 성분을 고쳐도 앞 단계로
     * 되돌리지 않는다. 성분 오타 하나 고쳤다고 기능성을 다시 보게 만들 이유가 없다.
     */
    public Product reviewIngredients(List<String> ingredients, boolean ingredientNotFound) {
        ProductStatus nextStatus;
        if (ingredientNotFound) {
            nextStatus = ProductStatus.NOT_FOUND;
        } else if (ingredients == null || ingredients.isEmpty()) {
            // 빈 저장은 아무것도 확인하지 못한 것이다. INSUFFICIENT_INGREDIENTS는 "크롤링은 됐는데
            // 성분이 5개 미만"이라는 파이프라인의 판정이라 어드민 저장으로 만들어 내면 안 된다.
            nextStatus = status;
        } else if (status.functionalReviewDone()) {
            nextStatus = status;
        } else {
            nextStatus = ProductStatus.INGREDIENTS_ADDED;
        }
        boolean cleared = ingredientNotFound || ingredients == null || ingredients.isEmpty();
        return toBuilder()
                .ingredients(cleared ? null : ingredients)
                .ingredientSource(cleared ? null : ADMIN_SOURCE)
                .status(nextStatus)
                .build();
    }

    /**
     * 2단계 검수 — 식약처 기능성 결과만 갱신한다. 성분은 손대지 않는다.
     *
     * <p>빈 목록도 "검수했고 기능성이 아니었다"는 결과이므로 상태는 똑같이 전진한다.
     * 검수 행위가 status를 옮기고, 무엇이 확인됐는지는 function이 담는다.
     *
     * <p>NOT_FOUND는 그대로 둔다 — 성분을 못 찾았다는 사실이 기능성 검수로 뒤집히지는 않는다.
     */
    public Product reviewFunction(List<ProductFunctionalCategory> function) {
        ProductStatus nextStatus;
        if (status == ProductStatus.NOT_FOUND
                || status == ProductStatus.COMPLETE
                || status == ProductStatus.SUMMARIZED) {
            nextStatus = status;
        } else if (ingredients == null || ingredients.isEmpty()) {
            nextStatus = status; // 성분 없이 기능성만 확정할 수는 없다 — 상태를 그대로 둔다.
        } else {
            nextStatus = ProductStatus.READY_FOR_INCIAPI;
        }
        return toBuilder().function(function).status(nextStatus).build();
    }

    public Builder toBuilder() {
        return new Builder()
                .id(id).asin(asin).name(name).brand(brand).category(category)
                .description(description).price(price).thumbnailUrl(thumbnailUrl).productUrl(productUrl)
                .mentionCount(mentionCount).adRatio(adRatio).adLikelihoodSum(adLikelihoodSum)
                .ingredientSource(ingredientSource).ingredients(ingredients)
                .inciapiRawData(inciapiRawData).analyzedAt(analyzedAt)
                .function(function).status(status);
    }

    /** 필드가 18개라 위치 인자 생성자는 읽을 수 없다 — 복원·수정 모두 이 빌더를 쓴다. */
    public static class Builder {
        private String id;
        private String asin;
        private String name;
        private String brand;
        private ProductCategory category;
        private String description;
        private BigDecimal price;
        private String thumbnailUrl;
        private String productUrl;
        private long mentionCount;
        private BigDecimal adRatio;
        private BigDecimal adLikelihoodSum;
        private String ingredientSource;
        private List<String> ingredients;
        private Map<String, Object> inciapiRawData;
        private Instant analyzedAt;
        private List<ProductFunctionalCategory> function;
        private ProductStatus status;

        public Builder id(String v) { this.id = v; return this; }
        public Builder asin(String v) { this.asin = v; return this; }
        public Builder name(String v) { this.name = v; return this; }
        public Builder brand(String v) { this.brand = v; return this; }
        public Builder category(ProductCategory v) { this.category = v; return this; }
        public Builder category(String v) { this.category = ProductCategory.from(v); return this; }
        public Builder description(String v) { this.description = v; return this; }
        public Builder price(BigDecimal v) { this.price = v; return this; }
        public Builder thumbnailUrl(String v) { this.thumbnailUrl = v; return this; }
        public Builder productUrl(String v) { this.productUrl = v; return this; }
        public Builder mentionCount(long v) { this.mentionCount = v; return this; }
        public Builder adRatio(BigDecimal v) { this.adRatio = v; return this; }
        public Builder adLikelihoodSum(BigDecimal v) { this.adLikelihoodSum = v; return this; }
        public Builder ingredientSource(String v) { this.ingredientSource = v; return this; }
        public Builder ingredients(List<String> v) { this.ingredients = v; return this; }
        public Builder inciapiRawData(Map<String, Object> v) { this.inciapiRawData = v; return this; }
        public Builder analyzedAt(Instant v) { this.analyzedAt = v; return this; }
        public Builder function(List<ProductFunctionalCategory> v) { this.function = v; return this; }
        public Builder status(ProductStatus v) { this.status = v; return this; }

        public Product build() { return new Product(this); }
    }
}
