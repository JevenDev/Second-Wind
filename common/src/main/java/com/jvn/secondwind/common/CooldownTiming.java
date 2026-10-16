package com.jvn.secondwind.common;

public final class CooldownTiming {
    public static final int TICKS_PER_SECOND = 20;
    public static final long TICKS_PER_MC_DAY = 24000L;

    private CooldownTiming() {
    }

    public static int remainingTimedTicks(long expiresEpochMillis, long epochMillis, long expiresGameTime, long gameTime) {
        long ticks;
        if (expiresEpochMillis > 0L) {
            long millis = Math.max(0L, expiresEpochMillis - epochMillis);
            ticks = millis / 50L + (millis % 50L == 0L ? 0L : 1L);
        } else {
            ticks = Math.max(0L, expiresGameTime - gameTime);
        }
        return (int) Math.min(Integer.MAX_VALUE, ticks);
    }

    public static long extendExpiryForPause(long expiresEpochMillis, long pausedMillis) {
        if (expiresEpochMillis <= 0L) {
            return 0L;
        }
        return expiresEpochMillis + Math.min(Long.MAX_VALUE - expiresEpochMillis, Math.max(0L, pausedMillis));
    }

    public static int remainingDayTicks(long dayTime) {
        return (int) (TICKS_PER_MC_DAY - Math.floorMod(dayTime, TICKS_PER_MC_DAY));
    }

    public static int remainingSeconds(int ticks) {
        return ticks < 0 ? -1 : (int) ((ticks + (TICKS_PER_SECOND - 1L)) / TICKS_PER_SECOND);
    }
}
