package com.jvn.secondwind.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class PlayerRepairTest {
    @Test
    void acceptsCorruptedHealthWithoutRequiringThePlayerToBeDowned() {
        for (float health : new float[]{Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, 1.0F, 20.0F}) {
            assertEquals(PlayerRepair.Result.SUCCESS, PlayerRepair.checkVitals(health, 20.0F, 0.0F));
            assertEquals(health == 20.0F ? 20.0F : 12.0F,
                    ReviveHealth.restoredHealth(health, 20.0F, 12.0F, false));
        }
    }

    @Test
    void refusesToRevivePlayersWhoNeedToRespawn() {
        assertEquals(PlayerRepair.Result.NEEDS_RESPAWN, PlayerRepair.checkVitals(0.0F, 20.0F, 0.0F));
        assertEquals(PlayerRepair.Result.NEEDS_RESPAWN, PlayerRepair.checkVitals(-1.0F, 20.0F, 0.0F));
    }

    @Test
    void refusesInvalidMaximumHealthInsteadOfReportingARepair() {
        for (float maxHealth : new float[]{Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, 0.0F, -1.0F}) {
            assertEquals(PlayerRepair.Result.INVALID_MAX_HEALTH, PlayerRepair.checkVitals(Float.NaN, maxHealth, 0.0F));
        }
    }

    @Test
    void rejectsInvalidAbsorptionLimitsBeforeAttemptingRecovery() {
        for (float maxAbsorption : new float[]{Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, -1.0F}) {
            assertEquals(PlayerRepair.Result.INVALID_MAX_ABSORPTION,
                    PlayerRepair.checkVitals(12.0F, 20.0F, maxAbsorption));
        }
        assertEquals(PlayerRepair.Result.SUCCESS, PlayerRepair.checkVitals(12.0F, 20.0F, 0.0F));
    }

    @Test
    void postRepairValidationRejectsUnrepairedOrReintroducedCorruption() {
        for (float invalid : new float[]{Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, -1.0F}) {
            assertFalse(PlayerRepair.hasHealthyVitals(invalid, 20.0F, 0.0F, 4.0F));
            assertFalse(PlayerRepair.hasHealthyVitals(12.0F, 20.0F, invalid, 4.0F));
            assertFalse(PlayerRepair.hasHealthyVitals(12.0F, invalid, 0.0F, 4.0F));
            assertFalse(PlayerRepair.hasHealthyVitals(12.0F, 20.0F, 0.0F, invalid));
        }
        assertFalse(PlayerRepair.hasHealthyVitals(0.0F, 20.0F, 0.0F, 4.0F));
        assertFalse(PlayerRepair.hasHealthyVitals(21.0F, 20.0F, 0.0F, 4.0F));
        assertFalse(PlayerRepair.hasHealthyVitals(12.0F, 20.0F, 5.0F, 4.0F));
        assertTrue(PlayerRepair.hasHealthyVitals(12.0F, 20.0F, 0.0F, 0.0F));
        assertTrue(PlayerRepair.hasHealthyVitals(30.0F, 40.0F, 4.0F, 8.0F));
    }

    @Test
    void respectsModdedMaximumHealth() {
        assertEquals(PlayerRepair.Result.SUCCESS, PlayerRepair.checkVitals(Float.NaN, 2.0F, 0.0F));
        assertEquals(2.0F, ReviveHealth.restoredHealth(Float.NaN, 2.0F, 12.0F, false));
        assertEquals(30.0F, ReviveHealth.restoredHealth(30.0F, 40.0F, 12.0F, false));
    }
}
