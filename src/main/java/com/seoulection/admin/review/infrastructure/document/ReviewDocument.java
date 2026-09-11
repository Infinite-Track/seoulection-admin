package com.seoulection.admin.review.infrastructure.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import java.time.Instant;

/** 검증 파이프라인의 reviews 문서. skin_info는 원본 구조를 유지한다. */
@Document(collection = "reviews")
public record ReviewDocument(
        @Id String id,
        @Field("video_id") String videoId,
        @Field("raw_review_id") String rawReviewId,
        @Field("product_name") String productName,
        String brand,
        String category,
        String asin,
        @Field("ad_likelihood") Double adLikelihood,
        Double specificity,
        Double relevance,
        @Field("skin_info") Object skinInfo,
        @Field("validator_model") String validatorModel,
        @Field("validator_prompt_version") String validatorPromptVersion,
        @Field("validated_at") Instant validatedAt,
        ReviewStatus status
) {}
