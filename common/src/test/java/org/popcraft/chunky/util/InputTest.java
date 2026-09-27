package org.popcraft.chunky.util;

import org.junit.Test;

import java.time.Duration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class InputTest {
    @Test
    public void parsesDurations() {
        assertEquals(Duration.ofSeconds(30), Input.tryDuration("30s").orElseThrow());
        assertEquals(Duration.ofMinutes(15), Input.tryDuration("15m").orElseThrow());
        assertEquals(Duration.ofHours(12), Input.tryDuration("12h").orElseThrow());
        assertEquals(Duration.ofDays(30), Input.tryDuration("30d").orElseThrow());
        assertEquals(Duration.ofDays(14), Input.tryDuration("2w").orElseThrow());
    }

    @Test
    public void rejectsInvalidDurations() {
        assertTrue(Input.tryDuration("30").isEmpty());
        assertTrue(Input.tryDuration("later").isEmpty());
    }
}
