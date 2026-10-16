package com.jvn.secondwind.effect;

import com.jvn.secondwind.SecondWindMod;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;

public final class SecondWindEffects {
    public static final Holder<MobEffect> COOLDOWN = Registry.registerForHolder(
            BuiltInRegistries.MOB_EFFECT,
            ResourceLocation.fromNamespaceAndPath(SecondWindMod.MOD_ID, "cooldown"),
            new SecondWindCooldownEffect());

    private SecondWindEffects() {
    }

    public static void register() {
    }
}
