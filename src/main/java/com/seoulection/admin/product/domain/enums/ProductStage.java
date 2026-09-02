package com.seoulection.admin.product.domain.enums;

import java.util.Arrays;
import java.util.List;

/**
 * 목록 화면의 작업 단계(탭). 여러 {@link ProductStatus}를 "지금 누가 움직여야 하는가"로 묶는다.
 *
 * <p>DB에는 status만 저장한다 — stage는 저장되지 않는 화면 개념이고, status로부터 유도된다
 * ({@link ProductStatus#stage()}). 그래서 상태가 늘어도 그 매핑 한 줄만 고치면 탭·배지·조회가
 * 모두 따라온다.
 *
 * <p>URL에는 {@code slug}가 나간다({@code ?stage=ingredient-review}). 화면·컨트롤러가
 * "ingredients" 같은 문자열을 직접 비교하지 않게 하려고 enum으로 둔다.
 */
public enum ProductStage {

    INGREDIENT_REVIEW("ingredient-review", "1 성분 보완", "내가 할 일",
            "파이프라인이 성분을 못 가져와 사람이 확인해야 하는 제품입니다."),

    FUNCTIONAL_REVIEW("functional-review", "2 기능성 확인", "내가 할 일",
            "전성분이 채워져 식약처 기능성 여부만 확인하면 되는 제품입니다."),

    PIPELINE("pipeline", "파이프라인", "파이프라인",
            "파이프라인이 움직일 차례입니다. 수집이 늦어지면 성분을 직접 입력해 앞당길 수 있습니다."),

    COMPLETED("completed", "분석 완료", "파이프라인",
            "분석까지 마친 제품입니다."),

    INGREDIENT_FAILED("ingredient-failed", "성분 확보 실패", "",
            "파이프라인이 성분을 확보하지 못한 제품입니다. 성분 부족은 크롤링된 성분이 5개 미만,"
                    + " 성분 정보 없음은 성분 정보를 찾지 못한 경우입니다.");

    // group: 탭 묶음 머리말. 빈 문자열이면 구분선만 긋고 이름은 붙이지 않는다.
    private final String slug;
    private final String label;
    private final String group;
    private final String description;

    ProductStage(String slug, String label, String group, String description) {
        this.slug = slug;
        this.label = label;
        this.group = group;
        this.description = description;
    }

    public String slug() { return slug; }
    public String label() { return label; }
    public String group() { return group; }
    public String description() { return description; }

    /**
     * 건수 배지의 강조 색. 밀리면 곤란한 단계(어드민 작업 큐, 확보 실패)만 눈에 띄게 한다 —
     * 파이프라인이 알아서 굴리는 단계까지 붉게 칠하면 강조가 의미를 잃는다.
     */
    public String countTone() {
        return switch (this) {
            case INGREDIENT_REVIEW, INGREDIENT_FAILED -> "is-alert";
            case FUNCTIONAL_REVIEW -> "is-attention";
            case PIPELINE, COMPLETED -> "";
        };
    }

    /** 이 단계에 속하는 상태들. ProductStatus.stage()에서 유도해 매핑을 한 곳에만 둔다. */
    public List<ProductStatus> statuses() {
        return Arrays.stream(ProductStatus.values()).filter(status -> status.stage() == this).toList();
    }

    /** URL slug로 단계를 찾는다. 모르는 값(손으로 고친 URL, 'all')이면 null. */
    public static ProductStage from(String slug) {
        return Arrays.stream(values())
                .filter(stage -> stage.slug.equals(slug))
                .findFirst()
                .orElse(null);
    }
}
