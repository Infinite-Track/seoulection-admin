package com.seoulection.admin.product.domain.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductCategoryTest {

    @Test
    void readsLegacyCategoryWithoutCaseSensitivity() {
        assertThat(ProductCategory.from("Moisturizers"))
                .isEqualTo(ProductCategory.MOISTURIZER);
    }
}
