package br.com.bobwizley.rootboot.feature.deathstats;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DeathStatsMessageTest {

    @Test
    void anUnstartedClockReadsAsZero() {
        assertEquals("0:00:00", DeathStatsMessage.duration(0L));
    }

    @Test
    void ticksBelowASecondAreNotCounted() {
        assertEquals("0:00:00", DeathStatsMessage.duration(19L));
    }

    @Test
    void minutesAndSecondsArePaddedToTwoDigits() {
        assertEquals("0:01:05", DeathStatsMessage.duration(65L * 20L));
    }

    @Test
    void hoursAreNotWrappedIntoDays() {
        assertEquals("30:00:00", DeathStatsMessage.duration(30L * 3_600L * 20L));
    }
}
