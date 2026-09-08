package com.seoulection.admin.product.functional.application.port;

import com.seoulection.admin.product.functional.domain.MfdsItem;

import java.util.List;

/**
 * 이름 문제를 푸는 자리. <b>기능성 판단은 여기서 하지 않는다</b> — 유형은 오직 안전나라
 * 응답에서만 나온다({@code FunctionalClaims}). 이 포트가 답하는 건 "무엇으로 검색할지"와
 * "이 후보가 같은 제품인지"뿐이다.
 *
 * <p>구현이 둘이다. LLM 구현({@code ClaudeProductNameResolver})과 규칙 기반 대체
 * 구현({@code HeuristicProductNameResolver}). 설정 한 줄로 갈리고, LLM이 꺼져 있어도
 * 자동화 전체가 동작한다 — 회수율만 낮아진다.
 */
public interface ProductNameResolverPort {

    /**
     * 브랜드의 한글 표기 후보. {@code Goodal → 구달}, {@code d'Alba → 달바}.
     *
     * <p>브랜드 단위라 캐시가 잘 듣는다. 제품마다 부르지 말 것.
     */
    List<String> koreanBrandAliases(String brand);

    /**
     * 안전나라 등록명 후보. {@code 구달 청귤 비타C 잡티 세럼 → 구달청귤비타씨잡티세럼}.
     *
     * <p>등록명은 유통명과 표기가 다르다({@code 비타C→비타씨}, {@code 10%→10퍼센트}).
     * 정규화로는 못 넘는 벽이고, 검색 실패의 대부분이 여기서 난다.
     */
    List<String> registrationNameCandidates(ScreeningTarget target, String brandKo);

    /**
     * 후보 중 같은 제품을 고른다.
     *
     * <p>유사도로는 못 거르는 것들이 있다 — 실측에서 {@code 311 모이스처라이저}가
     * {@code 131모이스처라이저}와 0.94로 붙었다. 숫자 규칙이 1차 방어이고, 이 판정이 2차다.
     */
    CandidateVerdict judge(ScreeningTarget target, List<MfdsItem> candidates);
}
