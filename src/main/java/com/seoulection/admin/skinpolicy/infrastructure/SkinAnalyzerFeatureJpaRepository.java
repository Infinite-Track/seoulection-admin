package com.seoulection.admin.skinpolicy.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SkinAnalyzerFeatureJpaRepository
        extends JpaRepository<SkinAnalyzerFeatureJpaEntity, SkinAnalyzerFeatureJpaEntity.Key> {

    List<SkinAnalyzerFeatureJpaEntity> findAllByOrderByAnalyzerAscFeatureKeyAsc();

    @Modifying
    @Query("delete from SkinAnalyzerFeatureJpaEntity e where e.analyzer = :analyzer")
    void deleteByAnalyzer(String analyzer);
}
