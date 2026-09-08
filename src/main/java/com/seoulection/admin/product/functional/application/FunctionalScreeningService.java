package com.seoulection.admin.product.functional.application;

import com.seoulection.admin.product.application.dto.ProductResult;
import com.seoulection.admin.product.application.service.ProductService;
import com.seoulection.admin.product.domain.enums.ProductFunctionalCategory;
import com.seoulection.admin.product.domain.enums.ProductStatus;
import com.seoulection.admin.product.functional.application.port.CandidateVerdict;
import com.seoulection.admin.product.functional.application.port.FunctionalScreeningRepository;
import com.seoulection.admin.product.functional.application.port.MfdsCatalogPort;
import com.seoulection.admin.product.functional.application.port.ProductNameResolverPort;
import com.seoulection.admin.product.functional.application.port.ScreeningTarget;
import com.seoulection.admin.product.functional.domain.FunctionalScreening;
import com.seoulection.admin.product.functional.domain.ItemName;
import com.seoulection.admin.product.functional.domain.MfdsCandidate;
import com.seoulection.admin.product.functional.domain.MfdsItem;
import com.seoulection.admin.product.functional.domain.ScreeningOutcome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 기능성 자동 판정의 오케스트레이션. 판정 규칙이 흩어지지 않도록 <b>결정은 전부 여기서</b>
 * 내리고, 조회(안전나라)와 이름 해석(LLM)은 포트에 맡긴다.
 *
 * <p>조회 순서가 이 모양인 이유:
 * <ol>
 *   <li>정규화한 브랜드+제품명으로 바로 검색 — 되는 건 여기서 끝난다(실측 35%).</li>
 *   <li>등록명 후보로 재검색 — {@code 비타C→비타씨} 같은 표기 차이를 넘는다.</li>
 *   <li>브랜드 전수 조회 — 등록명이 아무리 달라도 그 브랜드 목록 안에는 들어 있다.
 *       회수율의 나머지 절반이 여기서 나온다. 덤으로 "브랜드 등록 0건"이라는 음성 근거와
 *       업체명(ENTP_NAME) 기준값을 얻는다.</li>
 * </ol>
 */
@Service
public class FunctionalScreeningService {

    private static final Logger log = LoggerFactory.getLogger(FunctionalScreeningService.class);

    /** 자동으로 "기능성 아님"을 확정하면 안 되는 카테고리. 선크림은 법적으로 기능성이어야 한다. */
    private static final List<String> FUNCTIONAL_BY_LAW_CATEGORIES = List.of("sunscreens");

    /** 이름에 이게 들어 있으면 기능성일 공산이 크다 — 후보 0건이어도 검색 실패로 본다. */
    private static final List<String> FUNCTIONAL_HINTS =
            List.of("선크림", "선스틱", "선쿠션", "선세럼", "자차", "톤업", "미백", "주름", "브라이트닝",
                    "sun", "spf", "uv", "whitening", "brightening", "wrinkle");

    private final ProductService productService;
    private final MfdsCatalogPort mfdsCatalog;
    private final ProductNameResolverPort nameResolver;
    private final FunctionalScreeningRepository repository;
    private final FunctionalScreeningProperties properties;

    /** 브랜드 한글 표기는 브랜드 단위로만 달라진다 — 제품마다 LLM을 부르지 않기 위한 캐시. */
    private final Map<String, List<String>> brandAliasCache = new java.util.concurrent.ConcurrentHashMap<>();

    public FunctionalScreeningService(ProductService productService,
                                      MfdsCatalogPort mfdsCatalog,
                                      ProductNameResolverPort nameResolver,
                                      FunctionalScreeningRepository repository,
                                      FunctionalScreeningProperties properties) {
        this.productService = productService;
        this.mfdsCatalog = mfdsCatalog;
        this.nameResolver = nameResolver;
        this.repository = repository;
        this.properties = properties;
    }

    public Optional<FunctionalScreening> find(String productId) {
        return repository.findByProductId(productId);
    }

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    /** 자동 판정이 상태까지 옮기는 모드인가. 화면 문구와 이동 경로가 이걸로 갈린다. */
    public boolean appliesDecisions() {
        return properties.isApplyDecisions();
    }

