package com.seoulection.admin.product.functional.infrastructure;

import com.seoulection.admin.product.domain.enums.ProductFunctionalCategory;
import com.seoulection.admin.product.functional.domain.ClaimReading;
import com.seoulection.admin.product.functional.domain.FunctionalScreening;
import com.seoulection.admin.product.functional.domain.MfdsCandidate;
import com.seoulection.admin.product.functional.domain.MfdsItem;
import com.seoulection.admin.product.functional.domain.MfdsSource;
import com.seoulection.admin.product.functional.domain.ScreeningOutcome;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.List;

/**
 * {@code functional_screenings} 한 건. _id가 곧 product_id다 — 제품당 최신 판정 하나만 둔다.
 *
 * <p>판정 이력을 쌓지 않는 이유: 되짚어야 하는 건 "지금 이 제품이 왜 이렇게 결정됐나"이지
 * 판정이 몇 번 돌았는지가 아니다. 규칙이 바뀐 재판정은 {@code engineVersion}으로 구분한다.
 */
@Document(collection = "functional_screenings")
public class FunctionalScreeningDocument {

    @Id
    private String id;

    private ScreeningOutcome outcome;
    private List<ProductFunctionalCategory> claims;
    private List<Candidate> candidates;

    @Field("selected_index")
    private int selectedIndex;

    private String confidence;
    private String reason;

    @Field("brand_registry_count")
    private long brandRegistryCount;

    @Field("decided_by")
    private String decidedBy;

    @Field("engine_version")
    private String engineVersion;

    @Field("screened_at")
    private Instant screenedAt;

    protected FunctionalScreeningDocument() {
    }

    public static FunctionalScreeningDocument fromDomain(FunctionalScreening screening) {
        FunctionalScreeningDocument document = new FunctionalScreeningDocument();
        document.id = screening.productId();
        document.outcome = screening.outcome();
        document.claims = screening.claims();
        document.candidates = screening.candidates().stream().map(Candidate::fromDomain).toList();
        document.selectedIndex = screening.selectedIndex();
        document.confidence = screening.confidence();
        document.reason = screening.reason();
        document.brandRegistryCount = screening.brandRegistryCount();
        document.decidedBy = screening.decidedBy();
        document.engineVersion = screening.engineVersion();
        document.screenedAt = screening.screenedAt();
        return document;
    }

    public FunctionalScreening toDomain() {
        return new FunctionalScreening(id, outcome,
                claims == null ? List.of() : claims,
                candidates == null ? List.of() : candidates.stream().map(Candidate::toDomain).toList(),
                selectedIndex, confidence, reason, brandRegistryCount, decidedBy, engineVersion, screenedAt);
    }

    /** 후보는 값 그대로 박아 둔다 — 나중에 화면을 열 때 안전나라를 다시 부르지 않기 위해서다. */
    public static class Candidate {
        private String source;
        @Field("item_name")
        private String itemName;
        @Field("entp_name")
        private String entpName;
        @Field("ee_name")
        private String eeName;
        private String spf;
        private String pa;
        @Field("target_flag_name")
        private String targetFlagName;
        @Field("report_date")
        private String reportDate;
        private double score;
        private List<ProductFunctionalCategory> claims;
        @Field("numeric_match")
        private boolean numericMatch;
        @Field("brand_match")
        private boolean brandMatch;
        @Field("out_of_scope")
        private boolean outOfScope;
        private boolean derivable;

        static Candidate fromDomain(MfdsCandidate candidate) {
            MfdsItem item = candidate.item();
            Candidate row = new Candidate();
            row.source = item.source().name();
            row.itemName = item.itemName();
            row.entpName = item.entpName();
            row.eeName = item.eeName();
            row.spf = item.spf();
            row.pa = item.pa();
            row.targetFlagName = item.targetFlagName();
            row.reportDate = item.reportDate();
            row.score = candidate.score();
            row.claims = candidate.claims().categories();
            row.numericMatch = candidate.numericMatch();
            row.brandMatch = candidate.brandMatch();
            row.outOfScope = candidate.claims().outOfScope();
            row.derivable = candidate.claims().derivable();
            return row;
        }

        MfdsCandidate toDomain() {
            MfdsItem item = new MfdsItem(MfdsSource.valueOf(source), itemName, entpName, null, eeName,
                    spf, pa, targetFlagName, reportDate, false);
            return new MfdsCandidate(item, score,
                    new ClaimReading(claims == null ? List.of() : claims, outOfScope, derivable),
                    numericMatch, brandMatch);
        }
    }
}
