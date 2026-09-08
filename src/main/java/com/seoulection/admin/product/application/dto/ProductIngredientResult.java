package com.seoulection.admin.product.application.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 화면에 뿌릴 제품 성분 한 줄.
 *
 * <p>{@code matchedInciName} 이 null 이면 성분 사전에서 못 찾은 것이다 — 화면이 그걸 표시해
 * 어드민이 사전에 추가하도록 유도한다. 못 찾는 것은 오류가 아니라 정상적인 상태다.
 *
 * <p>순도가 별도 필드가 아니라 {@code properties} 안에 있는 이유는 {@link ProductIngredientProperty} 참고.
 */
public record ProductIngredientResult(long id, String ingredientId, String rawName, int order,
        BigDecimal concentrationMin, BigDecimal concentrationMax, String unit,
        String notes, String matchedInciName, String matchedNameKo,
        List<ProductIngredientProperty> properties) {

    /** 성분 사전과 연결됐는가. 화면의 매칭 배지가 이걸 본다. */
    public boolean matched() {
        return ingredientId != null && !ingredientId.isBlank();
    }

    /** 표시용 이름 — 사전에 있으면 한글명, 없으면 원문. */
    public String displayName() {
        if (matchedNameKo != null && !matchedNameKo.isBlank()) return matchedNameKo;
        return rawName;
    }
}
