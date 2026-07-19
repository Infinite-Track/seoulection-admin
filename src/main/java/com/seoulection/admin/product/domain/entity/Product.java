package com.seoulection.admin.product.domain.entity;

import com.seoulection.admin.product.domain.enums.ProductCategory;
import com.seoulection.admin.product.domain.enums.ProductStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Product {

    private final String id;
    private final String name;
    private final String brand;
    private final ProductCategory category;
    private final long mentionCount;
    private final BigDecimal adRatio;
    private final List<String> ingredients;
    private final Map<String, Object> inciapiRawData;
    private final Instant analyzedAt;
    private final ProductStatus status;

    private Product(
            String id,
            String name,
            String brand,
            ProductCategory category,
            long mentionCount,
            BigDecimal adRatio,
            List<String> ingredients,
            Map<String, Object> inciapiRawData,
            Instant analyzedAt,
            ProductStatus status
    ) {
        this.id = id;
        this.name = requireText(name, "name");
        this.brand = requireText(brand, "brand");
        this.category = Objects.requireNonNull(category);
        this.mentionCount = mentionCount;
        this.adRatio = Objects.requireNonNull(adRatio);
        this.ingredients = ingredients;
        this.inciapiRawData = inciapiRawData;
        this.analyzedAt = analyzedAt;
        this.status = Objects.requireNonNull(status);
    }

    public static Product pending(String name, String brand, String category) {
        return new Product(
                null,
                name,
                brand,
                ProductCategory.from(category),
                0L,
                BigDecimal.ZERO,
                null,
                null,
                null,
                ProductStatus.PENDING
        );
    }

    public static Product restore(
            String id,
            String name,
            String brand,
            String category,
            long mentionCount,
            BigDecimal adRatio,
            List<String> ingredients,
            Map<String, Object> inciapiRawData,
            Instant analyzedAt,
            ProductStatus status
    ) {
        return new Product(
                id,
                name,
                brand,
                ProductCategory.from(category),
                mentionCount,
                adRatio,
                ingredients,
                inciapiRawData,
                analyzedAt,
                status
        );
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + "은(는) 필수입니다.");
        }
        return value.trim();
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String brand() {
        return brand;
    }

    public String category() {
        return category.value();
    }

    public long mentionCount() {
        return mentionCount;
    }

    public BigDecimal adRatio() {
        return adRatio;
    }

    public List<String> ingredients() {
        return ingredients;
    }

    public Map<String, Object> inciapiRawData() {
        return inciapiRawData;
    }

    public Instant analyzedAt() {
        return analyzedAt;
    }

    public ProductStatus status() {
        return status;
    }
}
