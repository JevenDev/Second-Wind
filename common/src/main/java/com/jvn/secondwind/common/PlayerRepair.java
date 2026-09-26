package com.jvn.secondwind.common;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;

public final class PlayerRepair {
    private PlayerRepair() {
    }

    public enum Result {
        SUCCESS,
        NEEDS_RESPAWN,
        INVALID_MAX_HEALTH
    }

    static Result checkHealth(float health, float maxHealth) {
        if (Float.isFinite(health) && health <= 0.0F) {
            return Result.NEEDS_RESPAWN;
        }
        if (!Float.isFinite(maxHealth) || maxHealth <= 0.0F) {
            return Result.INVALID_MAX_HEALTH;
        }
        return Result.SUCCESS;
    }

    public static Result repairVitals(ServerPlayer player, float configuredHealth) {
        if (player.isRemoved()) {
            return Result.NEEDS_RESPAWN;
        }
        Result result = checkHealth(player.getHealth(), player.getMaxHealth());
        if (result != Result.SUCCESS) {
            SecondWindCommon.LOGGER.warn("Cannot repair player {}: result={}, health={}, maxHealth={}, absorption={}",
                    player.getUUID(), result, player.getHealth(), player.getMaxHealth(), player.getAbsorptionAmount());
            return result;
        }

        SecondWindCommon.LOGGER.info("Repairing player {}: health={}, maxHealth={}, absorption={}",
                player.getUUID(), player.getHealth(), player.getMaxHealth(), player.getAbsorptionAmount());
        ReviveHealth.prepareRecovery(player);
        player.setHealth(ReviveHealth.restoredHealth(player.getHealth(), player.getMaxHealth(), configuredHealth, false));
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