    /**
     * <b>자동화의 진입점.</b> 어드민이 한글 이름을 저장한 직후에 불린다.
     *
     * <p>왜 하필 여기인가: 등록명(ITEM_NAME)은 전부 한글이고 브랜드 한글 표기로 시작한다.
     * 영문 제품명만으로는 조회가 시작조차 안 되고, 한글 이름이 채워진 그 순간이 자동 조회가
     * 가장 잘 듣는 시점이다. 그래서 4단계 마법사의 "한글 이름" 저장이 곧 기능성 조회 트리거다.
     *
     * <p>결과가 나오면 기능성까지 확정돼 다음 단계로 넘어가고, 못 찾으면 기능성 폼이 열린 채
     * 후보만 채워진다 — 그때만 사람이 고른다.
     */
    public Optional<FunctionalScreening> screenAfterNameSaved(String productId) {
        if (!properties.isEnabled()) {
            return Optional.empty();
        }
        return Optional.of(screen(productId));
    }

    /**
     * 화면에서 쓰는 조회 — 판정이 없으면 그때 한 번 돌린다.
     *
     * <p>한글 이름이 없으면 돌리지 않는다. 등록명이 전부 한글이라 영문명으로는 어차피 0건이고,
     * 그 0건이 "검색 결과 없음"으로 기록되면 사람이 잘못된 근거를 보게 된다.
     */
    public Optional<FunctionalScreening> findOrScreen(String productId) {
        if (!properties.isEnabled()) {
            return Optional.empty();
        }
        Optional<FunctionalScreening> saved = repository.findByProductId(productId);
        if (saved.isPresent()) {
            return saved;
        }
        ProductResult product = productService.getProduct(productId);
        if (product.nameKo() == null || product.nameKo().isBlank()) {
            return Optional.empty();
        }
        return Optional.of(screen(product));
    }

    /**
     * 어드민이 직접 저장했음을 판정 기록에 남긴다.
     *
     * <p>{@code decidedBy}를 남기는 이유: 기능성은 규제 정보라 "이 제품이 왜 이렇게 기록됐나"를
     * 되짚을 수 있어야 하고, 자동이 틀렸던 건들을 모아 규칙을 고칠 때도 이 표시가 열쇠가 된다.
     */
    public void markDecidedByAdmin(String productId) {
        repository.findByProductId(productId).ifPresent(screening -> repository.save(
                new FunctionalScreening(screening.productId(), screening.outcome(), screening.claims(),
                        screening.candidates(), screening.selectedIndex(), screening.confidence(),
                        screening.reason(), screening.brandRegistryCount(),
                        FunctionalScreening.DECIDED_BY_ADMIN, screening.engineVersion(), Instant.now())));
    }

    /**
     * 기능성 확인 큐를 한 번에 훑는다. 목록 화면의 [자동 조회 실행] 버튼과 스케줄러가 부른다.
     *
     * @return 결말별 건수
     */
    public Map<ScreeningOutcome, Integer> screenQueue(int limit) {
        Map<ScreeningOutcome, Integer> summary = new LinkedHashMap<>();
        var page = productService.getProducts(List.of(ProductStatus.INGREDIENTS_ADDED), null, 0, Math.max(limit, 1));
        for (ProductResult product : page.content()) {
            if (product.nameKo() == null || product.nameKo().isBlank()) {
                continue; // 한글 이름이 없으면 조회할 근거가 없다.
            }
            FunctionalScreening screening = screen(product);
            summary.merge(screening.outcome(), 1, Integer::sum);
        }
        return summary;
    }

    public FunctionalScreening screen(String productId) {
        return screen(productService.getProduct(productId));
    }

    public FunctionalScreening screen(ProductResult product) {
        ScreeningTarget target = ScreeningTarget.from(product);
        FunctionalScreening screening;
        try {
            screening = decide(target);
        } catch (RuntimeException e) {
            // 조회·판정 실패는 "기능성 아님"이 아니다. 사유만 남기고 큐에 그대로 둔다.
            log.warn("기능성 자동 판정 실패 productId={}", target.id(), e);
            screening = FunctionalScreening.failed(target.id(), "조회 중 오류: " + e.getMessage());
        }
        repository.save(screening);
        applyIfDecided(product, screening);
        return screening;
    }

    // ── 판정 ────────────────────────────────────────────────────────────────

