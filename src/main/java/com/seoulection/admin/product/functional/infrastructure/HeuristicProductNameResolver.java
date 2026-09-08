package com.seoulection.admin.product.functional.infrastructure;

import com.seoulection.admin.product.functional.application.port.CandidateVerdict;
import com.seoulection.admin.product.functional.application.port.ProductNameResolverPort;
import com.seoulection.admin.product.functional.application.port.ScreeningTarget;
import com.seoulection.admin.product.functional.domain.ItemName;
import com.seoulection.admin.product.functional.domain.MfdsItem;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;

/**
 * LLM 없이 도는 대체 구현. {@code admin.functional-screening.llm.enabled=false}(기본)일 때 뜬다.
 *
 * <p>여기까지가 규칙으로 갈 수 있는 한계다. 표기 변환은 몇 가지 흔한 패턴만 처리하고,
 * 후보 판정은 <b>하지 않는다</b> — 애매한 건 전부 사람에게 넘긴다. 규칙이 어설프게 확정하는
 * 것보다 큐에 남는 편이 낫다.
 */
@Component
@ConditionalOnProperty(name = "admin.functional-screening.llm.enabled", havingValue = "false", matchIfMissing = true)
public class HeuristicProductNameResolver implements ProductNameResolverPort {

    /** 한글이 섞여 있으면 그 자체가 등록명에 쓰이는 표기다. 영문뿐이면 음차를 만들 수 없다. */
    @Override
    public List<String> koreanBrandAliases(String brand) {
        return containsHangul(brand) ? List.of(brand.trim()) : List.of();
    }

    @Override
    public List<String> registrationNameCandidates(ScreeningTarget target, String brandKo) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        // 정규화가 기호를 지워 버리므로 표기 변환이 먼저다 — normalize 후엔 "%"가 이미 없다.
        String branded = ItemName.normalize(transliterate(target.brandedName(brandKo)));
        candidates.add(branded);
        candidates.add(ItemName.normalize(transliterate(target.displayName())));

        // 등록명 검색은 부분 일치라 앞부분만 넣어도 걸린다("구달청귤비타" → 구달청귤비타씨잡티세럼).
        // 뒤에 붙는 제형어(세럼·크림)나 표기 차이를 통째로 우회하는 가장 값싼 방법이다.
        for (int length : new int[]{10, 8, 6}) {
            if (branded.length() > length) {
                candidates.add(branded.substring(0, length));
            }
        }
        candidates.removeIf(value -> value == null || value.length() < 2);
        return List.copyOf(candidates);
    }

    /** 규칙만으로는 "311 vs 131"을 가릴 수 없다 — 판정하지 않고 후보만 남긴다. */
    @Override
    public CandidateVerdict judge(ScreeningTarget target, List<MfdsItem> candidates) {
        return CandidateVerdict.none("LLM 판정이 꺼져 있어 후보만 제시합니다");
    }

    /**
     * 등록명에서 반복적으로 관찰되는 표기 차이 몇 가지. 정규화 <b>전에</b> 돌려야 한다.
     *
     * <p>실측 사례: {@code 폴라초이스 10% 나이아신아마이드} → {@code 폴라초이스10퍼센트나이아신아마이드},
     * {@code 구달 청귤 비타C 잡티 세럼} → {@code 구달청귤비타씨잡티세럼}.
     */
    private String transliterate(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replaceAll("(?i)비타\\s*C", "비타씨")
                .replaceAll("(?i)비타민\\s*C", "비타민씨")
                .replace("%", "퍼센트");
    }

    private boolean containsHangul(String value) {
        return value != null && value.chars().anyMatch(ch -> ch >= 0xAC00 && ch <= 0xD7A3);
    }
}
