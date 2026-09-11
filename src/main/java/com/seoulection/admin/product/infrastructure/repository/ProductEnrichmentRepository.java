package com.seoulection.admin.product.infrastructure.repository;

import com.seoulection.admin.product.presentation.dto.ProductEnrichmentRequest;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.stereotype.Repository;
import org.bson.Document;
import java.util.*;
import java.util.regex.Pattern;

/** 빈 카탈로그는 필수 이름/브랜드를 요구하는 Product 도메인으로 변환하지 않는다. */
@Repository
public class ProductEnrichmentRepository {
    private final MongoTemplate mongo;
    public ProductEnrichmentRepository(MongoTemplate mongo) { this.mongo = mongo; }
    public static final Map<String, String> FIELDS = Map.of(
            "name", "name", "nameKo", "name_kr", "brand", "brand", "category", "category",
            "description", "description", "price", "price", "productUrl", "product_url", "thumbnailUrl", "thumbnail_url");
    public record Target(String id, String asin, Map<String, Object> values) {
        public Target(String id, String asin) { this(id, asin, Map.of()); }
        public boolean missing(String field) { return empty(values.get(field)); }
    }
    private static boolean empty(Object value) {
        return value == null || value instanceof String text && text.isBlank();
    }
    private Target target(Document doc) {
        Map<String, Object> values = new HashMap<>();
        FIELDS.forEach((field, key) -> {
            Object value = doc.get(key);
            if (value instanceof org.bson.types.Decimal128 decimal) value = decimal.bigDecimalValue();
            if (value != null) values.put(field, value);
        });
        return new Target(doc.get("_id").toString(), doc.getString("asin"), Collections.unmodifiableMap(values));
    }

    static Criteria needsProductInfo() {
        return Criteria.where("status").is("NEED_PRODUCT_INFO");
    }
    public Page<Target> find(String keyword, int page) {
        Criteria criteria = needsProductInfo();
        if (keyword != null && !keyword.isBlank()) {
            String escaped = Pattern.quote(keyword.trim());
            criteria = new Criteria().andOperator(criteria, new Criteria().orOperator(
                    Criteria.where("asin").regex(escaped, "i"), idCriteria(keyword.trim())));
        }
        Query query = new Query(criteria);
        long total = mongo.count(query, "products");
        int last = (int) Math.max(0, (total - 1) / 25);
        Pageable pageable = PageRequest.of(Math.min(Math.max(0, page), last), 25, Sort.by("_id"));
        List<Target> rows = mongo.find(query.with(pageable), Document.class, "products").stream()
                .map(this::target).toList();
        return new PageImpl<>(rows, pageable, total);
    }

    public Map<String, Long> countNeedsProductInfoByCategory() {
        Map<String, Long> counts = new LinkedHashMap<>();
        Document group = new Document("$group", new Document("_id", new Document("$ifNull", List.of("$category", "미분류")))
                .append("count", new Document("$sum", 1)));
        List<Document> stages = List.of(
                new Document("$match", new Document("status", "NEED_PRODUCT_INFO")),
                group,
                new Document("$sort", new Document("_id", 1)));
        mongo.getCollection("products").aggregate(stages)
                .forEach(d -> counts.put(String.valueOf(d.get("_id")), ((Number) d.get("count")).longValue()));
        return counts;
    }
    public Target get(String id) {
        Document doc = mongo.getCollection("products").find(idQuery(id).getQueryObject()).first();
        if (doc == null) throw new IllegalArgumentException("제품을 찾을 수 없거나 이미 정보가 입력된 제품입니다.");
        return target(doc);
    }
    private Criteria idCriteria(String id) {
        return org.bson.types.ObjectId.isValid(id)
                ? new Criteria().orOperator(Criteria.where("_id").is(id), Criteria.where("_id").is(new org.bson.types.ObjectId(id)))
                : Criteria.where("_id").is(id);
    }
    private Query idQuery(String id) {
        return new Query(new Criteria().andOperator(needsProductInfo(), idCriteria(id)));
    }
    public void save(String id, ProductEnrichmentRequest request) {
        Document current = mongo.getCollection("products").find(idQuery(id).getQueryObject()).first();
        if (current == null) throw new IllegalArgumentException("제품 상태가 변경되었습니다. 목록을 새로고침해 주세요.");
        var bean = new org.springframework.beans.BeanWrapperImpl(request);
        Update update = new Update().set("status", "PENDING");
        List<Criteria> unchanged = new ArrayList<>();
        unchanged.add(needsProductInfo());
        unchanged.add(idCriteria(id));
        FIELDS.forEach((field, key) -> {
            Object existing = current.get(key);
            // 읽은 뒤 수집기나 다른 관리자가 값을 바꾸면 갱신 전체를 중단한다.
            unchanged.add(Criteria.where(key).is(existing));
            if (!empty(existing)) return;
            Object value = bean.getPropertyValue(field);
            if (empty(value)) {
                if (!field.equals("nameKo") && !field.equals("description")) throw new IllegalArgumentException("필수 제품 정보가 비어 있습니다. 화면을 새로고침해 주세요.");
                return;
            }
            if (value instanceof java.math.BigDecimal decimal) value = new org.bson.types.Decimal128(decimal);
            update.set(key, value);
        });
        if (mongo.updateFirst(new Query(new Criteria().andOperator(unchanged)), update, "products").getMatchedCount() == 0) {
            throw new IllegalArgumentException("다른 작업에서 제품 정보를 변경했습니다. 새로고침 후 다시 확인해 주세요.");
        }
    }
}
