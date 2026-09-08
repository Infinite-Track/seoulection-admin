package com.seoulection.admin.product.functional.application.port;

import com.seoulection.admin.product.functional.domain.MfdsItem;

import java.util.List;

/**
 * 의약품안전나라 기능성화장품 조회.
 *
 * <p>메서드가 두 개인 이유는 조회 방식이 둘이기 때문이다. 하나는 등록명 후보로 찍어 보는
 * 검색이고, 다른 하나는 브랜드 이름만 넣어 그 브랜드 등록 목록을 통째로 받는 것이다.
 * 후자가 회수율의 절반을 책임진다 — 등록명 표기가 우리 제품명과 달라도 브랜드 목록 안에는
 * 반드시 들어 있기 때문이다.
 *
 * <p>⚠️ {@code entp_name}·{@code bizrno} 같은 파라미터는 <b>API가 무시한다</b>(실측: 전체
 * 건수가 그대로 나온다). 필터로 동작하는 건 {@code item_name} 하나뿐이라, 업체명은 조회
 * 조건이 아니라 응답 검증에만 쓴다.
 */
public interface MfdsCatalogPort {

    /** 등록명 후보 하나로 심사·보고를 모두 조회한다. 한 페이지면 충분한 좁은 검색이다. */
    List<MfdsItem> searchByItemName(String term);

    /** 브랜드 한글명으로 등록 목록 전체를 받는다(페이지 상한까지). 없으면 빈 목록. */
    List<MfdsItem> searchBrand(String brandKo);
}
