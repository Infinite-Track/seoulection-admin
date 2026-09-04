package com.seoulection.admin.product.domain.repository;

import com.seoulection.admin.product.domain.entity.Product;
import com.seoulection.admin.product.domain.enums.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface ProductRepository {

    Product insert(Product product);

    Product findById(String id);

    Product save(Product product);

    void deleteById(String id);

    /**
     * 목록 한 페이지. 검색어·상태 필터를 모두 Mongo 쪽에서 적용한다 —
     * 전체를 메모리에 올려 자바에서 거르면 제품이 늘어날수록 목록 화면이 먼저 무너진다.
     *
     * @param keyword  제품명·브랜드 부분 일치. 비어 있으면 전체.
     * @param statuses 이 상태들만. 비어 있으면 전체.
     */
    Page<Product> find(String keyword, List<ProductStatus> statuses, Pageable pageable);

    /** 검색어를 적용한 상태별 건수. 탭 배지와 소계를 질의 한 번으로 채운다. */
    Map<ProductStatus, Long> countByStatus(String keyword);
}
