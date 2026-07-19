package com.seoulection.admin.product.application.service;

import com.seoulection.admin.product.application.dto.ProductResult;
import com.seoulection.admin.product.domain.entity.Product;
import com.seoulection.admin.product.domain.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    public ProductResult register(String name, String brand, String category) {
        Product product = Product.pending(name, brand, category);
        return ProductResult.from(repository.insert(product));
    }

    public List<ProductResult> getProducts() {
        return repository.findAll()
                .stream()
                .map(ProductResult::from)
                .toList();
    }
}
