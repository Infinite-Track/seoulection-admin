package com.seoulection.admin.survey.domain.entity;

import java.util.Objects;

/**
 * 설문 문항 — 관리자는 <b>문구({@code title})만</b> 고친다.
 *
 * <p>{@code questionKey}는 문자열 식별자다(api-server와 동일). 예전엔 {@code SurveyQuestionKey} enum이었지만,
 * api-server의 제출 요청이 문항마다 고정 필드를 갖는 구조를 벗어나 문항 목록 형태로 바뀌면서 문항 집합을
 * 코드에 고정할 이유가 사라졌다 — 지금은 {@link SurveyOption}과 마찬가지로 행의 추가·삭제가 데이터만으로 가능하다.
 */
public class SurveyQuestion {

    private final String questionKey;
    private final String title;
    private final int sortOrder;

    private SurveyQuestion(String questionKey, String title, int sortOrder) {
        this.questionKey = Objects.requireNonNull(questionKey, "questionKey");
        this.title = requireText(title);
        this.sortOrder = sortOrder;
    }

    public static SurveyQuestion of(String questionKey, String title, int sortOrder) {
        return new SurveyQuestion(questionKey, title, sortOrder);
    }

    public SurveyQuestion withTitle(String newTitle) {
        return new SurveyQuestion(questionKey, newTitle, sortOrder);
    }

    private static String requireText(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("질문 문구를 입력해 주세요.");
        }
        return title.trim();
    }

    public String getQuestionKey() {
        return questionKey;
    }

    public String getTitle() {
        return title;
    }

    public int getSortOrder() {
        return sortOrder;
    }
}
