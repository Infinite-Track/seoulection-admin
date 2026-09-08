package com.seoulection.admin.survey.infrastructure.entity;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * {@code survey_question_evidence} 영속성 엔티티. 스키마 주인은 api-server 다(여기는 ddl-auto=none).
 *
 * <p>설문 문항의 근거 — "이 문항을 왜 묻는가". 문항 하나에 여러 근거가 붙는다(1:N).
 */
@Entity
@Table(name = "survey_question_evidence")
public class SurveyQuestionEvidenceJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "question_key", nullable = false, length = 32)
    private String questionKey;

    @Column(nullable = false, length = 300)
    private String title;

    /** 🔴 이 필드가 이 테이블의 목적이다. 링크를 열지 않고도 "왜 묻는지"를 알 수 있어야 한다. */
    @Column(nullable = false, columnDefinition = "text")
    private String rationale;

    /** 내부 자료는 링크가 없을 수 있다. */
    @Column(length = 1000)
    private String url;

    @Column(name = "source_type", nullable = false, length = 30)
    private String sourceType;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    protected SurveyQuestionEvidenceJpaEntity() { }

    public SurveyQuestionEvidenceJpaEntity(String questionKey, String title, String rationale,
                                           String url, String sourceType, Integer sortOrder) {
        this.questionKey = questionKey;
        this.title = title;
        this.rationale = rationale;
        this.url = url;
        this.sourceType = sourceType;
        this.sortOrder = sortOrder == null ? 0 : sortOrder;
    }

    public void update(String title, String rationale, String url, String sourceType, Integer sortOrder) {
        this.title = title;
        this.rationale = rationale;
        this.url = url;
        this.sourceType = sourceType;
        this.sortOrder = sortOrder == null ? 0 : sortOrder;
    }

    public Long getId() { return id; }
    public String getQuestionKey() { return questionKey; }
    public String getTitle() { return title; }
    public String getRationale() { return rationale; }
    public String getUrl() { return url; }
    public String getSourceType() { return sourceType; }
    public Integer getSortOrder() { return sortOrder; }
    public Instant getCreatedAt() { return createdAt; }
}
