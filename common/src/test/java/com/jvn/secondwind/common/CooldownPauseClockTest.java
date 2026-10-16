package com.jvn.secondwind.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class CooldownPauseClockTest {
    @Test
    void excludesPausedTimeThroughTheFirstResumedTick() {
        CooldownPauseClock clock = new CooldownPauseClock();
        assertEquals(0L, clock.advance(false, 1000L));
        assertEquals(0L, clock.advance(true, 1050L));
        assertEquals(950L, clock.advance(true, 2000L));
        assertEquals(50L, clock.advance(false, 2050L));
        assertEquals(0L, clock.advance(false, 2100L));
    }

    @Test
    void activeServerLagAndMultiplayerMenusDoNotExtendCooldown() {
        CooldownPauseClock clock = new CooldownPauseClock();
        assertEquals(0L, clock.advance(false, 1000L));
        assertEquals(0L, clock.advance(false, 10000L));
        assertEquals(0L, clock.advance(false, 60000L));
    }

    @Test
    void pausedCooldownKeepsItsRemainingTimeAndResumesNormally() {
        CooldownPauseClock clock = new CooldownPauseClock();
        long expiry = 11000L;
        clock.advance(false, 1000L);
        clock.advance(true, 2000L);
        expiry = CooldownTiming.extendExpiryForPause(expiry, clock.advance(true, 32000L));
        assertEquals(180, CooldownTiming.remainingTimedTicks(expiry, 32000L, 0L, 0L));
        expiry = CooldownTiming.extendExpiryForPause(expiry, clock.advance(false, 33000L));
        assertEquals(180, CooldownTiming.remainingTimedTicks(expiry, 33000L, 0L, 0L));
        expiry = CooldownTiming.extendExpiryForPause(expiry, clock.advance(false, 34000L));
        assertEquals(160, CooldownTiming.remainingTimedTicks(expiry, 34000L, 0L, 0L));
    }

    @Test
    void aNewWorldClockDoesNotTreatOfflineTimeAsPaused() {
        CooldownPauseClock clock = new CooldownPauseClock();
        assertEquals(0L, clock.advance(true, 60000L));
        assertEquals(0, CooldownTiming.remainingTimedTicks(11000L, 60000L, 0L, 0L));
    }

    @Test
    void backwardClockChangesCannotSubtractFromCooldown() {
        CooldownPauseClock clock = new CooldownPauseClock();
        clock.advance(true, 1000L);
        assertEquals(0L, clock.advance(true, 900L));
    }

    @Test
    void repeatedPauseCyclesAccumulateOnlyPausedIntervals() {
        CooldownPauseClock clock = new CooldownPauseClock();
        long pausedMillis = clock.advance(false, 1000L);
        pausedMillis += clock.advance(true, 2000L);
        pausedMillis += clock.advance(false, 3000L);
        pausedMillis += clock.advance(false, 4000L);
        pausedMillis += clock.advance(true, 5000L);
        pausedMillis += clock.advance(true, 6000L);
        pausedMillis += clock.advance(false, 7000L);
        assertEquals(3000L, pausedMillis);
    }

    @Test
    void extendingPausePreservesDisabledAndLegacyCooldownsWithoutOverflow() {
        assertEquals(0L, CooldownTiming.extendExpiryForPause(0L, 5000L));
        assertEquals(1000L, CooldownTiming.extendExpiryForPause(1000L, -1L));
        assertEquals(6000L, CooldownTiming.extendExpiryForPause(1000L, 5000L));
        assertEquals(Long.MAX_VALUE, CooldownTiming.extendExpiryForPause(Long.MAX_VALUE - 1L, 5000L));
    }
}
