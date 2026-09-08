package com.seoulection.admin.product.functional.domain;

import com.seoulection.admin.product.domain.enums.ProductFunctionalCategory;

import java.util.List;

/**
 * 안전나라 한 행에서 읽어 낸 기능성 유형과, 읽어 내지 못한 사유.
 *
 * @param categories  우리 6분류로 옮겨진 유형
 * @param outOfScope  기능성이긴 한데 우리 분류 밖(염모·탈색·제모 등)
 * @param derivable   유형을 도출할 근거가 응답에 있었는가
 */
public record ClaimReading(List<ProductFunctionalCategory> categories, boolean outOfScope, boolean derivable) {

    public static ClaimReading none(boolean outOfScope, boolean derivable) {
        return new ClaimReading(List.of(), outOfScope, derivable);
    }

    /** 이 행만으로 자동 확정해도 되는가. 분류 밖이거나 근거가 없으면 사람이 봐야 한다. */
    public boolean autoConfirmable() {
        return derivable && !outOfScope && !categories.isEmpty();
    }

    public String reason() {
        if (outOfScope) {
            return "기능성이지만 우리 분류 밖(염모·탈색 등)입니다";
        }
        if (!derivable) {
            return "등록 건에 효능효과(EE_NAME)도 SPF/PA도 없어 유형을 도출할 수 없습니다";
        }
        return "";
    }
}
