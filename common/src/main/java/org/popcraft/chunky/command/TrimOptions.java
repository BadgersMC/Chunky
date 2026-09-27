package org.popcraft.chunky.command;

import java.time.Duration;
import java.util.Optional;
import java.util.OptionalLong;

final class TrimOptions {
    private final OptionalLong inhabitedTime;
    private final Optional<Duration> staleAfter;
    private final boolean dryRun;

    TrimOptions(final OptionalLong inhabitedTime, final Optional<Duration> staleAfter, final boolean dryRun) {
        this.inhabitedTime = inhabitedTime;
        this.staleAfter = staleAfter;
        this.dryRun = dryRun;
    }

    public boolean hasInhabitedTime() {
        return inhabitedTime.isPresent();
    }

    public long inhabitedTime() {
        return inhabitedTime.orElseThrow();
    }

    public boolean hasStaleAfter() {
        return staleAfter.isPresent();
    }

    public Optional<Duration> staleAfter() {
        return staleAfter;
    }

    public boolean dryRun() {
        return dryRun;
    }

    public boolean hasFilters() {
        return hasInhabitedTime() || hasStaleAfter();
    }

    public boolean shouldTrim(final OptionalLong chunkInhabitedTime, final OptionalLong lastUpdate, final long now) {
        if (!hasFilters()) {
            return true;
        }
        boolean matches = false;
        if (hasInhabitedTime()) {
            matches = chunkInhabitedTime.isEmpty() || chunkInhabitedTime.getAsLong() <= inhabitedTime();
        }
        if (hasStaleAfter()) {
            final long cutoff = now - staleAfter.orElseThrow().toMillis();
            matches |= lastUpdate.isPresent() && lastUpdate.getAsLong() <= cutoff;
        }
        return matches;
    }
}
