package com.seoulection.admin.product.functional.application.port;

import com.seoulection.admin.product.functional.domain.FunctionalScreening;

import java.util.Optional;

public interface FunctionalScreeningRepository {

    void save(FunctionalScreening screening);

    Optional<FunctionalScreening> findByProductId(String productId);
}
