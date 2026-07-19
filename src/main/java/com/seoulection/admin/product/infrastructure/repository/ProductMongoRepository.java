package com.seoulection.admin.product.infrastructure.repository;

import com.seoulection.admin.product.infrastructure.document.ProductDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

interface ProductMongoRepository extends MongoRepository<ProductDocument, String> {
}
