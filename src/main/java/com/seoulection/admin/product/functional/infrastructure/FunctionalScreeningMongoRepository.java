package com.seoulection.admin.product.functional.infrastructure;

import com.seoulection.admin.product.functional.application.port.FunctionalScreeningRepository;
import com.seoulection.admin.product.functional.domain.FunctionalScreening;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class FunctionalScreeningMongoRepository implements FunctionalScreeningRepository {

    private final MongoTemplate mongoTemplate;

    public FunctionalScreeningMongoRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void save(FunctionalScreening screening) {
        mongoTemplate.save(FunctionalScreeningDocument.fromDomain(screening));
    }

    @Override
    public Optional<FunctionalScreening> findByProductId(String productId) {
        return Optional.ofNullable(mongoTemplate.findById(productId, FunctionalScreeningDocument.class))
                .map(FunctionalScreeningDocument::toDomain);
    }
}
