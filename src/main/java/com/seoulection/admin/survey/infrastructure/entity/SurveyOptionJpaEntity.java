package com.seoulection.admin.survey.infrastructure.entity;

import com.seoulection.admin.survey.domain.entity.SurveyOption;
import com.seoulection.admin.survey.domain.enums.SurveyQuestionKey;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * {@code survey_option} 영속성 엔티티.
 *
 * <p>⚠️ <b>이 테이블은 api-server가 소유한다</b>(그쪽 {@code SurveyOptionJpaEntity}의 매핑이 곧 DDL).
 * 여기는 {@code ddl-auto=none}으로 붙는 소비자이므로, 컬럼이 어긋나면 기동이 아니라
 * <b>쿼리 실행 시점에</b> 터진다. api-server 쪽 매핑을 바꾸면 이 클래스도 같이 고쳐야 한다.
 */
@Entity
@Table(name = "survey_option")
public class SurveyOptionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_key", nullable = false, length = 32)
    private SurveyQuestionKey questionKey;

    @Column(nullable = false, length = 64, updatable = false)
    private String code;

    @Column(nullable = false)
    private String label;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(nullable = false)
    private boolean exclusive;

    @Column(nullable = false)
    private boolean active;

    protected SurveyOptionJpaEntity() {
    }

    private SurveyOptionJpaEntity(Long id, SurveyQuestionKey questionKey, String code, String label,
                                  int sortOrder, boolean exclusive, boolean active) {
        this.id = id;
        this.questionKey = questionKey;
        this.code = code;
        this.label = label;
        this.sortOrder = sortOrder;
        this.exclusive = exclusive;
        this.active = active;
    }

    public static SurveyOptionJpaEntity fromDomain(SurveyOption option) {
        return new SurveyOptionJpaEntity(option.getId(), option.getQuestionKey(), option.getCode(),
                option.getLabel(), option.getSortOrder(), option.isExclusive(), option.isActive());
    }

    public SurveyOption toDomain() {
        return SurveyOption.of(id, questionKey, code, label, sortOrder, exclusive, active);
    }
}
