package com.jvn.secondwind.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class PlayerRepairTest {
    @Test
    void acceptsCorruptedHealthWithoutRequiringThePlayerToBeDowned() {
        for (float health : new float[]{Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, 1.0F, 20.0F}) {
            assertEquals(PlayerRepair.Result.SUCCESS, PlayerRepair.checkHealth(health, 20.0F));
            assertEquals(health == 20.0F ? 20.0F : 12.0F,
                    ReviveHealth.restoredHealth(health, 20.0F, 12.0F, false));
        }
    }

    @Test
    void refusesToRevivePlayersWhoNeedToRespawn() {
        assertEquals(PlayerRepair.Result.NEEDS_RESPAWN, PlayerRepair.checkHealth(0.0F, 20.0F));
        assertEquals(PlayerRepair.Result.NEEDS_RESPAWN, PlayerRepair.checkHealth(-1.0F, 20.0F));
    }

    @Test
    void refusesInvalidMaximumHealthInsteadOfReportingARepair() {
        for (float maxHealth : new float[]{Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, 0.0F, -1.0F}) {
            assertEquals(PlayerRepair.Result.INVALID_MAX_HEALTH, PlayerRepair.checkHealth(Float.NaN, maxHealth));
        }
    }

    @Test
    void respectsModdedMaximumHealth() {
        assertEquals(PlayerRepair.Result.SUCCESS, PlayerRepair.checkHealth(Float.NaN, 2.0F));
        assertEquals(2.0F, ReviveHealth.restoredHealth(Float.NaN, 2.0F, 12.0F, false));
        assertEquals(30.0F, ReviveHealth.restoredHealth(30.0F, 40.0F, 12.0F, false));
    }
}
