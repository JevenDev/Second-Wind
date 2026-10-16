package com.jvn.secondwind.client;

import com.jvn.secondwind.network.ClientboundTrackedDownedPlayerPayload;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ClientReviveTimerTest {
    private static final int LOCAL_PLAYER_ID = 81;

    @AfterEach
    void clearTiming() {
        ClientSecondWindState.updateReviveTiming(payload(LOCAL_PLAYER_ID, true, true, 0), LOCAL_PLAYER_ID);
    }

    @Test
    void usesServerDurationForTheLocalPlayer() {
        ClientSecondWindState.updateReviveTiming(payload(LOCAL_PLAYER_ID, true, true, 100), LOCAL_PLAYER_ID);
        assertEquals(100.0F, ClientSecondWindState.reviveTicksRemaining());
        ClientSecondWindState.updateReviveTiming(payload(LOCAL_PLAYER_ID, true, true, 30), LOCAL_PLAYER_ID);
        assertEquals(30.0F, ClientSecondWindState.reviveTicksRemaining());
    }

    @Test
    void otherPlayersCannotChangeLocalReviveTiming() {
        ClientSecondWindState.updateReviveTiming(payload(LOCAL_PLAYER_ID, true, true, 100), LOCAL_PLAYER_ID);
        ClientSecondWindState.updateReviveTiming(payload(92, true, true, 20), LOCAL_PLAYER_ID);
        assertEquals(100.0F, ClientSecondWindState.reviveTicksRemaining());
    }

    @Test
    void interruptionPreservesDurationForTheOverlayFade() {
        ClientSecondWindState.updateReviveTiming(payload(LOCAL_PLAYER_ID, true, true, 100), LOCAL_PLAYER_ID);
        ClientSecondWindState.updateReviveTiming(payload(LOCAL_PLAYER_ID, true, false, 40), LOCAL_PLAYER_ID);
        ClientSecondWindState.updateReviveTiming(payload(LOCAL_PLAYER_ID, false, false, 40), LOCAL_PLAYER_ID);
        assertEquals(100.0F, ClientSecondWindState.reviveTicksRemaining());
    }

    @Test
    void instantAndInvalidDurationsCannotCreateANegativeTimer() {
        ClientSecondWindState.updateReviveTiming(payload(LOCAL_PLAYER_ID, true, true, 0), LOCAL_PLAYER_ID);
        assertEquals(0.0F, ClientSecondWindState.reviveTicksRemaining());
        ClientSecondWindState.updateReviveTiming(payload(LOCAL_PLAYER_ID, true, true, -1), LOCAL_PLAYER_ID);
        assertEquals(0.0F, ClientSecondWindState.reviveTicksRemaining());
    }

    private static ClientboundTrackedDownedPlayerPayload payload(int entityId, boolean downed, boolean paused, int requiredTicks) {
        return new ClientboundTrackedDownedPlayerPayload(entityId, downed, true, 240, 240, paused,
                true, requiredTicks, 2.5D, ResourceLocation.fromNamespaceAndPath("secondwind", "crawl"));
    }
}
