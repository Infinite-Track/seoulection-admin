# 관리자 MongoDB 스키마

첨부된 products/reviews 필드 목록에 맞춘 매핑이다. 다이어그램의 `id`는 MongoDB 기본 키 `_id`에 매핑한다. 별도 `id` 필드를 추가하지 않는다. 실행 중인 DB의 기존 문서를 변경하거나 validator를 설치하는 마이그레이션은 포함하지 않는다.

## products

`_id`, `asin`, `name`, `name_kr`, `brand`, `category`, `description`, `price`, `product_url`, `thumbnail_url`, `mention_count`, `ad_ratio`, `ad_likelihood_sum`, `ingredient_source`, `ingredients`, `inciapi_raw_data`, `analyzed_at`, `function`, `status`.

가격과 광고 비율/합계는 기존 Decimal128 매핑을 유지한다. 분석 시각은 BSON Date, 성분과 기능성은 배열이다. NEED_PRODUCT_INFO 상태에서는 이름·브랜드·카테고리가 null이어도 복원할 수 있다. 신규 수동 등록의 필수 입력 검증은 유지한다.

상태: NEED_PRODUCT_INFO, PENDING, NEED_MANUAL_REVIEW, INSUFFICIENT_INGREDIENTS, INGREDIENTS_ADDED, NOT_FOUND, READY_FOR_INCIAPI, READY_FOR_KEYWORD_ANALYSIS, READY_FOR_ANALYSIS, COMPLETE, SUMMARIZED.

첨부 이미지의 상태 목록 하단이 잘려 있어 기존 COMPLETE/SUMMARIZED는 유지했다. 제품 정보 보완은 NEED_PRODUCT_INFO만 조회하고 저장 완료 시 PENDING으로 전환한다. 일반 목록에도 제품 정보 보완 단계를 제공하며 해당 제품 상세는 입력 페이지로 연결한다.

## reviews

`_id`, `video_id`, `raw_review_id`, `product_name`, `brand`, `category`, `asin`, `ad_likelihood`, `specificity`, `relevance`, `skin_info`, `validator_model`, `validator_prompt_version`, `validated_at`, `status`.

상태: PENDING, NOTFOUND, DISQUALIFIED, COMPLETED. 제품의 NOT_FOUND와 리뷰의 NOTFOUND는 서로 다른 값이다.

이미지에는 타입이 지정되어 있지 않아 점수 필드는 nullable Double, validated_at은 Instant/BSON Date로 모델링했다. skin_info는 구조가 제시되지 않아 Object로 원본을 보존한다. 리뷰 관리 화면/수집 동작을 새로 추가하지 않고 매핑 모델만 추가했다.
