package com.portfolio.inventory.asset;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AssetSpecificationsTest {

    @Test
    void escapesLikeWildcards() {
        assertThat(AssetSpecifications.escape("50%_off\\")).isEqualTo("50\\%\\_off\\\\");
    }
}
