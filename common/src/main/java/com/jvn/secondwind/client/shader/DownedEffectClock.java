package com.jvn.secondwind.client.shader;

public final class DownedEffectClock {
    private long startTick = -1L;
    private float startPartialTick;

    public float seconds(long gameTime, float partialTick) {
        if (startTick < 0L || gameTime < startTick) {
            startTick = gameTime;
            startPartialTick = partialTick;
        }
        return Math.max(0.0F, (gameTime - startTick + partialTick - startPartialTick) / 20.0F);
    }

    public void reset() {
        startTick = -1L;
    }
}
