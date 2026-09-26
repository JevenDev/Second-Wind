package com.jvn.secondwind.common;

import net.minecraft.world.entity.LivingEntity;

public final class ReviveHealth {
    private ReviveHealth() {
    }

    public static void prepareRecovery(LivingEntity entity) {
        float health = entity.getHealth();
        float maxHealth = entity.getMaxHealth();
        float absorption = entity.getAbsorptionAmount();
        float restoredAbsorption = restoredAbsorption(absorption);
        if (!Float.isFinite(health) || !Float.isFinite(maxHealth) || maxHealth <= 0.0F
                || absorption != restoredAbsorption) {
            SecondWindCommon.LOGGER.warn("Invalid health state during Second Wind recovery for {}: health={}, maxHealth={}, absorption={}",
                    entity.getUUID(), health, maxHealth, absorption);
        }
        if (absorption != restoredAbsorption) {
            entity.setAbsorptionAmount(restoredAbsorption);
        }
    }

    public static float restoredAbsorption(float absorption) {
        return Float.isFinite(absorption) && absorption >= 0.0F ? absorption : 0.0F;
    }

    public static float restoredHealth(float currentHealth, float maxHealth, float configuredHealth, boolean regeneration) {
        float targetHealth = Math.min(maxHealth, configuredHealth);
        float initialHealth = regeneration ? Math.max(4.0F, targetHealth * 0.5F) : targetHealth;
        float healthyCurrentHealth = Float.isFinite(currentHealth) ? currentHealth : 0.0F;
        return Math.min(maxHealth, Math.max(healthyCurrentHealth, initialHealth));
    }
}
