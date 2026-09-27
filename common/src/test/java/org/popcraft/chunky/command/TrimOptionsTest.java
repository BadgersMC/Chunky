package org.popcraft.chunky.command;

import org.junit.Test;

import java.time.Duration;
import java.util.Optional;
import java.util.OptionalLong;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TrimOptionsTest {
    private static final long NOW = 1_800_000_000_000L;

    @Test
    public void missingChunkTimestampIsProtectedFromStaleFilter() {
        final TrimOptions options = new TrimOptions(OptionalLong.empty(), Optional.of(Duration.ofDays(30)), false);
        assertFalse(options.shouldTrim(OptionalLong.empty(), OptionalLong.empty(), NOW));
    }

    @Test
    public void oldChunkUpdateMatchesStaleFilter() {
        final TrimOptions options = new TrimOptions(OptionalLong.empty(), Optional.of(Duration.ofDays(30)), false);
        assertTrue(options.shouldTrim(OptionalLong.empty(), OptionalLong.of(NOW - Duration.ofDays(31).toMillis()), NOW));
    }

    @Test
    public void filtersAreCombinedWithOr() {
        final TrimOptions options = new TrimOptions(OptionalLong.of(600), Optional.of(Duration.ofDays(30)), false);
        assertTrue(options.shouldTrim(OptionalLong.of(400), OptionalLong.of(NOW), NOW));
        assertTrue(options.shouldTrim(OptionalLong.of(20_000), OptionalLong.of(NOW - Duration.ofDays(31).toMillis()), NOW));
        assertFalse(options.shouldTrim(OptionalLong.of(20_000), OptionalLong.of(NOW), NOW));
    }

    @Test
    public void noFiltersPreservesLegacyBehavior() {
        final TrimOptions options = new TrimOptions(OptionalLong.empty(), Optional.empty(), false);
        assertTrue(options.shouldTrim(OptionalLong.empty(), OptionalLong.empty(), NOW));
    }
}
