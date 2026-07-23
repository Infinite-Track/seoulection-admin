package com.seoulection.admin.survey.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * 선택지 추가 폼.
 *
 * <p>{@code code}는 한 번 만들면 못 고친다(사용자 응답이 이 문자열을 참조한다) → 폼 설명에도 그렇게 적는다.
 */
public class SurveyOptionCreateRequest {

    @NotBlank(message = "문항을 선택해 주세요.")
    private String questionKey;

    @NotBlank(message = "코드를 입력해 주세요.")
    @Size(max = 64, message = "코드는 64자 이하여야 합니다.")
    @Pattern(regexp = "[A-Za-z][A-Za-z0-9_]*",
            message = "코드는 영문으로 시작하고 영문·숫자·밑줄만 쓸 수 있습니다(예: FRAGRANCE_ALLERGY).")
    private String code;

    @NotBlank(message = "노출 문구를 입력해 주세요.")
    @Size(max = 255, message = "노출 문구는 255자 이하여야 합니다.")
    private String label;

    @NotNull(message = "노출 순서를 입력해 주세요.")
    @PositiveOrZero(message = "노출 순서는 0 이상이어야 합니다.")
    private Integer sortOrder;

    private boolean exclusive;

    public String getQuestionKey() {
        return questionKey;
    }

    public void setQuestionKey(String questionKey) {
        this.questionKey = questionKey;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public boolean isExclusive() {
        return exclusive;
    }

    public void setExclusive(boolean exclusive) {
        this.exclusive = exclusive;
    }
}
