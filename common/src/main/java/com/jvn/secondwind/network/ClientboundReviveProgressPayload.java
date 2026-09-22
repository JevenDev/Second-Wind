package com.jvn.secondwind.network;

import com.jvn.secondwind.common.SecondWindCommon;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ClientboundReviveProgressPayload(int targetEntityId, int completedTicks, int requiredTicks) implements CustomPacketPayload {
    public static final Type<ClientboundReviveProgressPayload> TYPE = new Type<>(SecondWindCommon.id("revive_progress"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundReviveProgressPayload> STREAM_CODEC =
            StreamCodec.of(ClientboundReviveProgressPayload::write, ClientboundReviveProgressPayload::read);

    private static void write(RegistryFriendlyByteBuf buffer, ClientboundReviveProgressPayload payload) {
        buffer.writeVarInt(payload.targetEntityId);
        buffer.writeVarInt(payload.completedTicks);
        buffer.writeVarInt(payload.requiredTicks);
    }

    private static ClientboundReviveProgressPayload read(RegistryFriendlyByteBuf buffer) {
        return new ClientboundReviveProgressPayload(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
