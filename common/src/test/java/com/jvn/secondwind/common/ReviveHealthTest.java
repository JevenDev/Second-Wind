package com.jvn.secondwind.common;

import net.minecraft.util.Mth;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class ReviveHealthTest {
    @Test
    void clearsInvalidAbsorptionThatWouldCorruptHealthOnTheNextHit() {
        for (float absorption : new float[]{Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, -1.0F}) {
            assertEquals(0.0F, ReviveHealth.restoredAbsorption(absorption));
            float restoredHealth = ReviveHealth.restoredHealth(Float.NaN, 20.0F, 12.0F, false);
            float damage = 3.0F - Math.min(ReviveHealth.restoredAbsorption(absorption), 3.0F);
            assertEquals(9.0F, restoredHealth - damage);
        }
    }

    @Test
    void preservesValidAbsorptionDuringRecovery() {
        for (float absorption : new float[]{0.0F, 0.5F, 4.0F, 40.0F}) {
            assertEquals(absorption, ReviveHealth.restoredAbsorption(absorption));
        }
    }

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
    void recoveryRequiresValidMaximumHealthAndAbsorption() {
        for (float invalid : new float[]{Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, -1.0F}) {
            assertFalse(ReviveHealth.hasValidLimits(invalid, 0.0F));
            assertFalse(ReviveHealth.hasValidLimits(20.0F, invalid));
        }
        assertFalse(ReviveHealth.hasValidLimits(0.0F, 0.0F));
        assertTrue(ReviveHealth.hasValidLimits(0.5F, 0.0F));
        assertTrue(ReviveHealth.hasValidLimits(40.0F, 8.0F));
    }

    @Test
    void downedVitalsRejectInfiniteHealthAndCorruptedAbsorption() {
        for (float invalid : new float[]{Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, -1.0F}) {
            assertFalse(ReviveHealth.hasHealthyVitals(invalid, 20.0F, 0.0F, 4.0F));
            assertFalse(ReviveHealth.hasHealthyVitals(1.0F, 20.0F, invalid, 4.0F));
        }
        assertFalse(ReviveHealth.hasHealthyVitals(0.0F, 20.0F, 0.0F, 0.0F));
        assertTrue(ReviveHealth.hasHealthyVitals(1.0F, 20.0F, 0.0F, 0.0F));
    }

    @Test
    void forcedDeathClampsToZeroEvenWithCorruptedMaximumHealth() {
        assertTrue(Float.isNaN(Mth.clamp(0.0F, 0.0F, Float.NaN)));
        for (float maxHealth : new float[]{Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, -1.0F, 0.0F, 20.0F}) {
            assertEquals(0.0F, Mth.clamp(ReviveHealth.FORCED_DEATH_HEALTH, 0.0F, maxHealth));
        }
    }

    @Test
    void keepsConfiguredHealthAndRegenerationMinimum() {
        assertEquals(1.0F, ReviveHealth.restoredHealth(0.0F, 20.0F, 1.0F, false));
        assertEquals(4.0F, ReviveHealth.restoredHealth(0.0F, 20.0F, 1.0F, true));
    }
}
