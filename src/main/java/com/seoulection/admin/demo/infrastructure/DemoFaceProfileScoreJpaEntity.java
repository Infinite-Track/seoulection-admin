package com.seoulection.admin.demo.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Objects;

/** {@code demo_face_profile_score}(api-server V37) — (code, feature_key) 복합 키. */
@Entity
@Table(name = "demo_face_profile_score")
@IdClass(DemoFaceProfileScoreJpaEntity.Key.class)
public class DemoFaceProfileScoreJpaEntity {
    @Id
    @Column(length = 32)
    private String code;
    @Id
    @Column(name = "feature_key", length = 64)
    private String featureKey;
    @Column(nullable = false)
    private double score;

    protected DemoFaceProfileScoreJpaEntity() {
    }

    public DemoFaceProfileScoreJpaEntity(String code, String featureKey, double score) {
        this.code = code;
        this.featureKey = featureKey;
        this.score = score;
    }

    public String getCode() { return code; }
    public String getFeatureKey() { return featureKey; }
    public double getScore() { return score; }

    public static class Key implements Serializable {
        private String code;
        private String featureKey;

        protected Key() {
        }

        public Key(String code, String featureKey) {
            this.code = code;
            this.featureKey = featureKey;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Key k && Objects.equals(code, k.code) && Objects.equals(featureKey, k.featureKey);
        }

        @Override
        public int hashCode() {
            return Objects.hash(code, featureKey);
        }
    }
}
