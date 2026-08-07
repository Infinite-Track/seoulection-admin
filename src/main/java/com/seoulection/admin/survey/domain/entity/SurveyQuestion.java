package com.seoulection.admin.survey.domain.entity;

import java.util.regex.Pattern;

/**
 * 설문 문항 — 관리자가 CRUD하는 대상(추가·문구 수정·숨김).
 *
 * <p>{@code questionKey}는 문자열 자연 PK다(api-server와 동일). 예전엔 {@code SurveyQuestionKey} enum이었지만,
 * api-server의 제출 요청이 문항마다 고정 필드를 갖는 구조를 벗어나 문항 목록 형태로 바뀌면서 문항 집합을
 * 코드에 고정할 이유가 사라졌다 — 지금은 {@link SurveyOption}과 마찬가지로 행의 추가·숨김이 데이터만으로 가능하다.
 *
 * <p>삭제는 {@link SurveyOption}과 같은 이유로 {@link #withActive}(soft delete)만 제공한다 — 이미 이 문항으로
 * 답한 {@code survey_answer} 행이 있어, 하드 삭제하면 그 응답이 가리키는 문항 문구를 되찾을 수 없다.
 */
public class SurveyQuestion {

    /** 대문자 스네이크. {@link SurveyOption#getCode()}와 같은 규칙 — api-server가 안정 키로 다룬다. */
    private static final Pattern QUESTION_KEY_FORMAT = Pattern.compile("^[A-Z][A-Z0-9_]*$");

    private final String questionKey;
    private final String title;
    private final int sortOrder;
    private final boolean active;

    private SurveyQuestion(String questionKey, String title, int sortOrder, boolean active) {
        this.questionKey = requireQuestionKey(questionKey);
        this.title = requireText(title);
        this.sortOrder = sortOrder;
        this.active = active;
    }

    /** 새 문항. 활성 상태로 시작한다. */
    public static SurveyQuestion create(String questionKey, String title, int sortOrder) {
        return new SurveyQuestion(questionKey, title, sortOrder, true);
    }

    /** 저장소가 읽어온 기존 행을 복원할 때 쓴다. */
    public static SurveyQuestion of(String questionKey, String title, int sortOrder, boolean active) {
        return new SurveyQuestion(questionKey, title, sortOrder, active);
    }

    public SurveyQuestion withTitle(String newTitle) {
        return new SurveyQuestion(questionKey, newTitle, sortOrder, active);
    }

    /** 노출/숨김. {@code false}가 곧 삭제다(행은 남는다). */
    public SurveyQuestion withActive(boolean newActive) {
        return new SurveyQuestion(questionKey, title, sortOrder, newActive);
    }

    private static String requireQuestionKey(String questionKey) {
        if (questionKey == null || questionKey.isBlank()) {
            throw new IllegalArgumentException("문항 키를 입력해 주세요.");
        }
        String trimmed = questionKey.trim();
        if (!QUESTION_KEY_FORMAT.matcher(trimmed).matches()) {
            throw new IllegalArgumentException(
                    "문항 키는 영문 대문자·숫자·밑줄만 쓸 수 있습니다(예: SKIN_TYPE_CHECK).");
        }
        return trimmed;
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

    public boolean isActive() {
        return active;
    }
}
