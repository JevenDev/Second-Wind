package com.jvn.secondwind.network;

import com.jvn.secondwind.common.SecondWindCommon;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ServerboundGiveUpPayload() implements CustomPacketPayload {
    public static final ServerboundGiveUpPayload INSTANCE = new ServerboundGiveUpPayload();
    public static final Type<ServerboundGiveUpPayload> TYPE =
            new Type<>(SecondWindCommon.id("give_up"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundGiveUpPayload> STREAM_CODEC =
            StreamCodec.of((buffer, payload) -> {
            }, buffer -> INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
