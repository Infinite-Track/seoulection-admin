package com.seoulection.admin.product.infrastructure.repository;

import com.seoulection.admin.product.domain.entity.Product;
import com.seoulection.admin.product.domain.enums.ProductStatus;
import com.seoulection.admin.product.domain.repository.ProductRepository;
import com.seoulection.admin.product.infrastructure.document.ProductDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 파생 쿼리(findByXxx) 대신 MongoTemplate을 쓰는 이유: 목록은 검색어 × 상태 필터 × 페이지의
 * 조합이라 메서드 이름으로 표현하면 금방 감당이 안 된다. 조건을 Criteria로 조립하면 조합이
 * 늘어도 메서드 하나로 끝난다.
 */
@Repository
public class ProductRepositoryImpl implements ProductRepository {

    /** 정규식 메타문자. 검색어를 그대로 regex에 넣으면 "("만 쳐도 쿼리가 깨진다. */
    private static final Pattern REGEX_META = Pattern.compile("[\\\\^$.|?*+()\\[\\]{}]");

    private final MongoTemplate mongoTemplate;

    public ProductRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Product insert(Product product) {
        return mongoTemplate.insert(ProductDocument.fromDomain(product)).toDomain();
    }

    @Override
    public Product findById(String id) {
        ProductDocument found = mongoTemplate.findById(id, ProductDocument.class);
        if (found == null) {
            throw new IllegalArgumentException("제품을 찾을 수 없습니다: " + id);
        }
        return found.toDomain();
    }

    @Override
    public Product save(Product product) {
        return mongoTemplate.save(ProductDocument.fromDomain(product)).toDomain();
    }

    @Override
    public void deleteById(String id) {
        mongoTemplate.remove(new Query(Criteria.where("_id").is(id)), ProductDocument.class);
    }

    @Override
    public Page<Product> find(String keyword, List<ProductStatus> statuses, Pageable pageable) {
        Query query = new Query();
        criteria(keyword, statuses).forEach(query::addCriteria);
        long total = mongoTemplate.count(query, ProductDocument.class);
        List<Product> content = mongoTemplate.find(query.with(pageable), ProductDocument.class)
                .stream()
                .map(ProductDocument::toDomain)
                .toList();
        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public Map<ProductStatus, Long> countByStatus(String keyword) {
        List<Criteria> criteria = criteria(keyword, List.of());
        List<AggregationOperation> stages = new ArrayList<>();
        criteria.forEach(c -> stages.add(Aggregation.match(c)));
        stages.add(Aggregation.group("status").count().as("count"));

        Map<ProductStatus, Long> counts = new EnumMap<>(ProductStatus.class);
        mongoTemplate.aggregate(Aggregation.newAggregation(stages), ProductDocument.class, StatusCount.class)
                .forEach(row -> {
                    if (row.id() != null) {
                        counts.merge(ProductStatus.valueOf(row.id()), row.count(), Long::sum);
                    }
                });
        return counts;
    }

    private List<Criteria> criteria(String keyword, List<ProductStatus> statuses) {
        List<Criteria> criteria = new ArrayList<>();
        if (keyword != null && !keyword.isBlank()) {
            String escaped = REGEX_META.matcher(keyword.trim()).replaceAll("\\\\$0");
            criteria.add(new Criteria().orOperator(
                    Criteria.where("name").regex(escaped, "i"),
                    Criteria.where("brand").regex(escaped, "i")));
        }
        if (statuses != null && !statuses.isEmpty()) {
            criteria.add(Criteria.where("status").in(statuses));
        }
        return criteria;
    }

    /** group 단계의 결과 한 줄. _id에 status 문자열이, count에 건수가 들어온다. */
    private record StatusCount(String id, long count) {
    }
}
