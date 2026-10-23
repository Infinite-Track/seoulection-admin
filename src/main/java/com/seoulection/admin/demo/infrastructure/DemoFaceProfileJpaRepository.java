package com.seoulection.admin.demo.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DemoFaceProfileJpaRepository extends JpaRepository<DemoFaceProfileJpaEntity, String> {
    List<DemoFaceProfileJpaEntity> findAllByOrderBySortOrderAscCodeAsc();
}
