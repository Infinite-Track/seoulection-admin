package com.seoulection.admin.survey.domain.entity;

import com.seoulection.admin.survey.domain.enums.SurveyQuestionKey;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 설문 선택지 — 관리자가 CRUD하는 대상.
 *
 * <p><b>{@code code}는 생성 시 확정되고 이후 바뀌지 않는다.</b> 사용자 응답이
 * {@code survey_response.avoidances} jsonb 배열에 이 문자열로 저장돼 있어서, 코드를 고치면 이미 제출된
 * 응답이 가리키는 대상이 사라진다. 그래서 수정 메서드({@link #withDetails})에 code가 없다 —
 * 화면에서 막는 게 아니라 <b>타입에서 막는다.</b>
 *
 * <p>삭제도 마찬가지 이유로 {@link #withActive}(soft delete)만 제공한다.
 */
public class SurveyOption {

    /** 대문자 스네이크. api-server가 이 문자열을 안정 키로 다루므로 공백·소문자를 허용하지 않는다. */
    private static final Pattern CODE_FORMAT = Pattern.compile("^[A-Z][A-Z0-9_]*$");

    private final Long id;
    private final SurveyQuestionKey questionKey;
    private final String code;
    private final String label;
    private final int sortOrder;
    private final boolean exclusive;
    private final boolean active;

    private SurveyOption(Long id, SurveyQuestionKey questionKey, String code, String label,
                         int sortOrder, boolean exclusive, boolean active) {
        this.id = id;
        this.questionKey = Objects.requireNonNull(questionKey, "questionKey");
        this.code = requireCode(code);
        this.label = requireText(label, "label");
        this.sortOrder = requireNonNegative(sortOrder);
        this.exclusive = exclusive;
        this.active = active;
    }

    /** 새 선택지. 활성 상태로 시작한다. */
    public static SurveyOption create(SurveyQuestionKey questionKey, String code, String label,
                                      int sortOrder, boolean exclusive) {
        return new SurveyOption(null, questionKey, code, label, sortOrder, exclusive, true);
    }

    /** 저장소가 읽어온 기존 행을 복원할 때 쓴다. */
    public static SurveyOption of(Long id, SurveyQuestionKey questionKey, String code, String label,
                                  int sortOrder, boolean exclusive, boolean active) {
        return new SurveyOption(id, questionKey, code, label, sortOrder, exclusive, active);
    }

    /** 문구·순서·단독선택 여부를 고친다. <b>code와 active는 대상이 아니다.</b> */
    public SurveyOption withDetails(String newLabel, int newSortOrder, boolean newExclusive) {
        return new SurveyOption(id, questionKey, code, newLabel, newSortOrder, newExclusive, active);
    }

    /** 노출/숨김. {@code false}가 곧 삭제다(행은 남는다). */
    public SurveyOption withActive(boolean newActive) {
        return new SurveyOption(id, questionKey, code, label, sortOrder, exclusive, newActive);
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

    public Long getId() {
        return id;
    }

    public SurveyQuestionKey getQuestionKey() {
        return questionKey;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
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
}
