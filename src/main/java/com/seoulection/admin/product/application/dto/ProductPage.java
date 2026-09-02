package com.seoulection.admin.product.application.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 목록 한 페이지. 템플릿에서 Spring의 Page를 직접 다루면 표현식이 지저분해져서
 * 화면이 실제로 쓰는 값만 추려 둔다.
 */
public record ProductPage(
        List<ProductResult> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static ProductPage from(Page<ProductResult> source) {
        return new ProductPage(source.getContent(), source.getNumber(), source.getSize(),
                source.getTotalElements(), source.getTotalPages());
    }

    public boolean isEmpty() { return content.isEmpty(); }
    public boolean hasPrevious() { return page > 0; }
    public boolean hasNext() { return page + 1 < totalPages; }

    /** "21-40 / 132"의 앞 두 숫자. 비어 있으면 0을 돌려준다. */
    public long firstItem() { return content.isEmpty() ? 0 : (long) page * size + 1; }
    public long lastItem() { return content.isEmpty() ? 0 : (long) page * size + content.size(); }
}
