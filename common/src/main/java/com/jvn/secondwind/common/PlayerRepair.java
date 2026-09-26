package com.jvn.secondwind.common;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;

public final class PlayerRepair {
    private PlayerRepair() {
    }

    public enum Result {
        SUCCESS,
        NEEDS_RESPAWN,
        INVALID_MAX_HEALTH,
        INVALID_MAX_ABSORPTION,
        RECOVERY_FAILED
    }

    static Result checkVitals(float health, float maxHealth, float maxAbsorption) {
        if (Float.isFinite(health) && health <= 0.0F) {
            return Result.NEEDS_RESPAWN;
        }
        if (!Float.isFinite(maxHealth) || maxHealth <= 0.0F) {
            return Result.INVALID_MAX_HEALTH;
        }
        if (!Float.isFinite(maxAbsorption) || maxAbsorption < 0.0F) {
            return Result.INVALID_MAX_ABSORPTION;
        }
        return Result.SUCCESS;
    }

    static boolean hasHealthyVitals(float health, float maxHealth, float absorption, float maxAbsorption) {
        return ReviveHealth.hasHealthyVitals(health, maxHealth, absorption, maxAbsorption);
    }

    public static Result repairVitals(ServerPlayer player, float configuredHealth) {
        if (player.isRemoved()) {
            return Result.NEEDS_RESPAWN;
        }
        Result result = checkVitals(player.getHealth(), player.getMaxHealth(), player.getMaxAbsorption());
        if (result != Result.SUCCESS) {
            SecondWindCommon.LOGGER.warn("Cannot repair player {}: result={}, health={}, maxHealth={}, absorption={}, maxAbsorption={}",
                    player.getUUID(), result, player.getHealth(), player.getMaxHealth(), player.getAbsorptionAmount(), player.getMaxAbsorption());
            return result;
        }

        SecondWindCommon.LOGGER.info("Repairing player {}: health={}, maxHealth={}, absorption={}, maxAbsorption={}",
                player.getUUID(), player.getHealth(), player.getMaxHealth(), player.getAbsorptionAmount(), player.getMaxAbsorption());
        ReviveHealth.prepareRecovery(player);
        player.setHealth(ReviveHealth.restoredHealth(player.getHealth(), player.getMaxHealth(), configuredHealth, false));
        if (!hasHealthyVitals(player.getHealth(), player.getMaxHealth(), player.getAbsorptionAmount(), player.getMaxAbsorption())) {
            SecondWindCommon.LOGGER.warn("Player repair did not restore valid vitals for {}: health={}, maxHealth={}, absorption={}, maxAbsorption={}",
                    player.getUUID(), player.getHealth(), player.getMaxHealth(), player.getAbsorptionAmount(), player.getMaxAbsorption());
            return Result.RECOVERY_FAILED;
        }
        player.deathTime = 0;
        player.hurtTime = 0;
        player.invulnerableTime = 0;
        player.setInvulnerable(false);
        player.getAbilities().invulnerable = player.isCreative() || player.isSpectator();
        player.onUpdateAbilities();
        player.fallDistance = 0.0F;
        player.setSwimming(false);
        player.setSprinting(false);
        player.setPose(Pose.STANDING);
        return Result.SUCCESS;
    }
}
