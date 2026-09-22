package com.jvn.secondwind.common;

public final class ReviveHoldTiming {
    private static final int MIN_GRACE_TICKS = 10;
    private static final int MAX_GRACE_TICKS = 60;

    private ReviveHoldTiming() {
    }

    public static boolean isExpired(long gameTime, long lastHoldGameTime, int latencyMillis) {
        long latencyTicks = (Math.max(0L, latencyMillis) + 49L) / 50L;
        long graceTicks = Math.min(MAX_GRACE_TICKS, MIN_GRACE_TICKS + latencyTicks);
        return gameTime - lastHoldGameTime > graceTicks;
    }
}
