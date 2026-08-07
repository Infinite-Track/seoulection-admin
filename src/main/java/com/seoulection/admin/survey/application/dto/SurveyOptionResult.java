package com.seoulection.admin.survey.application.dto;

import com.seoulection.admin.survey.domain.entity.SurveyOption;

/** 관리 화면에 뿌릴 선택지 한 줄. */
public record SurveyOptionResult(
        Long id,
        String code,
        String label,
        Integer value,
        int sortOrder,
        boolean exclusive,
        boolean active
) {

    public static SurveyOptionResult from(SurveyOption o) {
        return new SurveyOptionResult(o.getId(), o.getCode(), o.getLabel(), o.getValue(), o.getSortOrder(),
                o.isExclusive(), o.isActive());
    }
}
