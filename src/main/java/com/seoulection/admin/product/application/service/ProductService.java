package com.seoulection.admin.product.application.service;

import com.seoulection.admin.product.application.dto.ProductPage;
import com.seoulection.admin.product.application.dto.ProductResult;
import com.seoulection.admin.product.domain.entity.Product;
import com.seoulection.admin.product.domain.enums.ProductFunctionalCategory;
import com.seoulection.admin.product.domain.enums.ProductStatus;
import com.seoulection.admin.product.domain.repository.ProductRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.math.BigDecimal;
import com.seoulection.admin.product.application.dto.ProductIngredientResult;

@Service
public class ProductService {

    /** ObjectId 앞자리에 생성 시각이 들어가므로 _id 내림차순이 곧 최신순이다. */
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "_id");

    private final ProductRepository repository;
    private final com.seoulection.admin.product.application.port.ProductIngredientPort productIngredientRepository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
        this.productIngredientRepository = null;
    }

    @Autowired
    public ProductService(ProductRepository repository,
                           com.seoulection.admin.product.application.port.ProductIngredientPort productIngredientRepository) {
        this.repository = repository;
        this.productIngredientRepository = productIngredientRepository;
    }

    public ProductResult register(String name, String brand, String category) {
        return ProductResult.from(repository.insert(Product.pending(name, brand, category)));
    }

    public ProductResult register(String name, String brand, String category, List<String> ingredients) {
        return register(name, null, brand, category, ingredients);
    }

    /**
     * 신규 등록 — 한글 이름과 전성분까지 함께 받는다.
     *
     * <p>함량은 여기서 받지 않는다. 성분 행이 저장돼야 각 행에 붙일 수 있기 때문이다 —
     * 등록 직후 성분 보완 탭으로 보내 그 자리에서 채우게 한다.
     */
    public ProductResult register(String name, String nameKo, String brand, String category,
                                  List<String> ingredients) {
        ProductResult result = ProductResult.from(
                repository.insert(Product.pending(name, nameKo, brand, category, ingredients)));
        syncProductIngredients(result);
        return result;
    }

    /** 목록 한 페이지. statuses가 비어 있으면 상태 조건 없이 조회한다(전체 탭). */
    public ProductPage getProducts(List<ProductStatus> statuses, String keyword, int page, int size) {
        var found = repository.find(keyword, statuses, PageRequest.of(page, size, NEWEST_FIRST));
        return ProductPage.from(found.map(ProductResult::from));
    }

    /** 탭 배지·소계용 상태별 건수. 검색 중이면 검색 결과 기준으로 센다. */
    public Map<ProductStatus, Long> countByStatus(String keyword) {
        return repository.countByStatus(keyword);
    }

    public ProductResult getProduct(String id) {
        return ProductResult.from(repository.findById(id));
    }

    public ProductResult updateBasicInfo(String id, String name, String nameKo, String brand, String category) {
        Product product = repository.findById(id);
        return ProductResult.from(repository.save(product.updateBasicInfo(name, nameKo, brand, category)));
    }

    public List<ProductIngredientResult> getProductIngredients(String id) {
        repository.findById(id);
        return productIngredientRepository == null ? List.of() : productIngredientRepository.findByProductId(id);
    }

    public void reviewProductIngredient(String productId, long rowId, String ingredientId, BigDecimal min,
                                        BigDecimal max, String unit, String notes,
                                        java.util.List<com.seoulection.admin.product.application.dto.ProductIngredientProperty> properties) {
        repository.findById(productId);
        productIngredientRepository.review(productId, rowId, ingredientId, min, max, unit, notes, properties);
    }

    /** 화면이 특성 입력 칸을 그릴 때 쓰는 정의 목록. */
    public java.util.List<com.seoulection.admin.product.application.dto.PropertyDefinitionResult> propertyDefinitions() {
        return productIngredientRepository == null ? List.of() : productIngredientRepository.propertyDefinitions();
    }

    /** 1단계 검수 — 전성분만 저장한다. 기능성(function)은 그대로 남는다. */
    public ProductResult reviewIngredients(String id, List<String> ingredients, boolean ingredientNotFound) {
        Product product = repository.findById(id);
        ProductResult result = ProductResult.from(repository.save(product.reviewIngredients(ingredients, ingredientNotFound)));
        syncProductIngredients(result);
        return result;
    }

    /** 2단계 검수 — 식약처 기능성만 저장한다. 빈 목록은 "확인했으나 기능성 아님"이다. */
    public ProductResult reviewFunction(String id, List<String> function) {
        Product product = repository.findById(id);
        return ProductResult.from(repository.save(product.reviewFunction(parseFunction(function))));
    }

    public void delete(String id) {
        repository.findById(id); // 없는 id면 여기서 IllegalArgumentException으로 걸린다.
        repository.deleteById(id);
    }

    private List<ProductFunctionalCategory> parseFunction(List<String> values) {
        return values == null ? List.of() : values.stream().filter(Objects::nonNull)
                .map(ProductFunctionalCategory::from).distinct().toList();
    }

    private void syncProductIngredients(ProductResult result) {
        if (productIngredientRepository != null) {
            productIngredientRepository.replace(result.id(), result.ingredients(), result.ingredientSource());
        }
    }
}
