package com.seoulection.admin.product.functional.infrastructure;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.StructuredMessageCreateParams;
import com.seoulection.admin.product.functional.application.FunctionalScreeningProperties;
import com.seoulection.admin.product.functional.application.port.CandidateVerdict;
import com.seoulection.admin.product.functional.application.port.ProductNameResolverPort;
import com.seoulection.admin.product.functional.application.port.ScreeningTarget;
import com.seoulection.admin.product.functional.domain.MfdsItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * 이름 문제를 Claude로 푸는 구현. {@code admin.functional-screening.llm.enabled=true}일 때 뜬다.
 *
 * <p><b>이 클래스는 기능성을 판단하지 않는다.</b> 프롬프트 어디에도 "이 제품이 미백인가"를
 * 묻는 곳이 없고, 기능성 유형은 오직 안전나라 응답에서 나온다. 모델에게 맡기는 건 이름 세 가지다 —
 * 브랜드 한글 표기, 등록명 후보, 그리고 "이 후보가 같은 제품인가".
 *
 * <p>실패하면 예외를 던지지 않고 빈 결과를 돌려준다. LLM이 죽었다고 자동화 전체가 멈추면
 * 안 되고, 빈 결과는 곧 "사람이 본다"로 이어지므로 안전한 방향의 실패다.
 */
@Component
@ConditionalOnProperty(name = "admin.functional-screening.llm.enabled", havingValue = "true")
public class ClaudeProductNameResolver implements ProductNameResolverPort {

    private static final Logger log = LoggerFactory.getLogger(ClaudeProductNameResolver.class);

    private final AnthropicClient client;
    private final FunctionalScreeningProperties.Llm config;

    public ClaudeProductNameResolver(FunctionalScreeningProperties properties) {
        this.config = properties.getLlm();
        this.client = AnthropicOkHttpClient.fromEnv(); // ANTHROPIC_API_KEY
    }

    // ── 구조화 출력 스키마 ────────────────────────────────────────────────────

    public record BrandAliases(List<String> koreanNames) { }

    public record RegistrationNames(List<String> names) { }

    /** {@code index}가 Integer인 이유: 모델이 필드를 빠뜨려도 NPE 대신 -1로 흘려보내기 위함. */
    public record Judgement(Integer index, String confidence, String reason) { }

    // ── ① 브랜드 한글 표기 ────────────────────────────────────────────────────

    @Override
    public List<String> koreanBrandAliases(String brand) {
        String prompt = """
                한국 화장품 브랜드의 한글 표기를 알려 주세요.

                브랜드: %s

                의약품안전나라(식약처) 기능성화장품 등록 목록에서 이 브랜드 제품이 어떤 한글 표기로
                올라가 있을지, 가능성이 높은 순으로 최대 3개 적어 주세요.
                예: Goodal → 구달 / d'Alba → 달바 / Round Lab → 라운드랩

                - 한글만 적습니다. 법인명(주식회사 ○○)이 아니라 브랜드 표기입니다.
                - 모르면 빈 배열을 주세요. 지어내지 마세요.
                """.formatted(brand);

        return call(prompt, BrandAliases.class)
                .map(BrandAliases::koreanNames)
                .orElse(List.of());
    }

    // ── ② 등록명 후보 ────────────────────────────────────────────────────────

    @Override
    public List<String> registrationNameCandidates(ScreeningTarget target, String brandKo) {
        String prompt = """
                의약품안전나라 기능성화장품 등록명(ITEM_NAME) 후보를 만들어 주세요.

                브랜드: %s (한글 표기: %s)
                제품명: %s

                등록명은 유통명과 표기가 다릅니다. 공백·기호가 모두 빠진 한 덩어리이고,
                외래어는 한글로 음차됩니다.
                예: "구달 청귤 비타C 잡티 세럼" → 구달청귤비타씨잡티세럼
                    "폴라초이스 10%% 나이아신아마이드 부스터" → 폴라초이스10퍼센트나이아신아마이드부스터
                    "달바 워터풀 톤업 선크림 [SPF50+/PA++++]" → 달바워터풀톤업선크림

                규칙:
                - 브랜드 한글 표기로 시작하는 형태를 반드시 포함하세요.
                - SPF/PA 같은 대괄호 표기와 용량은 뺍니다.
                - 표기가 갈릴 수 있는 부분(비타C/비타씨 등)은 각각 따로 넣어 최대 5개까지.
                - 제품명에 있는 숫자는 절대 바꾸지 마세요.
                """.formatted(target.brand(), brandKo, target.displayName());

        return call(prompt, RegistrationNames.class)
                .map(RegistrationNames::names)
                .orElse(List.of());
    }

    // ── ③ 후보 판정 ──────────────────────────────────────────────────────────

    @Override
    public CandidateVerdict judge(ScreeningTarget target, List<MfdsItem> candidates) {
        if (candidates.isEmpty()) {
            return CandidateVerdict.none("후보가 없습니다");
        }
        String rows = IntStream.range(0, candidates.size())
                .mapToObj(i -> "%d. %s (업체: %s, 등록: %s)".formatted(
                        i, candidates.get(i).itemName(), candidates.get(i).entpName(), candidates.get(i).reportDate()))
                .reduce((a, b) -> a + "\n" + b).orElse("");

        String prompt = """
                아래 제품과 같은 제품인 등록 건이 후보 중에 있는지 판정해 주세요.

                [우리 제품]
                브랜드: %s
                제품명: %s
                카테고리: %s

                [의약품안전나라 등록 후보]
                %s

                판정 기준:
                - 제품명 안의 숫자(311, 77, 50 등)가 다르면 다른 제품입니다. 가장 흔한 오판입니다.
                - 제형이 다르면(세럼 vs 크림 vs 마스크) 다른 제품입니다.
                - 음차 표기 차이(비타C/비타씨, 10%%/10퍼센트)나 중간에 끼는 단어, 뒤에 붙는 (보) 같은
                  꼬리표는 같은 제품일 수 있습니다.
                - 확신이 없으면 confidence를 LOW나 MEDIUM으로 주세요. 억지로 고르지 마세요.

                index: 같은 제품인 후보 번호. 없으면 -1.
                confidence: HIGH | MEDIUM | LOW
                reason: 한국어 한 문장.
                """.formatted(target.brand(), target.displayName(), target.category(), rows);

        return call(prompt, Judgement.class)
                .map(judgement -> new CandidateVerdict(
                        judgement.index() == null ? -1 : judgement.index(),
                        judgement.confidence() == null ? "LOW" : judgement.confidence(),
                        judgement.reason() == null ? "" : judgement.reason()))
                .orElseGet(() -> CandidateVerdict.none("판정 호출에 실패했습니다"));
    }

    // ── 호출 ─────────────────────────────────────────────────────────────────

    private <T> Optional<T> call(String prompt, Class<T> schema) {
        try {
            StructuredMessageCreateParams<T> params = MessageCreateParams.builder()
                    .model(config.getModel())
                    .maxTokens(2048L)
                    .outputConfig(schema)
                    .addUserMessage(prompt)
                    .build();
            return client.messages().create(params).content().stream()
                    .flatMap(block -> block.text().stream())
                    .findFirst()
                    .map(block -> block.text());
        } catch (RuntimeException e) {
            // 여기서 던지면 제품 한 건이 FAILED가 된다. 빈 결과 → 사람 큐가 더 나은 실패다.
            log.warn("Claude 호출 실패 ({}): {}", schema.getSimpleName(), e.toString());
            return Optional.empty();
        }
    }
}
