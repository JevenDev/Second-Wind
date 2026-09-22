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

    private static SecondWindPlayerState roundTrip(SecondWindPlayerState state) {
        return SecondWindPlayerState.CODEC.parse(JsonOps.INSTANCE,
                SecondWindPlayerState.CODEC.encodeStart(JsonOps.INSTANCE, state).getOrThrow()).getOrThrow();
    }
}
