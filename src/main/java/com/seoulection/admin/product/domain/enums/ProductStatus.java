package com.seoulection.admin.product.domain.enums;

/**
 * 제품의 파이프라인 상태.
 *
 * <p>어드민이 옮기는 상태와 파이프라인이 옮기는 상태가 섞여 있다. 특히 아래 둘은
 * <b>파이프라인만</b> 만드는 결과라 어드민 화면에서 만들어 내면 안 된다.
 * <ul>
 *   <li>{@code INSUFFICIENT_INGREDIENTS} — 크롤링은 됐는데 성분이 5개 미만</li>
 *   <li>{@code NOT_FOUND} — 성분 정보 자체를 찾지 못함</li>
 * </ul>
 */
public enum ProductStatus {
    PENDING,
    NEED_MANUAL_REVIEW,
    NOT_FOUND,
    INSUFFICIENT_INGREDIENTS,
    INGREDIENTS_ADDED,
    READY_FOR_INCIAPI,
    READY_FOR_ANALYSIS,
    COMPLETE,
    SUMMARIZED;

    public String displayName() {
        return switch (this) {
            case PENDING -> "대기 중";
            case NEED_MANUAL_REVIEW -> "성분 수동 확인 필요";
            case NOT_FOUND -> "성분 정보 없음";
            case INSUFFICIENT_INGREDIENTS -> "성분 부족";
            case INGREDIENTS_ADDED -> "성분 입력 완료";
            case READY_FOR_INCIAPI -> "INCI API 준비";
            case READY_FOR_ANALYSIS -> "분석 준비";
            case COMPLETE -> "분석 완료";
            case SUMMARIZED -> "요약 완료";
        };
    }

    /**
     * 목록의 상태 배지 색. displayName()과 같은 결의 표현용 메서드다 —
     * 템플릿에서 상태 이름을 일일이 비교하는 th:if 더미를 만들지 않으려고 여기 둔다.
     */
    public String tone() {
        return switch (this) {
            case PENDING -> "neutral";
            case NEED_MANUAL_REVIEW, INSUFFICIENT_INGREDIENTS -> "warning";
            case NOT_FOUND -> "danger";
            case INGREDIENTS_ADDED, READY_FOR_INCIAPI, READY_FOR_ANALYSIS -> "accent";
            case COMPLETE, SUMMARIZED -> "success";
        };
    }

    /**
     * 이 상태가 속한 작업 단계(탭). 단계가 답하는 질문은 "지금 누가 움직여야 하는가"다.
     *
     * <p>READY_FOR_INCIAPI와 READY_FOR_ANALYSIS를 각각 탭으로 쪼개지 않는 이유: 둘 다 어드민이
     * 손댈 게 없어 눌러도 할 일이 없는 탭만 늘어난다. 세부 상태는 표의 상태 배지와 탭 안
     * 소계 칩으로 드러난다. PENDING(수집 대기)도 같은 이유로 PIPELINE에 둔다.
     */
    public ProductStage stage() {
        return switch (this) {
            case NEED_MANUAL_REVIEW -> ProductStage.INGREDIENT_REVIEW;
            case INGREDIENTS_ADDED -> ProductStage.FUNCTIONAL_REVIEW;
            case PENDING, READY_FOR_INCIAPI, READY_FOR_ANALYSIS -> ProductStage.PIPELINE;
            case COMPLETE, SUMMARIZED -> ProductStage.COMPLETED;
            // 성분을 확보하지 못한 두 결말. 다시 시도해 볼 대상이라 한곳에 모아 둔다.
            case INSUFFICIENT_INGREDIENTS, NOT_FOUND -> ProductStage.INGREDIENT_FAILED;
        };
    }

    /**
     * 어드민 작업이 필요한 상태가 열어야 할 workflow 단계. 작업이 없으면 null이다.
     *
     * <p>성분과 기능성은 근거 자료가 다른 별개의 작업이라 한 폼에 같이 두지 않는다. 파이프라인이
     * 굴리는 중이거나 이미 끝난 제품은 null을 돌려 상세 화면으로 보낸다 — 할 일이 없는 제품을
     * 작업 화면에 넣으면 "뭘 하라는 거지"가 된다.
     *
     * <p>NOT_FOUND는 "성분을 못 찾았다"는 뜻이므로 다시 성분 단계로 돌려보낸다.
     */
    public String workflowStep() {
        return switch (this) {
            case NEED_MANUAL_REVIEW, INSUFFICIENT_INGREDIENTS, NOT_FOUND -> "ingredients";
            case INGREDIENTS_ADDED -> "functional";
            case PENDING, READY_FOR_INCIAPI, READY_FOR_ANALYSIS, COMPLETE, SUMMARIZED -> null;
        };
    }

    /** 목록의 "검수" 열 링크 문구. */
    public String actionLabel() {
        String step = workflowStep();
        if (step == null) {
            return "상세 보기";
        }
        return "ingredients".equals(step) ? "성분 보완" : "기능성 확인";
    }

    /** 기능성 검수를 이미 지났는가. 별도 필드 대신 이 판정이 functional_review_status를 대신한다. */
    public boolean functionalReviewDone() {
        return switch (this) {
            case READY_FOR_INCIAPI, READY_FOR_ANALYSIS, COMPLETE, SUMMARIZED -> true;
            default -> false;
        };
    }

}
