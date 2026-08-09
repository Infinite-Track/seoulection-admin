package com.seoulection.admin.survey.infrastructure.entity;

import com.seoulection.admin.survey.domain.entity.SurveyQuestion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * {@code survey_question} 영속성 엔티티. 스키마 주인은 api-server다(여기는 {@code ddl-auto=none}).
 *
 * <p>{@code questionKey}가 자연 PK(문자열, enum 아님)라 {@code save()}가 곧 upsert다 —
 * 관리자의 문구 수정이 새 행을 만들지 않는다.
 */
@Entity
@Table(name = "survey_question")
public class SurveyQuestionJpaEntity {

    @Id
    @Column(name = "question_key", nullable = false, length = 32)
    private String questionKey;

    @Column(nullable = false, length = 512)
    private String title;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    /** soft delete 플래그. false면 GET /survey/questions에서 빠진다(api-server SurveyService 참조). */
    @Column(nullable = false)
    private boolean active;

    protected SurveyQuestionJpaEntity() {
    }

    private SurveyQuestionJpaEntity(String questionKey, String title, int sortOrder, boolean active) {
        this.questionKey = questionKey;
        this.title = title;
        this.sortOrder = sortOrder;
        this.active = active;
    }

    public static SurveyQuestionJpaEntity fromDomain(SurveyQuestion question) {
        return new SurveyQuestionJpaEntity(question.getQuestionKey(), question.getTitle(),
                question.getSortOrder(), question.isActive());
    }

    public SurveyQuestion toDomain() {
        return SurveyQuestion.of(questionKey, title, sortOrder, active);
    }
}
