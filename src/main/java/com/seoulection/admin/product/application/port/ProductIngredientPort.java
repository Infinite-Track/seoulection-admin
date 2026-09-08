package com.seoulection.admin.product.application.port;

import com.seoulection.admin.product.application.dto.ProductIngredientProperty;
import com.seoulection.admin.product.application.dto.ProductIngredientResult;
import com.seoulection.admin.product.application.dto.PropertyDefinitionResult;

import java.math.BigDecimal;
import java.util.List;

/**
 * 제품-성분 데이터에 접근하는 <b>포트</b>.
 *
 * <p><b>왜 인터페이스인가:</b> 지금은 어드민이 PostgreSQL 에 직접 쓴다(V1 구조). 앞으로 이 데이터의
 * 주인은 seoulection-server-V2 의 product-service 가 되고, 어드민은 그 서비스의 어드민 API 를
 * 부르게 된다. 화면과 서비스가 이 포트만 보게 해 두면 <b>전환이 설정 한 줄</b>이 된다:
 *
 * <pre>
 *   admin.product-ingredient.source = jdbc   기본값 — PostgreSQL 직접
 *   admin.product-ingredient.source = api    V2 product-service 호출
 * </pre>
 *
 * <p>🔴 두 구현이 같은 결과를 내는지는 컴파일러가 봐 주지 않는다. 전환 전에 두 어댑터를 같은
 * 시나리오로 돌려 비교할 것 — 설정이 틀려도 앱은 정상 기동하고, 화면을 눌러야 드러난다.
 */
public interface ProductIngredientPort {

    /** 전성분을 통째로 교체한다. 원문을 성분 사전과 대조해 찾으면 연결하고 못 찾으면 비워 둔다. */
    void replace(String productId, List<String> rawNames, String source);

    List<ProductIngredientResult> findByProductId(String productId);

    /**
     * 성분 한 행의 보완 입력.
     *
     * <p>{@code properties} 는 <b>통째 교체</b>다. 부분 갱신으로 두면 "특성을 지웠다"를 표현할
     * 방법이 없다(빈 목록과 미지정을 구분해야 한다). {@code null} 이면 특성은 건드리지 않는다.
     */
    void review(String productId, long rowId, String ingredientId, BigDecimal concentrationMin,
                BigDecimal concentrationMax, String unit, String notes,
                List<ProductIngredientProperty> properties);

    /** 입력 가능한 특성 목록. 화면이 이걸로 입력 칸을 그린다. */
    List<PropertyDefinitionResult> propertyDefinitions();
}
