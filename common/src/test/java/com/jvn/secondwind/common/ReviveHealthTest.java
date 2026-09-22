package com.jvn.secondwind.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ReviveHealthTest {
    @Test
    void recoversInvalidHealthInsteadOfLeavingThePlayerUnableToHealOrDie() {
        for (float health : new float[]{Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, 0.0F, -1.0F}) {
            assertEquals(12.0F, ReviveHealth.restoredHealth(health, 20.0F, 12.0F, false));
            assertEquals(6.0F, ReviveHealth.restoredHealth(health, 20.0F, 12.0F, true));
        }
    }

    @Test
    void preservesExistingHealthAndCapsRecoveryAtMaximumHealth() {
        assertEquals(18.0F, ReviveHealth.restoredHealth(18.0F, 20.0F, 12.0F, true));
        assertEquals(20.0F, ReviveHealth.restoredHealth(30.0F, 20.0F, 12.0F, false));
        assertEquals(2.0F, ReviveHealth.restoredHealth(0.0F, 2.0F, 12.0F, true));
        assertEquals(20.0F, ReviveHealth.restoredHealth(0.0F, 20.0F, 40.0F, false));
    }

    @Test
    void keepsConfiguredHealthAndRegenerationMinimum() {
        assertEquals(1.0F, ReviveHealth.restoredHealth(0.0F, 20.0F, 1.0F, false));
        assertEquals(4.0F, ReviveHealth.restoredHealth(0.0F, 20.0F, 1.0F, true));
    }
}
