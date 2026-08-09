package com.minyaryung.livelab.domain.status;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UnitProgressTest {

    @Test
    void preservesUtf8StatusLabel() {
        assertThat(UnitProgress.CURRENT_LABEL)
                .isEqualTo("U10 AWS 운영 페이지 구축 중 · AI-DLC Construction");
    }

    @Test
    void keepsTestcontainersUnitPlannedUntilIntegrationTestsExist() {
        assertThat(UnitProgress.snapshot()).containsEntry("11", "planned");
    }
}