    private FunctionalScreening decide(ScreeningTarget target) {
        List<String> aliases = brandAliases(target.brand());
        String brandKo = aliases.isEmpty() ? target.brand() : aliases.get(0);

        // 브랜드 전수: 등록 목록 + 업체명 기준값 + "0건"이라는 음성 근거를 한 번에 준다.
        //
        // ⚠️ 첫 별칭에서 멈추지 않는다. 표기가 갈리는 브랜드가 실제로 있고("아누아"/"어누아",
        //    "넘버즈인"/"넘버즈인"), 한 표기로 몇 건 나왔다고 나머지를 안 보면 정작 맞는 제품이
        //    다른 표기 아래 있을 때 통째로 놓친다. 별칭마다 조회해 합친다.
        List<MfdsItem> brandItems = new ArrayList<>();
        int bestHits = 0;
        for (String alias : aliases) {
            List<MfdsItem> found = mfdsCatalog.searchBrand(alias);
            brandItems.addAll(found);
            // 이름 비교의 기준이 될 표기는 가장 많이 걸린 것으로 둔다 — 그게 실제 등록 표기다.
            if (found.size() > bestHits) {
                bestHits = found.size();
                brandKo = alias;
            }
        }
        String brandEntpName = dominantEntpName(brandItems);

        List<MfdsItem> pool = new ArrayList<>(brandItems);
        for (String term : searchTerms(target, brandKo)) {
            pool.addAll(mfdsCatalog.searchByItemName(term));
        }

        long brandCount = brandItems.size();
        List<MfdsCandidate> scored = score(pool, target, brandKo, brandEntpName);
        if (scored.isEmpty()) {
            // 브랜드 전수를 한글 표기로 못 돌렸으면 "0건"이 음성 근거가 되지 못한다 —
            // 등록명은 전부 한글이라 영문 브랜드명으로 조회하면 무조건 0건이 나온다.
            return noCandidate(target, brandCount, containsHangul(brandKo));
        }

        // 유사도만으로 확실한 것들. 여기서 끝나면 판정을 부르지 않는다(호출 비용을 아낀다).
        List<MfdsCandidate> strong = scored.stream()
                .filter(candidate -> candidate.score() >= properties.getCandidateThreshold()
                        || candidate.coverage() >= properties.getCoverageThreshold())
                .limit(properties.getMaxCandidates())
                .toList();

        if (!strong.isEmpty() && strong.get(0).confirmable(properties.getAutoThreshold())
                && !strong.get(0).partialNameMatch()) {
            return confirmed(target, strong, 0, "등록명이 거의 일치합니다", brandCount);
        }

        // ⚠️ 판정에 <b>임계값을 통과한 것만</b> 넘기지 않는다. 유사도는 표기가 크게 다른 제품을
        //    통째로 떨어뜨린다 — "토리든 다이브인 저분자 히알루론산 세럼"은 이 브랜드 등록 2건과
        //    모두 0.5 미만이라, 예전에는 판정이 아예 호출되지 않고 화면엔 "후보 없음"만 떴다.
        //    브랜드 등록 목록을 점수순으로 넉넉히 넘기고 <b>고르는 일을 판정에 맡긴다</b>.
        List<MfdsCandidate> reviewed = scored.stream().limit(properties.getJudgePoolSize()).toList();
        CandidateVerdict verdict = nameResolver.judge(target, reviewed.stream().map(MfdsCandidate::item).toList());
        List<MfdsCandidate> shown = reviewed.stream().limit(properties.getMaxCandidates()).toList();

        if (!verdict.matched()) {
            // 판정이 "없다"고 했다. 유사도 높은 후보가 있었으면 사람이 다시 볼 값어치가 있고,
            // 그것마저 없으면 이 브랜드에 이 제품의 등록이 없다는 쪽에 가깝다. 어느 쪽이든
            // 무엇을 보고 그렇게 판단했는지는 화면에 남긴다.
            ScreeningOutcome outcome = strong.isEmpty() ? ScreeningOutcome.NOT_MATCHED : ScreeningOutcome.NEEDS_REVIEW;
            String head = strong.isEmpty()
                    ? "이 브랜드 등록 " + brandCount + "건 중 같은 제품을 찾지 못했습니다"
                    : "같은 제품으로 볼 후보가 없습니다";
            return new FunctionalScreening(target.id(), outcome, List.of(), shown, -1,
                    verdict.confidence(), head + ": " + verdict.reason(),
                    brandCount, FunctionalScreening.DECIDED_BY_AUTO, FunctionalScreening.ENGINE_VERSION, Instant.now());
        }

        int index = Math.min(Math.max(verdict.index(), 0), reviewed.size() - 1);
        MfdsCandidate chosen = reviewed.get(index);
        // 판정이 고른 건이 표시 범위 밖일 수 있다(20개 중 15번째를 골랐다면). 맨 앞에 세운다.
        List<MfdsCandidate> withChosen = new ArrayList<>();
        withChosen.add(chosen);
        shown.stream().filter(candidate -> candidate != chosen)
                .limit(Math.max(properties.getMaxCandidates() - 1, 0))
                .forEach(withChosen::add);

        if (verdict.high() && chosen.numericMatch() && chosen.brandMatch() && chosen.claims().autoConfirmable()) {
            return confirmed(target, withChosen, 0, verdict.reason(), brandCount);
        }
        String block = chosen.blockReason();
        return new FunctionalScreening(target.id(), ScreeningOutcome.NEEDS_REVIEW, chosen.claims().categories(),
                withChosen, 0, verdict.confidence(),
                block.isBlank() ? "판정 신뢰도가 낮아 확인이 필요합니다: " + verdict.reason() : block,
                brandCount, FunctionalScreening.DECIDED_BY_AUTO, FunctionalScreening.ENGINE_VERSION, Instant.now());
    }

