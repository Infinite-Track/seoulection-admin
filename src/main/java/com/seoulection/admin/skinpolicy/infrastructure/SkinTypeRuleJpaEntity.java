package com.seoulection.admin.skinpolicy.infrastructure;

import com.seoulection.admin.skinpolicy.domain.SkinTypeRule;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * {@code survey_skin_type_rule} — 한 행(id = 1). 스키마 주인은 api-server(V34), 여기는 {@code ddl-auto=none}.
 * api-server 쪽 매핑을 바꾸면 이 클래스도 같이 고쳐야 한다(어긋나면 쿼리 시점에 터진다).
 */
@Entity
@Table(name = "survey_skin_type_rule")
public class SkinTypeRuleJpaEntity {

    public static final short SINGLETON_ID = 1;

    @Id
    private Short id;
    @Column(name = "t_zone_question_key", nullable = false, length = 32)
    private String tZoneQuestionKey;
    @Column(name = "cheek_question_key", nullable = false, length = 32)
    private String cheekQuestionKey;
    @Column(name = "oily_max", nullable = false)
    private int oilyMax;
    @Column(name = "clear_min", nullable = false)
    private int clearMin;
    @Column(name = "dry_water_max", nullable = false)
    private int dryWaterMax;
    @Column(name = "prefer_self_declared", nullable = false, columnDefinition = "boolean default false")
    private boolean preferSelfDeclared;
    @Column(name = "updated_at", nullable = false, columnDefinition = "timestamp(6) with time zone default now()")
    private Instant updatedAt;

    protected SkinTypeRuleJpaEntity() {
    }

    private SkinTypeRuleJpaEntity(SkinTypeRule rule) {
        this.id = SINGLETON_ID;
        this.tZoneQuestionKey = rule.tZoneQuestionKey();
        this.cheekQuestionKey = rule.cheekQuestionKey();
        this.oilyMax = rule.oilyMax();
        this.clearMin = rule.clearMin();
        this.dryWaterMax = rule.dryWaterMax();
        this.preferSelfDeclared = rule.preferSelfDeclared();
        this.updatedAt = Instant.now();
    }

    public static SkinTypeRuleJpaEntity from(SkinTypeRule rule) {
        return new SkinTypeRuleJpaEntity(rule);
    }

    public SkinTypeRule toDomain() {
        return new SkinTypeRule(tZoneQuestionKey, cheekQuestionKey, oilyMax, clearMin, dryWaterMax, preferSelfDeclared);
    }
}
