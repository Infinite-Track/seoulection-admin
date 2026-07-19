package com.seoulection.admin.product.application.dto;

import com.seoulection.admin.product.domain.entity.Product;
import com.seoulection.admin.product.domain.enums.ProductStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ProductResult(
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

    public static ProductResult from(Product product) {
        return new ProductResult(
                product.id(),
                product.name(),
                product.brand(),
                product.category(),
                product.mentionCount(),
                product.adRatio(),
                product.ingredients(),
                product.inciapiRawData(),
                product.analyzedAt(),
                product.status()
        );
    }
}
