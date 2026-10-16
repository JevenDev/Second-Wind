package com.jvn.secondwind.common;

public final class CooldownPauseClock {
    private long lastTickMillis = -1L;
    private boolean wasPaused;

    public long advance(boolean paused, long epochMillis) {
        long pausedMillis = wasPaused && lastTickMillis >= 0L ? Math.max(0L, epochMillis - lastTickMillis) : 0L;
        lastTickMillis = epochMillis;
        wasPaused = paused;
        return pausedMillis;
    }
}
