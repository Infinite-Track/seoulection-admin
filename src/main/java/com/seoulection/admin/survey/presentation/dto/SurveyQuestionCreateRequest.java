package com.seoulection.admin.survey.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * 문항 추가 폼.
 *
 * <p>{@code questionKey}는 자연 PK다 — 생성 후엔 사실상 못 바꾼다(제출된 응답과 화면 코드가 이 문자열을
 * 그대로 참조한다).
 */
public class SurveyQuestionCreateRequest {

    @NotBlank(message = "문항 키를 입력해 주세요.")
    @Size(max = 32, message = "문항 키는 32자 이하여야 합니다.")
    @Pattern(regexp = "[A-Za-z][A-Za-z0-9_]*",
            message = "문항 키는 영문으로 시작하고 영문·숫자·밑줄만 쓸 수 있습니다(예: SKIN_TYPE_CHECK).")
    private String questionKey;

    @NotBlank(message = "문항 문구를 입력해 주세요.")
    @Size(max = 512, message = "문항 문구는 512자 이하여야 합니다.")
    private String title;

    @NotNull(message = "노출 순서를 입력해 주세요.")
    @PositiveOrZero(message = "노출 순서는 0 이상이어야 합니다.")
    private Integer sortOrder;

    public String getQuestionKey() {
        return questionKey;
    }

    public void setQuestionKey(String questionKey) {
        this.questionKey = questionKey;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
