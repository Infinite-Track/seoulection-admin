package com.seoulection.admin.demo.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DemoFaceProfileScoreJpaRepository
        extends JpaRepository<DemoFaceProfileScoreJpaEntity, DemoFaceProfileScoreJpaEntity.Key> {
    List<DemoFaceProfileScoreJpaEntity> findAllByOrderByCodeAscFeatureKeyAsc();

    @Modifying
    @Query("delete from DemoFaceProfileScoreJpaEntity s where s.code = :code")
    void deleteByCode(String code);
}
