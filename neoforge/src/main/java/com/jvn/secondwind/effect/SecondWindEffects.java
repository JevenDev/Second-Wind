package com.jvn.secondwind.effect;

import com.jvn.secondwind.SecondWindMod;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class SecondWindEffects {
    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, SecondWindMod.MOD_ID);
    public static final Holder<MobEffect> COOLDOWN = EFFECTS.register("cooldown", SecondWindCooldownEffect::new);

    private SecondWindEffects() {
    }

    public static void register(IEventBus modEventBus) {
        EFFECTS.register(modEventBus);
    }
}
