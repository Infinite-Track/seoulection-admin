package com.seoulection.admin.product.application;

import com.seoulection.admin.product.application.dto.ProductResult;
import com.seoulection.admin.product.application.service.ProductService;
import com.seoulection.admin.product.domain.entity.Product;
import com.seoulection.admin.product.domain.enums.ProductStatus;
import com.seoulection.admin.product.domain.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    ProductRepository repository;

    @InjectMocks
    ProductService service;

    @Test
    @DisplayName("제품 기본 정보만 받아 PENDING 제품으로 등록한다")
    void register() {
        given(repository.insert(any(Product.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        ProductResult result = service.register("시카 세럼", "서울렉션", "sunscreens");

        assertThat(result.name()).isEqualTo("시카 세럼");
        assertThat(result.brand()).isEqualTo("서울렉션");
        assertThat(result.category()).isEqualTo("sunscreens");
        assertThat(result.mentionCount()).isZero();
        assertThat(result.adRatio()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.ingredients()).isNull();
        assertThat(result.inciapiRawData()).isNull();
        assertThat(result.analyzedAt()).isNull();
        assertThat(result.status()).isEqualTo(ProductStatus.PENDING);
    }
}
