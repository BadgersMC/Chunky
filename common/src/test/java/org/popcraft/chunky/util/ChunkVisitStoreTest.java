package org.popcraft.chunky.util;

import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ChunkVisitStoreTest {
    @Test
    public void persistsAndClearsVisitsIncludingNegativeChunks() throws Exception {
        final Path directory = Files.createTempDirectory("chunky-visits");
        final String worldId = "minecraft:overworld";

        try (ChunkVisitStore store = new ChunkVisitStore(directory)) {
            assertTrue(store.read(worldId, -33, 64).isEmpty());
            store.write(worldId, -33, 64, 123456789L);
            assertEquals(123456789L, store.read(worldId, -33, 64).orElseThrow());
        }

        try (ChunkVisitStore store = new ChunkVisitStore(directory)) {
            assertEquals(123456789L, store.read(worldId, -33, 64).orElseThrow());
            store.clear(worldId, -33, 64);
            assertTrue(store.read(worldId, -33, 64).isEmpty());
        }
    }

    @Test
    public void clearsEntireVisitRegion() throws Exception {
        final Path directory = Files.createTempDirectory("chunky-visits-region");
        final String worldId = "minecraft:overworld";

        try (ChunkVisitStore store = new ChunkVisitStore(directory)) {
            store.write(worldId, 32, -1, 100L);
            store.write(worldId, 63, -32, 200L);
            store.clearRegion(worldId, 1, -1);
            assertTrue(store.read(worldId, 32, -1).isEmpty());
            assertTrue(store.read(worldId, 63, -32).isEmpty());
        }
    }
}
