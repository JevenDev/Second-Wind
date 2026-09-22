package com.jvn.secondwind.client.shader;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class DownedEffectClockTest {
    @Test
    void elapsedTimeIsIndependentOfFrameRate() {
        for (int framesPerSecond : new int[] {30, 60, 144}) {
            DownedEffectClock clock = new DownedEffectClock();
            clock.seconds(1000L, 0.0F);
            float elapsed = 0.0F;
            for (int frame = 1; frame <= framesPerSecond * 10; frame++) {
                double ticks = frame * 20.0D / framesPerSecond;
                elapsed = clock.seconds(1000L + (long) ticks, (float) (ticks - (long) ticks));
            }
            assertEquals(10.0F, elapsed, 0.0001F);
        }
    }

    @Test
    void pausedFramesDoNotAdvanceTheClock() {
        DownedEffectClock clock = new DownedEffectClock();
        clock.seconds(1000L, 0.25F);
        assertEquals(1.0F, clock.seconds(1020L, 0.25F));
        for (int frame = 0; frame < 120; frame++) {
            assertEquals(1.0F, clock.seconds(1020L, 0.25F));
        }
    }

    @Test
    void resetAndEarlierWorldTimeRestartTheEffect() {
        DownedEffectClock clock = new DownedEffectClock();
        clock.seconds(1000L, 0.5F);
        clock.reset();
        assertEquals(0.0F, clock.seconds(2000L, 0.75F));
        assertEquals(0.0F, clock.seconds(10L, 0.25F));
    }
}
