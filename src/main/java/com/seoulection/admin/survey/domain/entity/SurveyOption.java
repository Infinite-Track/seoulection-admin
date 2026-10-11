package com.seoulection.admin.survey.domain.entity;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 설문 선택지 — 관리자가 CRUD하는 대상.
 *
 * <p><b>{@code code}는 생성 시 확정되고 이후 바뀌지 않는다.</b> 사용자 응답이
 * {@code survey_answer.codes} jsonb 배열에 이 문자열로 저장돼 있어서, 코드를 고치면 이미 제출된
 * 응답이 가리키는 대상이 사라진다. 그래서 수정 메서드({@link #withDetails})에 code가 없다 —
 * 화면에서 막는 게 아니라 <b>타입에서 막는다.</b>
 *
 * <p>{@code capsFeatureAt} / {@code referDermatologist}(api-server V30): 이 선택지를 고르면 문항이 묶인 지표
 * 점수에 상한을 걸거나(결절 "예" → 여드름 ≤ 25), 결과 화면에 피부과 진료 안내를 띄운다. 점수({@code value})가
 * 없는 예/아니오 선택지에도 붙일 수 있다.
 *
 * <p>삭제도 마찬가지 이유로 {@link #withActive}(soft delete)만 제공한다.
 */
public class SurveyOption {

    /** 대문자 스네이크. api-server가 이 문자열을 안정 키로 다루므로 공백·소문자를 허용하지 않는다. */
    private static final Pattern CODE_FORMAT = Pattern.compile("^[A-Z][A-Z0-9_]*$");

    private final Long id;
    private final String questionKey;
    private final String code;
    private final String label;
    private final Integer value;
    private final int sortOrder;
    private final boolean exclusive;
    private final boolean active;
    private final Integer capsFeatureAt;
    private final boolean referDermatologist;

    private SurveyOption(Long id, String questionKey, String code, String label, Integer value,
                         int sortOrder, boolean exclusive, boolean active,
                         Integer capsFeatureAt, boolean referDermatologist) {
        this.id = id;
        this.questionKey = Objects.requireNonNull(questionKey, "questionKey");
        this.code = requireCode(code);
        this.label = requireText(label, "label");
        this.value = requireValidValue(value, "점수");
        this.sortOrder = requireNonNegative(sortOrder);
        this.exclusive = exclusive;
        this.active = active;
        this.capsFeatureAt = requireValidValue(capsFeatureAt, "지표 상한");
        this.referDermatologist = referDermatologist;
    }

    /** 새 선택지. 활성 상태로 시작한다. {@code value}는 카테고리성 선택지(회피 항목 등)라면 null. */
    public static SurveyOption create(String questionKey, String code, String label, Integer value,
                                      int sortOrder, boolean exclusive,
                                      Integer capsFeatureAt, boolean referDermatologist) {
        return new SurveyOption(null, questionKey, code, label, value, sortOrder, exclusive, true,
                capsFeatureAt, referDermatologist);
    }

    /** 상한·진료 안내 없는 선택지. 옛 호출부 호환. */
    public static SurveyOption create(String questionKey, String code, String label, Integer value,
                                      int sortOrder, boolean exclusive) {
        return create(questionKey, code, label, value, sortOrder, exclusive, null, false);
    }

    /** 저장소가 읽어온 기존 행을 복원할 때 쓴다. */
    public static SurveyOption of(Long id, String questionKey, String code, String label, Integer value,
                                  int sortOrder, boolean exclusive, boolean active,
                                  Integer capsFeatureAt, boolean referDermatologist) {
        return new SurveyOption(id, questionKey, code, label, value, sortOrder, exclusive, active,
                capsFeatureAt, referDermatologist);
    }

    /** 문구·점수·순서·단독선택·상한·진료 안내를 고친다. <b>code와 active는 대상이 아니다.</b> */
    public SurveyOption withDetails(String newLabel, Integer newValue, int newSortOrder, boolean newExclusive,
                                    Integer newCapsFeatureAt, boolean newReferDermatologist) {
        return new SurveyOption(id, questionKey, code, newLabel, newValue, newSortOrder, newExclusive, active,
                newCapsFeatureAt, newReferDermatologist);
    }

    /** 상한·진료 안내는 그대로 두고 나머지만 고친다. 옛 호출부 호환. */
    public SurveyOption withDetails(String newLabel, Integer newValue, int newSortOrder, boolean newExclusive) {
        return withDetails(newLabel, newValue, newSortOrder, newExclusive, capsFeatureAt, referDermatologist);
    }

    /** 노출/숨김. {@code false}가 곧 삭제다(행은 남는다). */
    public SurveyOption withActive(boolean newActive) {
        return new SurveyOption(id, questionKey, code, label, value, sortOrder, exclusive, newActive,
                capsFeatureAt, referDermatologist);
    }

    private static String requireCode(String code) {
        String trimmed = requireText(code, "code");
        if (!CODE_FORMAT.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("코드는 영문 대문자·숫자·밑줄만 쓸 수 있습니다(예: FRAGRANCE_ALLERGY).");
        }
        return trimmed;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + "을(를) 입력해 주세요.");
        }
        return value.trim();
    }

    private static int requireNonNegative(int sortOrder) {
        if (sortOrder < 0) {
            throw new IllegalArgumentException("노출 순서는 0 이상이어야 합니다.");
        }
        return sortOrder;
    }

    /** 카테고리성 선택지는 점수 개념이 없어 null을 허용한다. 있다면 0~100 범위여야 한다. */
    private static Integer requireValidValue(Integer value, String name) {
        if (value != null && (value < 0 || value > 100)) {
            throw new IllegalArgumentException(name + "은(는) 0에서 100 사이여야 합니다.");
        }
        return value;
    }

    public Long getId() {
        return id;
    }

    public String getQuestionKey() {
        return questionKey;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public Integer getValue() {
        return value;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public boolean isExclusive() {
        return exclusive;
    }

    public boolean isActive() {
        return active;
    }

    public Integer getCapsFeatureAt() {
        return capsFeatureAt;
    }

    public boolean isReferDermatologist() {
        return referDermatologist;
    }
}
