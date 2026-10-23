package com.seoulection.admin.skinpolicy.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Objects;

/**
 * {@code skin_analyzer_feature} — 분석기마다 "사진만으로 믿어도 되는 지표"(api-server V35). (analyzer, feature_key) 복합 키.
 * 스키마 주인은 api-server, 여기는 {@code ddl-auto=none}. api-server 쪽 매핑을 바꾸면 같이 고친다.
 */
@Entity
@Table(name = "skin_analyzer_feature")
@IdClass(SkinAnalyzerFeatureJpaEntity.Key.class)
public class SkinAnalyzerFeatureJpaEntity {

    @Id
    @Column(nullable = false, length = 32)
    private String analyzer;
    @Id
    @Column(name = "feature_key", nullable = false, length = 64)
    private String featureKey;

    protected SkinAnalyzerFeatureJpaEntity() {
    }

    public SkinAnalyzerFeatureJpaEntity(String analyzer, String featureKey) {
        this.analyzer = analyzer;
        this.featureKey = featureKey;
    }

    public String getAnalyzer() {
        return analyzer;
    }

    public String getFeatureKey() {
        return featureKey;
    }

    public static class Key implements Serializable {
        private String analyzer;
        private String featureKey;

        protected Key() {
        }

        public Key(String analyzer, String featureKey) {
            this.analyzer = analyzer;
            this.featureKey = featureKey;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Key k && Objects.equals(analyzer, k.analyzer) && Objects.equals(featureKey, k.featureKey);
        }

        @Override
        public int hashCode() {
            return Objects.hash(analyzer, featureKey);
        }
    }
}
