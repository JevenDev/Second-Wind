package com.jvn.secondwind.mixin.client;

import com.jvn.secondwind.common.CooldownPauseClock;
import com.jvn.secondwind.state.SecondWindService;
import java.util.function.BooleanSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(IntegratedServer.class)
abstract class IntegratedServerMixin {
    @Unique
    private final CooldownPauseClock secondwind$cooldownPauseClock = new CooldownPauseClock();

    @Inject(method = "tickServer", at = @At("HEAD"))
    private void secondwind$freezeCooldownDuringPause(BooleanSupplier hasTimeLeft, CallbackInfo callback) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean paused = minecraft.isPaused() && minecraft.getConnection() != null;
        long pausedMillis = secondwind$cooldownPauseClock.advance(paused, System.currentTimeMillis());
        if (pausedMillis > 0L) {
            IntegratedServer server = (IntegratedServer) (Object) this;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                SecondWindService.extendCooldownForPause(player, pausedMillis);
            }
        }
    }
}
