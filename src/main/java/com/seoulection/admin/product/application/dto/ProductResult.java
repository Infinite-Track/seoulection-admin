package com.seoulection.admin.product.application.dto;

import com.seoulection.admin.product.domain.entity.Product;
import com.seoulection.admin.product.domain.enums.ProductFunctionalCategory;
import com.seoulection.admin.product.domain.enums.ProductStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ProductResult(
        String id,
        String asin,
        String name,
        String nameKo,
        String brand,
        String category,
        String description,
        BigDecimal price,
        String thumbnailUrl,
        String productUrl,
        long mentionCount,
        BigDecimal adRatio,
        BigDecimal adLikelihoodSum,
        String ingredientSource,
        List<String> ingredients,
        Map<String, Object> inciapiRawData,
        Instant analyzedAt,
        List<ProductFunctionalCategory> function,
        ProductStatus status
) {

    public static ProductResult from(Product product) {
        return new ProductResult(
                product.id(),
                product.asin(),
                product.name(),
                product.nameKo(),
                product.brand(),
                product.category(),
                product.description(),
                product.price(),
                product.thumbnailUrl(),
                product.productUrl(),
                product.mentionCount(),
                product.adRatio(),
                product.adLikelihoodSum(),
                product.ingredientSource(),
                product.ingredients(),
                product.inciapiRawData(),
                product.analyzedAt(),
                product.function(),
                product.status()
        );
    }

    public int ingredientCount() {
        return ingredients == null ? 0 : ingredients.size();
    }

    /** 기능성 검수를 마쳤고 확인된 유형이 있는가. 검수 자체를 했는지는 status가 안다. */
    public boolean hasFunction() {
        return function != null && !function.isEmpty();
    }
}
