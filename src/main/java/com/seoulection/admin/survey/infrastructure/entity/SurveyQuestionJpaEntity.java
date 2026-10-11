package com.seoulection.admin.survey.infrastructure.entity;

import com.seoulection.admin.survey.domain.entity.SurveyQuestion;
import com.seoulection.admin.survey.domain.entity.SurveyQuestionRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * {@code survey_question} 영속성 엔티티. 스키마 주인은 api-server다(여기는 {@code ddl-auto=none}).
 *
 * <p>{@code questionKey}가 자연 PK(문자열, enum 아님)라 {@code save()}가 곧 upsert다 —
 * 관리자의 문구 수정이 새 행을 만들지 않는다.
 *
 * <p>api-server에는 {@code tier} 컬럼도 있지만 여기서는 매핑하지 않는다 — INSERT 시 DB 기본값('CORE')이
 * 들어가고, UPDATE는 매핑한 컬럼만 바꾼다. 지표 매핑 세 컬럼(V30)은 어드민이 편집하므로 매핑한다.
 * {@code columnDefinition}의 기본값은 테스트(이 매핑으로 스키마를 만든다)와 운영(V30)이 같게 두기 위한 것.
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

    /** {@code skin_score_policy.feature_key} FK(운영). 비면 점수에 쓰이지 않는다. */
    @Column(name = "feature_key", length = 64)
    private String featureKey;

    @Column(nullable = false, columnDefinition = "integer default 100")
    private int weight;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16, columnDefinition = "varchar(16) default 'SCORE'")
    private SurveyQuestionRole role;

    protected SurveyQuestionJpaEntity() {
    }

    private SurveyQuestionJpaEntity(String questionKey, String title, int sortOrder, boolean active,
                                    String featureKey, int weight, SurveyQuestionRole role) {
        this.questionKey = questionKey;
        this.title = title;
        this.sortOrder = sortOrder;
        this.active = active;
        this.featureKey = featureKey;
        this.weight = weight;
        this.role = role;
    }

    public static SurveyQuestionJpaEntity fromDomain(SurveyQuestion question) {
        return new SurveyQuestionJpaEntity(question.getQuestionKey(), question.getTitle(),
                question.getSortOrder(), question.isActive(), question.getFeatureKey(), question.getWeight(),
                question.getRole());
    }

    public SurveyQuestion toDomain() {
        return SurveyQuestion.of(questionKey, title, sortOrder, active, featureKey, weight,
                role == null ? SurveyQuestionRole.SCORE : role);
    }
}
