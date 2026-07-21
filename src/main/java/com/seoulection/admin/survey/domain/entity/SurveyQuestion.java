package com.seoulection.admin.survey.domain.entity;

import com.seoulection.admin.survey.domain.enums.SurveyQuestionKey;

import java.util.Objects;

/**
 * 설문 문항 — 관리자는 <b>문구({@code title})만</b> 고친다.
 *
 * <p>추가·삭제가 없는 이유: 문항 하나가 api-server 제출 요청 본문의 필드 하나와 1:1로 묶여 있다
 * ({@code AVOIDANCE → avoidances}). 행을 지우면 그 필드를 받는 쪽이 갈 곳을 잃는다.
 */
public class SurveyQuestion {

    private final SurveyQuestionKey questionKey;
    private final String title;
    private final int sortOrder;

    private SurveyQuestion(SurveyQuestionKey questionKey, String title, int sortOrder) {
        this.questionKey = Objects.requireNonNull(questionKey, "questionKey");
        this.title = requireText(title);
        this.sortOrder = sortOrder;
    }

    public static SurveyQuestion of(SurveyQuestionKey questionKey, String title, int sortOrder) {
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

    public SurveyQuestionKey getQuestionKey() {
        return questionKey;
    }

    public String getTitle() {
        return title;
    }

    public int getSortOrder() {
        return sortOrder;
    }
}
