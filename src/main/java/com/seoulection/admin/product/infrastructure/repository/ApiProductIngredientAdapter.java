package com.seoulection.admin.product.infrastructure.repository;

import com.seoulection.admin.product.application.dto.ProductIngredientProperty;
import com.seoulection.admin.product.application.dto.ProductIngredientResult;
import com.seoulection.admin.product.application.dto.PropertyDefinitionResult;
import com.seoulection.admin.product.application.port.ProductIngredientPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * seoulection-server-V2 의 product-service 어드민 API 를 부르는 어댑터.
 *
 * <p>{@code admin.product-ingredient.source=api} 일 때만 뜬다. 기본값은 여전히
 * {@link JdbcProductIngredientAdapter} 다 — 전환은 <b>설정 한 줄</b>이고 되돌리기도 같다.
 *
 * <p>🔴 서비스 키가 비어 있으면 V2 가 403 을 준다. 그런데 그 실패는 <b>기동 시점이 아니라
 * 화면을 눌렀을 때</b> 드러난다. api 로 전환할 때 {@code admin.product-service.service-key} 를
 * 반드시 함께 넣을 것.
 */
@Repository
@ConditionalOnProperty(name = "admin.product-ingredient.source", havingValue = "api")
public class ApiProductIngredientAdapter implements ProductIngredientPort {

    private final RestClient client;
    private final String serviceKey;

    public ApiProductIngredientAdapter(
            @Value("${admin.product-service.base-url:http://product-service:8080}") String baseUrl,
            @Value("${admin.product-service.service-key:}") String serviceKey) {
        this.serviceKey = serviceKey;
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        this.client = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    @Override
    public void replace(String productId, List<String> rawNames, String source) {
        if (productId == null) return;
        client.put().uri("/internal/admin/v1/products/{id}/ingredients", productId)
                .header("X-Service-Key", serviceKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("ingredients", rawNames == null ? List.of() : rawNames,
                        "source", source == null ? "ADMIN" : source))
                .retrieve().toBodilessEntity();
    }

    @Override
    public List<ProductIngredientResult> findByProductId(String productId) {
        List<IngredientRow> rows = client.get()
                .uri("/internal/admin/v1/products/{id}/ingredients", productId)
                .header("X-Service-Key", serviceKey)
                .retrieve().body(new ParameterizedTypeReference<List<IngredientRow>>() {});
        return rows == null ? List.of() : rows.stream().map(IngredientRow::toResult).toList();
    }

    @Override
    public void review(String productId, long rowId, String ingredientId, BigDecimal min, BigDecimal max,
                       String unit, String notes, List<ProductIngredientProperty> properties) {
        client.patch().uri("/internal/admin/v1/products/{id}/ingredients/{rowId}", productId, rowId)
                .header("X-Service-Key", serviceKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ReviewBody(ingredientId, min, max, unit, notes, properties))
                .retrieve().toBodilessEntity();
    }

    /**
     * 특성 정의는 아직 V2 에 API 가 없다.
     *
     * <p>빈 목록을 돌려주면 화면에 특성 입력 칸이 하나도 안 뜬다 — "고장" 이 아니라 "입력할 게
     * 없음" 으로 보여서 원인을 찾기 어렵다. V2 에 정의 API 가 생기기 전까지는 api 모드로
     * 전환하지 말 것. 여기서 명시적으로 실패시켜 그 사실을 드러낸다.
     */
    @Override
    public List<PropertyDefinitionResult> propertyDefinitions() {
        throw new UnsupportedOperationException(
                "특성 정의 API가 V2에 아직 없다. admin.product-ingredient.source=jdbc 로 두거나 "
                        + "product-service에 GET /internal/admin/v1/property-definitions 를 먼저 추가할 것.");
    }

    private record ReviewBody(String ingredientId, BigDecimal concentrationMin, BigDecimal concentrationMax,
                              String unit, String notes, List<ProductIngredientProperty> properties) {}

    /** V2 응답 모양. 어드민 DTO 와 필드가 달라(matched* 가 없다) 따로 둔다. */
    private record IngredientRow(long id, String ingredientId, String rawName, Integer order,
                                 BigDecimal concentrationMin, BigDecimal concentrationMax, String unit,
                                 String notes, String source, List<ProductIngredientProperty> properties) {

        ProductIngredientResult toResult() {
            // ⚠️ V2 는 사전의 이름을 함께 주지 않는다 → 매칭 여부만 ingredientId 로 판단한다.
            //    이름까지 보여주려면 V2 응답에 inciName/displayNameKo 를 추가해야 한다.
            return new ProductIngredientResult(id, ingredientId, rawName, order == null ? 0 : order,
                    concentrationMin, concentrationMax, unit, notes, null, null,
                    properties == null ? List.of() : properties);
        }
    }
}
