package com.seoulection.admin.product.functional.application.port;

import com.seoulection.admin.product.application.dto.ProductResult;

import java.util.List;

/**
 * 판정 대상 제품에서 조회에 필요한 것만 추린 값. 포트가 {@code ProductResult} 전체(18필드)를
 * 알 필요는 없고, LLM 프롬프트에 통째로 실어 보낼 이유는 더더욱 없다.
 */
public record ScreeningTarget(
        String id,
        String name,
        String nameKo,
        String brand,
        String category,
        List<String> ingredients
) {
    public static ScreeningTarget from(ProductResult product) {
        return new ScreeningTarget(product.id(), product.name(), product.nameKo(),
                product.brand(), product.category(),
                product.ingredients() == null ? List.of() : product.ingredients());
    }

    /** 조회·비교의 기준이 되는 이름. 한글명이 있으면 그게 등록명에 가깝다. */
    public String displayName() {
        return nameKo == null || nameKo.isBlank() ? name : nameKo;
    }

    /**
     * 등록명은 대개 브랜드로 시작한다 — 비교 대상도 같은 모양으로 만든다.
     *
     * <p>⚠️ 이름이 이미 브랜드로 시작하면 붙이지 않는다. 어드민은 한글 이름을 "달바 워터풀"처럼
     * 브랜드까지 넣어 적는 경우가 많고, 그때 앞에 또 붙이면 "달바달바워터풀"로 조회돼
     * <b>한 건도 안 나온다</b>.
     */
    public String brandedName(String brandKo) {
        String prefix = brandKo == null || brandKo.isBlank() ? brand : brandKo;
        String name = displayName();
        return com.seoulection.admin.product.functional.domain.ItemName.startsWithBrand(name, prefix)
                ? name : prefix + name;
    }
}
