package com.seoulection.admin.product.functional.infrastructure;

import com.seoulection.admin.product.functional.application.FunctionalScreeningProperties;
import com.seoulection.admin.product.functional.application.port.MfdsCatalogPort;
import com.seoulection.admin.product.functional.domain.MfdsItem;
import com.seoulection.admin.product.functional.domain.MfdsSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 의약품안전나라(data.go.kr) 기능성화장품 조회 클라이언트.
 *
 * <p>URL을 문자열로 조립해 {@code URI.create}로 넘기는 이유: 서비스 키가 이미 URL 인코딩된
 * 상태로 발급되는데({@code ...%2FXm1Lv...%3D%3D}) {@code RestClient}의 uri 템플릿에 문자열로
 * 넣으면 {@code %2F}가 {@code %252F}로 한 번 더 인코딩돼 403 "등록되지 않은 서비스키"가 난다.
 * 키는 받은 그대로 붙이고, 검색어만 직접 인코딩한다.
 *
 * <p>심사(1471057)와 보고(1471000)는 응답 스키마가 다르다. 심사 쪽엔 효능효과(EE_NAME)가
 * 없어서 기능성 유형을 도출할 수 없다 — 그래서 심사에서만 발견된 제품은 "기능성이긴 하다"까지만
 * 알 수 있고 유형은 사람이 고른다.
 */
@Component
public class MfdsCatalogClient implements MfdsCatalogPort {

    private static final Logger log = LoggerFactory.getLogger(MfdsCatalogClient.class);

    private final RestClient client;
    private final FunctionalScreeningProperties.Mfds config;

    public MfdsCatalogClient(FunctionalScreeningProperties properties) {
        this.config = properties.getMfds();
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(config.getConnectTimeoutMs());
        factory.setReadTimeout(config.getReadTimeoutMs());
        this.client = RestClient.builder().requestFactory(factory).build();
    }

    @Override
    public List<MfdsItem> searchByItemName(String term) {
        if (term == null || term.isBlank()) {
            return List.of();
        }
        List<MfdsItem> items = new ArrayList<>(fetch(config.getReportUrl(), MfdsSource.REPORT, term, 1, 50));
        items.addAll(fetch(config.getExamUrl(), MfdsSource.EXAMINATION, term, 1, 50));
        return items;
    }

    @Override
    public List<MfdsItem> searchBrand(String brandKo) {
        if (brandKo == null || brandKo.isBlank()) {
            return List.of();
        }
        List<MfdsItem> items = new ArrayList<>();
        for (int page = 1; page <= config.getMaxBrandPages(); page++) {
            List<MfdsItem> batch = fetch(config.getReportUrl(), MfdsSource.REPORT, brandKo, page, config.getPageSize());
            items.addAll(batch);
            if (batch.size() < config.getPageSize()) {
                break; // 마지막 페이지다.
            }
        }
        items.addAll(fetch(config.getExamUrl(), MfdsSource.EXAMINATION, brandKo, 1, config.getPageSize()));
        return items;
    }

    @SuppressWarnings("unchecked")
    private List<MfdsItem> fetch(String baseUrl, MfdsSource source, String itemName, int page, int rows) {
        if (config.getServiceKey() == null || config.getServiceKey().isBlank()) {
            throw new IllegalStateException("안전나라 서비스 키가 설정되지 않았습니다(admin.functional-screening.mfds.service-key)");
        }
        String url = baseUrl
                + "?serviceKey=" + config.getServiceKey()
                + "&type=json&pageNo=" + page + "&numOfRows=" + rows
                + "&item_name=" + URLEncoder.encode(itemName, StandardCharsets.UTF_8);

        // ⚠️ uri(String)을 쓰면 안 된다. RestClient가 그걸 URI 템플릿으로 보고 한 번 더 인코딩해서
        //    이미 인코딩된 서비스 키의 %2F가 %252F가 되고 403 "등록되지 않은 서비스키"로 튕긴다.
        //    URI.create로 넘겨 조립한 문자열을 그대로 쓰게 한다.
        Map<String, Object> response = client.get().uri(URI.create(url)).retrieve().body(Map.class);
        Map<String, Object> header = asMap(response == null ? null : response.get("header"));
        String resultCode = header == null ? null : String.valueOf(header.get("resultCode"));
        if (resultCode != null && !"00".equals(resultCode)) {
            // 키 오류·쿼터 초과는 200 + 에러코드로 온다. 조용히 빈 목록으로 넘기면 "기능성 아님"이 된다.
            throw new IllegalStateException("안전나라 응답 오류 " + resultCode + ": " + header.get("resultMsg"));
        }
        Map<String, Object> body = asMap(response == null ? null : response.get("body"));
        Object rawItems = body == null ? null : body.get("items");
        if (!(rawItems instanceof List<?> list)) {
            return List.of();
        }
        List<MfdsItem> items = new ArrayList<>(list.size());
        for (Object element : list) {
            Map<String, Object> row = asMap(element);
            if (row != null) {
                items.add(toItem(source, row));
            }
        }
        log.debug("안전나라 조회 source={} term={} page={} → {}건", source, itemName, page, items.size());
        return items;
    }

    private MfdsItem toItem(MfdsSource source, Map<String, Object> row) {
        return new MfdsItem(
                source,
                text(row.get("ITEM_NAME")),
                text(row.get("ENTP_NAME")),
                text(row.get("EE_CODE")),
                text(row.get("EE_NAME")),
                text(row.get("SPF")),
                text(row.get("PA")),
                text(row.get("COSMETIC_TARGET_FLAG_NAME")),
                // 심사는 허가일, 보고는 보고일. 화면에서는 "등록일" 한 칸으로 보여 준다.
                text(row.get("REPORT_DATE") != null ? row.get("REPORT_DATE") : row.get("ITEM_PERMIT_DATE")),
                "Y".equalsIgnoreCase(text(row.get("CANCEL_APPROVAL_YN"))));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        return value instanceof Map ? (Map<String, Object>) value : null;
    }

    private String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
