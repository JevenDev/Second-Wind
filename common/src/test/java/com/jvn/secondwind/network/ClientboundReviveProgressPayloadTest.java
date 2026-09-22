package com.jvn.secondwind.network;

import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class ClientboundReviveProgressPayloadTest {
    @Test
    void encodesProgressAndInterruptionForTheSameTarget() {
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            ClientboundReviveProgressPayload progress = new ClientboundReviveProgressPayload(81, 25, 40);
            ClientboundReviveProgressPayload interrupted = new ClientboundReviveProgressPayload(81, 0, 40);
            ClientboundReviveProgressPayload rejected = new ClientboundReviveProgressPayload(92, 0, 0);
            ClientboundReviveProgressPayload.STREAM_CODEC.encode(buffer, progress);
            ClientboundReviveProgressPayload.STREAM_CODEC.encode(buffer, interrupted);
            ClientboundReviveProgressPayload.STREAM_CODEC.encode(buffer, rejected);
            assertEquals(progress, ClientboundReviveProgressPayload.STREAM_CODEC.decode(buffer));
            assertEquals(interrupted, ClientboundReviveProgressPayload.STREAM_CODEC.decode(buffer));
            assertEquals(rejected, ClientboundReviveProgressPayload.STREAM_CODEC.decode(buffer));
            assertEquals(0, buffer.readableBytes());
        } finally {
            buffer.release();
        }
    }
}
