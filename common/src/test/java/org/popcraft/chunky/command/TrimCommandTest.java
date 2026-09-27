package org.popcraft.chunky.command;

import org.junit.Test;

import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.OptionalLong;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TrimCommandTest {
    @Test
    public void readsUnsignedAnvilChunkTimestamp() throws Exception {
        final Path region = Files.createTempFile("chunky-region", ".mca");
        final int chunkLocation = (7 + 11 * 32) * 4;
        final long timestampSeconds = 4_000_000_000L;
        try (final RandomAccessFile file = new RandomAccessFile(region.toFile(), "rw")) {
            file.setLength(8192);
            file.seek(4096L + chunkLocation);
            file.writeInt((int) timestampSeconds);

            final OptionalLong lastUpdate = TrimCommand.getLastUpdate(file, chunkLocation);
            assertTrue(lastUpdate.isPresent());
            assertEquals(timestampSeconds * 1000L, lastUpdate.getAsLong());
        } finally {
            Files.deleteIfExists(region);
        }
    }

    @Test
    public void zeroAnvilChunkTimestampIsUnknown() throws Exception {
        final Path region = Files.createTempFile("chunky-region", ".mca");
        try (final RandomAccessFile file = new RandomAccessFile(region.toFile(), "rw")) {
            file.setLength(8192);
            assertTrue(TrimCommand.getLastUpdate(file, 0).isEmpty());
        } finally {
            Files.deleteIfExists(region);
        }
    }
}
