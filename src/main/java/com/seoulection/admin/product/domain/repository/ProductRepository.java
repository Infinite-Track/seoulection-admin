package com.seoulection.admin.product.domain.repository;

import com.seoulection.admin.product.domain.entity.Product;

import java.util.List;

public interface ProductRepository {

    Product insert(Product product);

    List<Product> findAll();
}
