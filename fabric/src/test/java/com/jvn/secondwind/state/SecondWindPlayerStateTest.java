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
        assertEquals(800L, loaded.getCooldownExpiresGameTime());
        assertEquals(4L, loaded.getLastMcDayUsed());
        assertTrue(loaded.hasConsumedToday());
        assertTrue(loaded.hasConsumedSinceSleep());
        assertEquals(0L, loaded.getCooldownExpiresEpochMillis());
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
