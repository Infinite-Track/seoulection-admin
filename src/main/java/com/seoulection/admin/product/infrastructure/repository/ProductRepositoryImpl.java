package com.seoulection.admin.product.infrastructure.repository;

import com.seoulection.admin.product.domain.entity.Product;
import com.seoulection.admin.product.domain.repository.ProductRepository;
import com.seoulection.admin.product.infrastructure.document.ProductDocument;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ProductRepositoryImpl implements ProductRepository {

    private final ProductMongoRepository mongoRepository;

    public ProductRepositoryImpl(ProductMongoRepository mongoRepository) {
        this.mongoRepository = mongoRepository;
    }

    @Override
    public Product insert(Product product) {
        return mongoRepository.insert(ProductDocument.fromDomain(product)).toDomain();
    }

    @Override
    public List<Product> findAll() {
        return mongoRepository.findAll(Sort.by(Sort.Direction.DESC, "id"))
                .stream()
                .map(ProductDocument::toDomain)
                .toList();
    }
}
