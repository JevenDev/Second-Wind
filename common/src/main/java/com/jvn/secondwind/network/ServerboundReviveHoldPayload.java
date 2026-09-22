package com.jvn.secondwind.network;

import com.jvn.secondwind.common.SecondWindCommon;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ServerboundReviveHoldPayload(int targetEntityId) implements CustomPacketPayload {
    public static final int RELEASE_TARGET_ID = -1;
    public static final Type<ServerboundReviveHoldPayload> TYPE =
            new Type<>(SecondWindCommon.id("revive_hold"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundReviveHoldPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.INT, ServerboundReviveHoldPayload::targetEntityId, ServerboundReviveHoldPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
