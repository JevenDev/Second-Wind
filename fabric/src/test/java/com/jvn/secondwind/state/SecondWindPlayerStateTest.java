package com.jvn.secondwind.state;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class SecondWindPlayerStateTest {
    @Test
    void autosavedDownedPlayerRetainsUnsafeExitAndDeathMessage() {
        SecondWindPlayerState state = new SecondWindPlayerState();
        state.setDowned(true);
        state.setOriginalDownedDeathMessage("Player was slain by Zombie");
        state.setDownPenaltyCount(2);
        state.setCooldownExpiresEpochMillis(123456L);

        SecondWindPlayerState loaded = roundTrip(state);

        assertTrue(loaded.hasPendingUnsafeExitCooldown());
        assertFalse(loaded.isDowned());
        assertEquals("Player was slain by Zombie", loaded.getOriginalDownedDeathMessage());
        assertEquals(2, loaded.getDownPenaltyCount());
        assertEquals(123456L, loaded.getCooldownExpiresEpochMillis());
    }

    @Test
    void ordinarySaveDoesNotCreateUnsafeExit() {
        assertFalse(roundTrip(new SecondWindPlayerState()).hasPendingUnsafeExitCooldown());
    }

    @Test
    void logoutMarkerSurvivesWithoutRuntimeDownedState() {
        SecondWindPlayerState state = new SecondWindPlayerState();
        state.setPendingUnsafeExitCooldown(true);
        assertTrue(roundTrip(state).hasPendingUnsafeExitCooldown());
    }

    @Test
    void readsExistingFabricSaveFields() {
        SecondWindPlayerState loaded = SecondWindPlayerState.CODEC.parse(JsonOps.INSTANCE,
                JsonParser.parseString("""
                        {"pendingUnsafeExitCooldown":true,"downPenaltyCount":3,
                         "cooldownExpiresGameTime":800,"lastMcDayUsed":4,
                         "consumedToday":true,"consumedSinceSleep":true}
                        """)).getOrThrow();
        assertTrue(loaded.hasPendingUnsafeExitCooldown());
        assertEquals(3, loaded.getDownPenaltyCount());
        assertEquals(0, loaded.getDownPenaltyRecoveryTicks());
        assertEquals(800L, loaded.getCooldownExpiresGameTime());
        assertEquals(4L, loaded.getLastMcDayUsed());
        assertTrue(loaded.hasConsumedToday());
        assertTrue(loaded.hasConsumedSinceSleep());
        assertEquals(0L, loaded.getCooldownExpiresEpochMillis());
    }

    @Test
    void recoveryClearsAllPenaltiesAtTheConfiguredBoundaryWithoutClearingCooldown() {
        SecondWindPlayerState state = new SecondWindPlayerState();
        state.setDownPenaltyCount(3);
        state.setCooldownExpiresGameTime(800L);
        state.setCooldownExpiresEpochMillis(123456L);
        state.setLastMcDayUsed(4L);
        state.setConsumedToday(true);
        state.setConsumedSinceSleep(true);

        state.tickDownPenaltyRecovery(2);
        assertEquals(3, state.getDownPenaltyCount());
        assertEquals(1, state.getDownPenaltyRecoveryTicks());
        state.tickDownPenaltyRecovery(2);
        assertEquals(0, state.getDownPenaltyCount());
        assertEquals(0, state.getDownPenaltyRecoveryTicks());
        assertEquals(800L, state.getCooldownExpiresGameTime());
        assertEquals(123456L, state.getCooldownExpiresEpochMillis());
        assertEquals(4L, state.getLastMcDayUsed());
        assertTrue(state.hasConsumedToday());
        assertTrue(state.hasConsumedSinceSleep());
    }

    @Test
    void zeroDelayClearsExistingPenaltiesAndProgressWithoutClearingCooldown() {
        SecondWindPlayerState state = new SecondWindPlayerState();
        state.setDownPenaltyCount(2);
        state.setCooldownExpiresEpochMillis(123456L);
        state.tickDownPenaltyRecovery(20);
        state.tickDownPenaltyRecovery(0);
        assertEquals(0, state.getDownPenaltyCount());
        assertEquals(0, state.getDownPenaltyRecoveryTicks());
        assertEquals(123456L, state.getCooldownExpiresEpochMillis());
    }

    @Test
    void oneSecondDelayRequiresTwentyEligibleTicks() {
        SecondWindPlayerState state = new SecondWindPlayerState();
        state.setDownPenaltyCount(2);
        for (int tick = 0; tick < 19; tick++) {
            state.tickDownPenaltyRecovery(20);
        }
        assertEquals(2, state.getDownPenaltyCount());
        state.tickDownPenaltyRecovery(20);
        assertEquals(0, state.getDownPenaltyCount());
    }

    @Test
    void zeroDelayWaitsUntilTheDownedAndDeathFlowsEnd() {
        SecondWindPlayerState state = new SecondWindPlayerState();
        state.setDownPenaltyCount(2);
        state.setDowned(true);
        state.tickDownPenaltyRecovery(0);
        assertEquals(2, state.getDownPenaltyCount());
        state.setDowned(false);
        state.setForcedDeathFlow(true);
        state.tickDownPenaltyRecovery(0);
        assertEquals(2, state.getDownPenaltyCount());
        state.setForcedDeathFlow(false);
        state.setPendingUnsafeExitCooldown(true);
        state.tickDownPenaltyRecovery(0);
        assertEquals(2, state.getDownPenaltyCount());
        state.setPendingUnsafeExitCooldown(false);
        state.tickDownPenaltyRecovery(0);
        assertEquals(0, state.getDownPenaltyCount());
    }

    @Test
    void recoveryPausesDuringDownedAndDeathFlows() {
        SecondWindPlayerState state = new SecondWindPlayerState();
        state.setDownPenaltyCount(2);
        state.setDowned(true);
        state.tickDownPenaltyRecovery(1);
        state.setDowned(false);
        state.setForcedDeathFlow(true);
        state.tickDownPenaltyRecovery(1);
        state.setForcedDeathFlow(false);
        state.setPendingUnsafeExitCooldown(true);
        state.tickDownPenaltyRecovery(1);
        assertEquals(2, state.getDownPenaltyCount());
        assertEquals(0, state.getDownPenaltyRecoveryTicks());
    }

    @Test
    void anotherDownRestartsRecovery() {
        SecondWindPlayerState state = new SecondWindPlayerState();
        state.setDownPenaltyCount(2);
        state.tickDownPenaltyRecovery(2);
        state.incrementDownPenaltyCount();
        assertEquals(0, state.getDownPenaltyRecoveryTicks());
        state.tickDownPenaltyRecovery(2);
        assertEquals(3, state.getDownPenaltyCount());
        state.tickDownPenaltyRecovery(2);
        assertEquals(0, state.getDownPenaltyCount());
    }

    @Test
    void recoveryProgressSurvivesSavingAndResumes() {
        SecondWindPlayerState state = new SecondWindPlayerState();
        state.setDownPenaltyCount(2);
        state.tickDownPenaltyRecovery(2);
        SecondWindPlayerState loaded = roundTrip(state);
        assertEquals(1, loaded.getDownPenaltyRecoveryTicks());
        loaded.tickDownPenaltyRecovery(2);
        assertEquals(0, loaded.getDownPenaltyCount());
    }

    @Test
    void manualResetAndRepairClearRecoveryProgress() {
        SecondWindPlayerState state = new SecondWindPlayerState();
        state.setDownPenaltyCount(2);
        state.tickDownPenaltyRecovery(2);
        state.setDownPenaltyCount(0);
        assertEquals(0, state.getDownPenaltyRecoveryTicks());
        state.incrementDownPenaltyCount();
        state.tickDownPenaltyRecovery(2);
        state.resetForRepair();
        assertEquals(0, roundTrip(state).getDownPenaltyRecoveryTicks());
    }

    @Test
    void shorterConfiguredDelayAppliesToExistingProgress() {
        SecondWindPlayerState state = new SecondWindPlayerState();
        state.setDownPenaltyCount(2);
        state.tickDownPenaltyRecovery(20);
        state.tickDownPenaltyRecovery(1);
        assertEquals(0, state.getDownPenaltyCount());
    }

    @Test
    void recoveryDoesNotAccumulateWithoutAPenalty() {
        SecondWindPlayerState state = new SecondWindPlayerState();
        state.tickDownPenaltyRecovery(20);
        assertEquals(0, state.getDownPenaltyRecoveryTicks());
    }

    @Test
    void repairClearsRuntimeAndPersistentRecoveryState() {
        SecondWindPlayerState state = new SecondWindPlayerState();
        state.setDowned(true);
        state.setDownedTicksRemaining(100);
        state.setDownedMaxTicks(200);
        state.setDownedStartGameTime(42L);
        state.setLastDownedDamageGameTime(50L);
        state.setDownedByPlayer(java.util.UUID.randomUUID());
        state.setForcedDeathFlow(true);
        state.setPendingUnsafeExitCooldown(true);
        state.setOriginalDownedDeathMessage("stale death message");
        state.setReviveChannel(java.util.UUID.randomUUID(), 40);
        state.setReviveChannelTicks(20);
        state.setReviveChannelLastHoldGameTime(60L);
        state.setDownPenaltyCount(3);
        state.setCooldownExpiresGameTime(800L);
        state.setCooldownExpiresEpochMillis(123456L);
        state.setLastMcDayUsed(4L);
        state.setConsumedToday(true);
        state.setConsumedSinceSleep(true);

        state.resetForRepair();

        assertFalse(state.isDowned());
        assertFalse(state.isForcedDeathFlow());
        assertEquals(0, state.getDownedTicksRemaining());
        assertEquals(0, state.getDownedMaxTicks());
        assertEquals(0L, state.getDownedStartGameTime());
        assertEquals(0L, state.getLastDownedDamageGameTime());
        assertTrue(state.getDownedByPlayer().isEmpty());
        assertTrue(state.getReviveChannelReviver().isEmpty());
        assertEquals(0, state.getReviveChannelTicks());
        assertEquals(0, state.getReviveChannelRequiredTicks());
        assertEquals(0L, state.getReviveChannelLastHoldGameTime());
        assertNull(state.getOriginalDownedDeathMessage());
        assertNull(state.getOriginalDownedDamageSource());
        SecondWindPlayerState loaded = roundTrip(state);
        assertFalse(loaded.hasPendingUnsafeExitCooldown());
        assertEquals(0, loaded.getDownPenaltyCount());
        assertEquals(0L, loaded.getCooldownExpiresGameTime());
        assertEquals(0L, loaded.getCooldownExpiresEpochMillis());
        assertEquals(-1L, loaded.getLastMcDayUsed());
        assertFalse(loaded.hasConsumedToday());
        assertFalse(loaded.hasConsumedSinceSleep());
    }

    @Test
    void repairAlsoClearsStaleFlagsAfterTheDownedStateHasAlreadyEnded() {
        SecondWindPlayerState state = new SecondWindPlayerState();
        state.setForcedDeathFlow(true);
        state.setPendingUnsafeExitCooldown(true);
        state.setReviveChannel(java.util.UUID.randomUUID(), 40);

        state.resetForRepair();
        state.resetForRepair();

        assertFalse(state.isDowned());
        assertFalse(state.isForcedDeathFlow());
        assertFalse(roundTrip(state).hasPendingUnsafeExitCooldown());
        assertTrue(state.getReviveChannelReviver().isEmpty());
        assertEquals(0, state.getDownPenaltyCount());
    }

    private static SecondWindPlayerState roundTrip(SecondWindPlayerState state) {
        return SecondWindPlayerState.CODEC.parse(JsonOps.INSTANCE,
                SecondWindPlayerState.CODEC.encodeStart(JsonOps.INSTANCE, state).getOrThrow()).getOrThrow();
    }
}
