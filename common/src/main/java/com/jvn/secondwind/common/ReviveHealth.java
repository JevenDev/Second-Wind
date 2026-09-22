package com.jvn.secondwind.common;

public final class ReviveHealth {
    private ReviveHealth() {
    }

    public static float restoredHealth(float currentHealth, float maxHealth, float configuredHealth, boolean regeneration) {
        float targetHealth = Math.min(maxHealth, configuredHealth);
        float initialHealth = regeneration ? Math.max(4.0F, targetHealth * 0.5F) : targetHealth;
        float healthyCurrentHealth = Float.isFinite(currentHealth) ? currentHealth : 0.0F;
        return Math.min(maxHealth, Math.max(healthyCurrentHealth, initialHealth));
    }
}