    private FunctionalScreening confirmed(ScreeningTarget target, List<MfdsCandidate> candidates,
                                          int index, String reason, long brandCount) {
        return new FunctionalScreening(target.id(), ScreeningOutcome.AUTO_CONFIRMED,
                candidates.get(index).claims().categories(), candidates, index, CandidateVerdict.HIGH, reason,
                brandCount, FunctionalScreening.DECIDED_BY_AUTO, FunctionalScreening.ENGINE_VERSION, Instant.now());
    }

    /**
     * 후보가 하나도 없을 때. 여기가 자동화에서 가장 조심할 지점이다 — <b>검색 실패와 기능성
     * 아님은 겉보기가 같다</b>. 브랜드 등록이 0건일 때만 둘을 가를 수 있다.
     */
    private FunctionalScreening noCandidate(ScreeningTarget target, long brandCount, boolean brandLookupReliable) {
        boolean brandAbsent = brandCount == 0 && brandLookupReliable;
        boolean lawRequiresFunctional = FUNCTIONAL_BY_LAW_CATEGORIES.contains(target.category());
        boolean hintsFunctional = hasFunctionalHint(target);

        if (properties.isAutoConcludeNone() && brandAbsent && !lawRequiresFunctional && !hintsFunctional) {
            return new FunctionalScreening(target.id(), ScreeningOutcome.AUTO_NONE, List.of(), List.of(), -1,
                    CandidateVerdict.HIGH, "이 브랜드의 기능성 등록이 안전나라에 한 건도 없습니다",
                    0, FunctionalScreening.DECIDED_BY_AUTO, FunctionalScreening.ENGINE_VERSION, Instant.now());
        }

        String reason;
        if (lawRequiresFunctional) {
            reason = "자외선 차단 제품은 기능성 등록이 있어야 합니다 — 조회 실패로 보고 직접 확인해 주세요";
        } else if (hintsFunctional) {
            reason = "제품명이 기능성을 암시하는데 등록 건을 찾지 못했습니다 — 직접 확인해 주세요";
        } else if (brandAbsent) {
            reason = "이 브랜드의 기능성 등록이 한 건도 없습니다(기능성 아님일 가능성이 높습니다)";
        } else if (!brandLookupReliable) {
            reason = "브랜드 한글 표기를 몰라 전수 조회를 못 했습니다 — 등록 여부를 직접 확인해 주세요";
        } else {
            reason = "브랜드 등록은 " + brandCount + "건 있으나 이 제품과 맞는 건을 찾지 못했습니다";
        }
        return new FunctionalScreening(target.id(), ScreeningOutcome.NOT_MATCHED, List.of(), List.of(), -1,
                "LOW", reason, brandCount, FunctionalScreening.DECIDED_BY_AUTO,
                FunctionalScreening.ENGINE_VERSION, Instant.now());
    }

