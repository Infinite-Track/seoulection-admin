package com.seoulection.admin.trending;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class TrendingServiceTest {
    @Test void percentile95UsesContinuousInterpolation() {
        assertThat(TrendingService.percentile95(List.of(0, 1, 2, 3, 100))).isCloseTo(80.6, org.assertj.core.data.Offset.offset(0.000001));
    }

    @Test void percentile95OfEmptySetIsZero() {
        assertThat(TrendingService.percentile95(List.of())).isZero();
    }
}
