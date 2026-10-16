package com.jvn.secondwind.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class CooldownTimingTest {
    @Test
    void timedCooldownUsesWallClockAcrossLogoutAndServerLag() {
        assertEquals(6000, CooldownTiming.remainingTimedTicks(301000L, 1000L, 6100L, 100L));
        assertEquals(4800, CooldownTiming.remainingTimedTicks(301000L, 61000L, 6100L, 101L));
        assertEquals(0, CooldownTiming.remainingTimedTicks(301000L, 301000L, 6100L, 102L));
        assertEquals(0, CooldownTiming.remainingTimedTicks(301000L, 302000L, 6100L, 103L));
    }

    @Test
    void roundsUpPartialTicksWithoutEndingTheCooldownEarly() {
        assertEquals(2, CooldownTiming.remainingTimedTicks(1000L, 949L, 0L, 0L));
        assertEquals(1, CooldownTiming.remainingTimedTicks(1000L, 950L, 0L, 0L));
        assertEquals(1, CooldownTiming.remainingTimedTicks(1000L, 999L, 0L, 0L));
    }

    @Test
    void oldSavesFallBackToGameTime() {
        assertEquals(21, CooldownTiming.remainingTimedTicks(0L, 500000L, 121L, 100L));
        assertEquals(0, CooldownTiming.remainingTimedTicks(0L, 500000L, 121L, 121L));
        assertEquals(0, CooldownTiming.remainingTimedTicks(0L, 500000L, 121L, 122L));
        assertEquals(0, CooldownTiming.remainingTimedTicks(0L, 500000L, 0L, 100L));
    }

    @Test
    void dayCooldownTracksTimeChangesAndTheMidnightBoundary() {
        assertEquals(24000, CooldownTiming.remainingDayTicks(0L));
        assertEquals(12000, CooldownTiming.remainingDayTicks(12000L));
        assertEquals(1, CooldownTiming.remainingDayTicks(23999L));
        assertEquals(24000, CooldownTiming.remainingDayTicks(24000L));
        assertEquals(12000, CooldownTiming.remainingDayTicks(60000L));
        assertEquals(1, CooldownTiming.remainingDayTicks(-1L));
    }

    @Test
    void statusRoundsUpTheLastSecondAndPreservesSleepOnlyDuration() {
        assertEquals(-1, CooldownTiming.remainingSeconds(-1));
        assertEquals(0, CooldownTiming.remainingSeconds(0));
        assertEquals(1, CooldownTiming.remainingSeconds(1));
        assertEquals(1, CooldownTiming.remainingSeconds(20));
        assertEquals(2, CooldownTiming.remainingSeconds(21));
        assertEquals(300, CooldownTiming.remainingSeconds(6000));
    }

    @Test
    void longDurationsCannotOverflowIntoInfiniteOrExpiredEffects() {
        assertEquals(Integer.MAX_VALUE, CooldownTiming.remainingTimedTicks(Long.MAX_VALUE, 0L, 0L, 0L));
        assertEquals(Integer.MAX_VALUE, CooldownTiming.remainingTimedTicks(0L, 0L, Long.MAX_VALUE, 0L));
        assertEquals(107374183, CooldownTiming.remainingSeconds(Integer.MAX_VALUE));
    }
}