    /** 자동 확정을 실제 상태 전진으로 옮긴다. 그림자 모드면 판정만 남기고 상태는 그대로 둔다. */
    private void applyIfDecided(ProductResult product, FunctionalScreening screening) {
        if (!properties.isApplyDecisions() || !screening.outcome().decided()) {
            return;
        }
        if (product.status() != ProductStatus.INGREDIENTS_ADDED) {
            return; // 이미 사람이 손댔거나 파이프라인이 지나간 제품은 건드리지 않는다.
        }
        productService.reviewFunction(product.id(),
                screening.claims().stream().map(ProductFunctionalCategory::code).toList());
    }

    // ── 조회 재료 ────────────────────────────────────────────────────────────

    private List<String> brandAliases(String brand) {
        return brandAliasCache.computeIfAbsent(brand, key -> {
            List<String> aliases = new ArrayList<>(nameResolver.koreanBrandAliases(key));
            if (!aliases.contains(key)) {
                aliases.add(key);
            }
            return aliases.stream().filter(value -> value != null && !value.isBlank())
                    .distinct().limit(properties.getMaxBrandAliases()).toList();
        });
    }

    private List<String> searchTerms(ScreeningTarget target, String brandKo) {
        LinkedHashSet<String> terms = new LinkedHashSet<>();
        terms.add(ItemName.normalize(target.brandedName(brandKo)));
        terms.add(ItemName.normalize(target.displayName()));
        nameResolver.registrationNameCandidates(target, brandKo).stream()
                .map(ItemName::normalize)
                .forEach(terms::add);
        terms.removeIf(term -> term.length() < 2);
        // 상한을 두는 건 안전나라 쿼터 때문이지 정확도 때문이 아니다 — 후보가 많을수록 회수율은
        // 올라간다. 기본 8개는 "정규화 2 + 모델이 만든 표기 변형 여러 개"를 담는 크기다.
        return terms.stream().limit(properties.getMaxSearchTerms()).toList();
    }

    /**
     * 브랜드 등록 목록에서 가장 많이 나온 업체명. 후보의 {@code ENTP_NAME}을 견주는 기준값이다.
     *
     * <p>LLM에게 법인명을 물어보지 않는 이유가 여기 있다 — 안전나라가 직접 알려주는 값이라
     * 모델의 기억보다 정확하다.
     */
    private String dominantEntpName(List<MfdsItem> brandItems) {
        return brandItems.stream()
                .map(MfdsItem::entpName)
                .filter(name -> name != null && !name.isBlank())
                .collect(java.util.stream.Collectors.groupingBy(name -> name, java.util.stream.Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    /**
     * 취하 건을 걸러 내고 중복을 합친 뒤 점수순으로 세운다. <b>임계값은 보지 않는다.</b>
     *
     * <p>거르는 일을 여기서 하지 않는 이유: 유사도는 표기가 크게 다른 제품을 통째로 떨어뜨린다.
     * 무엇을 후보로 볼지는 호출하는 쪽이 정한다.
     */
    private List<MfdsCandidate> score(List<MfdsItem> pool, ScreeningTarget target,
                                      String brandKo, String brandEntpName) {
        String query = target.brandedName(brandKo);
        Map<String, MfdsCandidate> unique = new LinkedHashMap<>();
        for (MfdsItem item : pool) {
            if (item.canceled()) {
                continue; // 취하된 등록은 근거가 되지 못한다.
            }
            MfdsCandidate candidate = MfdsCandidate.of(item, query, brandKo, brandEntpName);
            String key = item.source() + "|" + ItemName.normalize(item.itemName()) + "|" + item.entpName();
            unique.merge(key, candidate,
                    (existing, incoming) -> existing.claims().categories().size() >= incoming.claims().categories().size()
                            ? existing : incoming);
        }
        return unique.values().stream()
                .sorted(Comparator.comparingDouble(MfdsCandidate::score).reversed())
                .toList();
    }

    private boolean containsHangul(String value) {
        return value != null && value.chars().anyMatch(ch -> ch >= 0xAC00 && ch <= 0xD7A3);
    }

    private boolean hasFunctionalHint(ScreeningTarget target) {
        String haystack = (target.name() + " " + (target.nameKo() == null ? "" : target.nameKo())).toLowerCase();
        return FUNCTIONAL_HINTS.stream().anyMatch(haystack::contains);
    }
}
