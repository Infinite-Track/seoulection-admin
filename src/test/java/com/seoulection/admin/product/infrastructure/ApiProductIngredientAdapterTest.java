package com.seoulection.admin.product.infrastructure;

import com.seoulection.admin.product.application.dto.ProductIngredientResult;
import com.seoulection.admin.product.infrastructure.repository.ApiProductIngredientAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * API 어댑터가 product-service 응답을 JDBC 어댑터와 <b>같은 모양</b>으로 옮기는지 본다.
 *
 * <p>왜 이 테스트인가: 두 어댑터를 실제로 나란히 돌리려면 Postgres 와 product-service 가 둘 다
 * 떠 있어야 한다. 그런데 실제로 깨지는 지점은 대부분 그게 아니라 <b>필드 이름 어긋남</b>이다 —
 * V2 가 응답 필드를 바꾸면 여기서 조용히 null 이 되고, 화면에는 빈 칸으로만 보인다.
 * 그 어긋남은 HTTP 를 흉내 내는 것만으로 잡을 수 있다.
 */
class ApiProductIngredientAdapterTest {

    private static final String BASE_URL = "http://product-service:8080";

    @Test
    @DisplayName("★ product-service 응답을 화면이 쓰는 결과로 옮긴다 — 필드 이름이 어긋나면 여기서 걸린다")
    void mapsResponseToResult() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        var adapter = new ApiProductIngredientAdapter(builder, BASE_URL, "secret");

        server.expect(requestTo(BASE_URL + "/internal/admin/v1/products/p1/ingredients"))
                .andExpect(header("X-Service-Key", "secret"))
                .andRespond(withSuccess("""
                        [
                          {"id": 7, "ingredientId": "ing-1", "rawName": "Sodium Hyaluronate", "order": 1,
                           "concentrationMin": 1.0, "concentrationMax": 2.5, "unit": "%",
                           "notes": "저분자", "source": "ADMIN",
                           "inciName": "SODIUM HYALURONATE", "displayNameKo": "소듐하이알루로네이트",
                           "properties": [
                             {"propertyKey": "PURITY", "displayNameKo": "순도", "valueText": null,
                              "valueMin": 99, "valueMax": 99, "valueUnit": "%", "notes": null}
                           ]},
                          {"id": 8, "ingredientId": null, "rawName": "Unknown Extract", "order": 2,
                           "concentrationMin": null, "concentrationMax": null, "unit": null,
                           "notes": null, "source": "PIPELINE",
                           "inciName": null, "displayNameKo": null, "properties": []}
                        ]
                        """, MediaType.APPLICATION_JSON));

        List<ProductIngredientResult> rows = adapter.findByProductId("p1");

        assertThat(rows).hasSize(2);

        ProductIngredientResult matched = rows.get(0);
        assertThat(matched.id()).isEqualTo(7);
        assertThat(matched.matched()).isTrue();
        assertThat(matched.rawName()).isEqualTo("Sodium Hyaluronate");
        assertThat(matched.order()).isEqualTo(1);
        assertThat(matched.concentrationMin()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(matched.unit()).isEqualTo("%");
        // 사전 이름이 오면 화면은 한글명을 쓴다 — JDBC 어댑터와 같은 규칙이다.
        assertThat(matched.displayName()).isEqualTo("소듐하이알루로네이트");
        assertThat(matched.properties()).singleElement()
                .satisfies(property -> {
                    assertThat(property.propertyKey()).isEqualTo("PURITY");
                    assertThat(property.display()).isEqualTo("99 %");
                });

        // 사전에 없는 성분은 오류가 아니다. 원문을 그대로 보여 준다.
        ProductIngredientResult unmatched = rows.get(1);
        assertThat(unmatched.matched()).isFalse();
        assertThat(unmatched.displayName()).isEqualTo("Unknown Extract");
        assertThat(unmatched.properties()).isEmpty();

        server.verify();
    }

    @Test
    @DisplayName("특성 정의는 V2에 API가 없다 — 빈 목록 대신 명시적으로 실패한다")
    void propertyDefinitionsFailsLoudly() {
        RestClient.Builder builder = RestClient.builder();
        var adapter = new ApiProductIngredientAdapter(builder, BASE_URL, "secret");

        // 빈 목록을 주면 화면에 특성 칸이 안 뜨는데 그게 "고장"이 아니라 "입력할 게 없음"으로
        // 보인다. 원인을 찾기 어려우므로 여기서 터뜨린다.
        assertThatThrownBy(adapter::propertyDefinitions)
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("property-definitions");
    }
}
