package com.seoulection.admin.skinpolicy.application;

import com.seoulection.admin.skinpolicy.domain.SkinScorePolicy;
import com.seoulection.admin.skinpolicy.infrastructure.SkinScorePolicyJpaEntity;
import com.seoulection.admin.skinpolicy.infrastructure.SkinScorePolicyJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class SkinScorePolicyServiceTest {
    @Mock SkinScorePolicyJpaRepository repository;
    @InjectMocks SkinScorePolicyService service;

    @Test
    void updatePreservesFeatureLabelAndStoresReliabilities() {
        var current = new SkinScorePolicy("water_score", "Hydration", 0.8, 0.2, 0.7, "v1");
        given(repository.findById("water_score")).willReturn(
                Optional.of(SkinScorePolicyJpaEntity.from(current)));

        service.update("water_score", 0.7, 0.3, 0.6, "skin-score-v2");

        var captor = ArgumentCaptor.forClass(SkinScorePolicyJpaEntity.class);
        then(repository).should().save(captor.capture());
        assertThat(captor.getValue().toDomain()).isEqualTo(
                new SkinScorePolicy("water_score", "Hydration", 0.7, 0.3, 0.6, "skin-score-v2"));
    }

    @Test
    void updateRejectsReliabilityOutsideZeroToOne() {
        var current = new SkinScorePolicy("water_score", "Hydration", 0.8, 0.2, 0.7, "v1");
        given(repository.findById("water_score")).willReturn(
                Optional.of(SkinScorePolicyJpaEntity.from(current)));

        assertThatThrownBy(() -> service.update("water_score", 1.1, 0.2, 0.7, "v2"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("0~1");
    }
}
