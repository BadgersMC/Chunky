package org.popcraft.chunky.util;

import org.popcraft.chunky.platform.Player;
import org.popcraft.chunky.platform.World;
import org.popcraft.chunky.platform.util.Location;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.OptionalLong;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class ChunkVisitTracker implements AutoCloseable {
    private static final long FLUSH_INTERVAL_SECONDS = 30;
    private final ChunkVisitStore store;
    private final ConcurrentMap<ChunkKey, Long> pending = new ConcurrentHashMap<>();
    private final ScheduledExecutorService flushExecutor;

    public ChunkVisitTracker(final Path directory) {
        this.store = new ChunkVisitStore(directory);
        this.flushExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            final Thread thread = new Thread(runnable, "chunky-visit-store");
            thread.setDaemon(true);
            return thread;
        });
        this.flushExecutor.scheduleAtFixedRate(this::flushQuietly, FLUSH_INTERVAL_SECONDS, FLUSH_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    public void record(final Player player) {
        final Location location = player.getLocation();
        final World world = location.getWorld();
        final int chunkX = Math.floorDiv((int) Math.floor(location.getX()), 16);
        final int chunkZ = Math.floorDiv((int) Math.floor(location.getZ()), 16);
        record(world.getName(), chunkX, chunkZ, System.currentTimeMillis());
    }

    public void recordPlayers(final Collection<Player> players) {
        players.forEach(this::record);
    }

    void record(final String worldId, final int chunkX, final int chunkZ, final long timestamp) {
        pending.merge(new ChunkKey(worldId, chunkX, chunkZ), timestamp, Math::max);
    }

    public synchronized OptionalLong lastVisit(final String worldId, final int chunkX, final int chunkZ) {
        final Long pendingTimestamp = pending.get(new ChunkKey(worldId, chunkX, chunkZ));
        if (pendingTimestamp != null) {
            return OptionalLong.of(pendingTimestamp);
        }
        try {
            return store.read(worldId, chunkX, chunkZ);
        } catch (IOException e) {
            e.printStackTrace();
            return OptionalLong.empty();
        }
    }

    public synchronized void clear(final String worldId, final int chunkX, final int chunkZ) {
        pending.remove(new ChunkKey(worldId, chunkX, chunkZ));
        try {
            store.clear(worldId, chunkX, chunkZ);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void clearRegion(final String worldId, final int regionX, final int regionZ) {
        pending.keySet().removeIf(key -> key.worldId().equals(worldId)
                && Math.floorDiv(key.chunkX(), 32) == regionX
                && Math.floorDiv(key.chunkZ(), 32) == regionZ);
        try {
            store.clearRegion(worldId, regionX, regionZ);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void flush() throws IOException {
        for (final Map.Entry<ChunkKey, Long> entry : new ArrayList<>(pending.entrySet())) {
            store.write(entry.getKey().worldId(), entry.getKey().chunkX(), entry.getKey().chunkZ(), entry.getValue());
            pending.remove(entry.getKey(), entry.getValue());
        }
    }

    private void flushQuietly() {
        try {
            flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() {
        flushExecutor.shutdown();
        try {
            if (!flushExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                flushExecutor.shutdownNow();
            }
            flush();
            store.close();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            flushExecutor.shutdownNow();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private record ChunkKey(String worldId, int chunkX, int chunkZ) {
    }
}
