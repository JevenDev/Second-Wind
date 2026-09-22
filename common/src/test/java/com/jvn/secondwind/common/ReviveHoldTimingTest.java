package com.jvn.secondwind.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ReviveHoldTimingTest {
    @Test
    void toleratesPacketJitterForLowAndHighLatencyRevivers() {
        for (int latency : new int[]{0, 50, 500, 1500}) {
            long lastHold = 100;
            for (long tick = 100; tick < 300; tick++) {
                if (tick % 8 == 0) lastHold = tick;
                assertFalse(ReviveHoldTiming.isExpired(tick, lastHold, latency));
            }
        }
    }

    @Test
    void usesTheReviversLatencyToAllowLongerGaps() {
        assertTrue(ReviveHoldTiming.isExpired(121, 100, 0));
        assertFalse(ReviveHoldTiming.isExpired(121, 100, 1000));
        assertFalse(ReviveHoldTiming.isExpired(130, 100, 1000));
        assertTrue(ReviveHoldTiming.isExpired(131, 100, 1000));
    }

    @Test
    void eventuallyExpiresEvenWithExtremeLatency() {
        assertFalse(ReviveHoldTiming.isExpired(160, 100, Integer.MAX_VALUE));
        assertTrue(ReviveHoldTiming.isExpired(161, 100, Integer.MAX_VALUE));
        assertTrue(ReviveHoldTiming.isExpired(111, 100, -1));
    }
}
