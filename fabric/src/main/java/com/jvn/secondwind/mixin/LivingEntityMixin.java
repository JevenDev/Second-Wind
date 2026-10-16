package com.jvn.secondwind.mixin;

import com.jvn.secondwind.config.SecondWindConfig;
import com.jvn.secondwind.state.SecondWindService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
abstract class LivingEntityMixin {
    @Inject(method = "heal", at = @At("HEAD"), cancellable = true)
    private void secondwind$blockDownedHealing(float amount, CallbackInfo callback) {
        if ((Object) this instanceof ServerPlayer player
                && SecondWindConfig.BLOCK_HEALING_WHILE_DOWNED.get()
                && SecondWindService.isDowned(player)) {
            callback.cancel();
        }
    }
}
