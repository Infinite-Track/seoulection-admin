package com.seoulection.admin.ingredient.infrastructure.repository;

import com.seoulection.admin.ingredient.infrastructure.document.IngredientDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface IngredientMongoRepository extends MongoRepository<IngredientDocument, String> { }
