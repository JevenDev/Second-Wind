package com.jvn.secondwind.common;

import net.minecraft.world.entity.LivingEntity;

public final class ReviveHealth {
    static final float FORCED_DEATH_HEALTH = -1.0F;

    private ReviveHealth() {
    }

    public static void prepareRecovery(LivingEntity entity) {
        float health = entity.getHealth();
        float maxHealth = entity.getMaxHealth();
        float absorption = entity.getAbsorptionAmount();
        float maxAbsorption = entity.getMaxAbsorption();
        float restoredAbsorption = restoredAbsorption(absorption);
        if (Float.isFinite(maxAbsorption) && maxAbsorption >= 0.0F) {
            restoredAbsorption = Math.min(restoredAbsorption, maxAbsorption);
        }
        if (!Float.isFinite(health) || !Float.isFinite(maxHealth) || maxHealth <= 0.0F
                || absorption != restoredAbsorption) {
            SecondWindCommon.LOGGER.warn("Invalid health state during Second Wind recovery for {}: health={}, maxHealth={}, absorption={}",
                    entity.getUUID(), health, maxHealth, absorption);
        }
        if (absorption != restoredAbsorption) {
            entity.setAbsorptionAmount(restoredAbsorption);
        }
    }

    public static boolean hasValidLimits(float maxHealth, float maxAbsorption) {
        return Float.isFinite(maxHealth) && maxHealth > 0.0F
                && Float.isFinite(maxAbsorption) && maxAbsorption >= 0.0F;
    }

    public static boolean hasHealthyVitals(float health, float maxHealth, float absorption, float maxAbsorption) {
        return hasValidLimits(maxHealth, maxAbsorption)
                && Float.isFinite(health) && health > 0.0F && health <= maxHealth
                && Float.isFinite(absorption) && absorption >= 0.0F && absorption <= maxAbsorption;
    }

    public static boolean trySetHealth(LivingEntity entity, float requestedHealth) {
        if (entity.isRemoved() || !hasValidLimits(entity.getMaxHealth(), entity.getMaxAbsorption())
                || !Float.isFinite(requestedHealth) || requestedHealth <= 0.0F) {
            logInvalidVitals(entity);
            return false;
        }
        prepareRecovery(entity);
        float health = Math.min(requestedHealth, entity.getMaxHealth());
        if (entity.getHealth() != health) {
            entity.setHealth(health);
        }
        if (!hasHealthyVitals(entity.getHealth(), entity.getMaxHealth(), entity.getAbsorptionAmount(), entity.getMaxAbsorption())) {
            logInvalidVitals(entity);
            return false;
        }
        return true;
    }

    public static boolean maintainDownedVitals(LivingEntity entity) {
        return trySetHealth(entity, entity.getHealth());
    }

    public static void setDeathHealth(LivingEntity entity) {
        // a negative input clamps to zero before minecraft consults a potentially NaN maximum
        entity.setHealth(FORCED_DEATH_HEALTH);
    }

    private static void logInvalidVitals(LivingEntity entity) {
        SecondWindCommon.LOGGER.warn("Cannot maintain Second Wind recovery for {}: health={}, maxHealth={}, absorption={}, maxAbsorption={}",
                entity.getUUID(), entity.getHealth(), entity.getMaxHealth(), entity.getAbsorptionAmount(), entity.getMaxAbsorption());
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
