package com.seoulection.admin.review.infrastructure.document;

/** reviews의 상태값. products.NOT_FOUND와 달리 NOTFOUND로 저장한다. */
public enum ReviewStatus {
    PENDING, NOTFOUND, DISQUALIFIED, COMPLETED
}
