package org.popcraft.chunky.util;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;

public final class ChunkVisitStore implements AutoCloseable {
    private static final int REGION_SIZE = 32;
    private static final int MAX_OPEN_FILES = 64;
    private final Path directory;
    private final Map<Path, RandomAccessFile> openFiles = new LinkedHashMap<>(16, 0.75f, true);

    public ChunkVisitStore(final Path directory) {
        this.directory = directory;
    }

    public synchronized void write(final String worldId, final int chunkX, final int chunkZ, final long timestamp) throws IOException {
        if (timestamp <= 0) {
            throw new IllegalArgumentException("timestamp must be positive");
        }
        final RandomAccessFile file = getFile(worldId, chunkX, chunkZ, true).orElseThrow();
        file.seek(offset(chunkX, chunkZ));
        file.writeLong(timestamp);
    }

    public synchronized OptionalLong read(final String worldId, final int chunkX, final int chunkZ) throws IOException {
        final Optional<RandomAccessFile> optionalFile = getFile(worldId, chunkX, chunkZ, false);
        if (optionalFile.isEmpty()) {
            return OptionalLong.empty();
        }
        final RandomAccessFile file = optionalFile.get();
        final long offset = offset(chunkX, chunkZ);
        if (file.length() < offset + Long.BYTES) {
            return OptionalLong.empty();
        }
        file.seek(offset);
        final long timestamp = file.readLong();
        return timestamp > 0 ? OptionalLong.of(timestamp) : OptionalLong.empty();
    }

    public synchronized void clear(final String worldId, final int chunkX, final int chunkZ) throws IOException {
        final Optional<RandomAccessFile> optionalFile = getFile(worldId, chunkX, chunkZ, false);
        if (optionalFile.isEmpty()) {
            return;
        }
        final RandomAccessFile file = optionalFile.get();
        final long offset = offset(chunkX, chunkZ);
        if (file.length() < offset + Long.BYTES) {
            return;
        }
        file.seek(offset);
        file.writeLong(0L);
    }

    public synchronized void clearRegion(final String worldId, final int regionX, final int regionZ) throws IOException {
        final Path path = regionPath(worldId, regionX, regionZ);
        final RandomAccessFile openFile = openFiles.remove(path);
        if (openFile != null) {
            openFile.close();
        }
        Files.deleteIfExists(path);
    }

    private Optional<RandomAccessFile> getFile(final String worldId, final int chunkX, final int chunkZ, final boolean create) throws IOException {
        final Path path = regionPath(worldId, Math.floorDiv(chunkX, REGION_SIZE), Math.floorDiv(chunkZ, REGION_SIZE));
        final RandomAccessFile cached = openFiles.get(path);
        if (cached != null) {
            return Optional.of(cached);
        }
        if (!create && Files.notExists(path)) {
            return Optional.empty();
        }
        if (create) {
            Files.createDirectories(path.getParent());
        }
        evictIfNecessary();
        final RandomAccessFile file = new RandomAccessFile(path.toFile(), "rw");
        openFiles.put(path, file);
        return Optional.of(file);
    }

    private void evictIfNecessary() throws IOException {
        if (openFiles.size() < MAX_OPEN_FILES) {
            return;
        }
        final Iterator<Map.Entry<Path, RandomAccessFile>> iterator = openFiles.entrySet().iterator();
        if (iterator.hasNext()) {
            final Map.Entry<Path, RandomAccessFile> eldest = iterator.next();
            eldest.getValue().close();
            iterator.remove();
        }
    }

    private Path regionPath(final String worldId, final int regionX, final int regionZ) {
        final UUID worldKey = UUID.nameUUIDFromBytes(worldId.getBytes(StandardCharsets.UTF_8));
        return directory.resolve(worldKey.toString()).resolve("r." + regionX + "." + regionZ + ".visits");
    }

    private static long offset(final int chunkX, final int chunkZ) {
        final int localX = Math.floorMod(chunkX, REGION_SIZE);
        final int localZ = Math.floorMod(chunkZ, REGION_SIZE);
        return (long) (localX + localZ * REGION_SIZE) * Long.BYTES;
    }

    @Override
    public synchronized void close() throws IOException {
        IOException thrown = null;
        for (final RandomAccessFile file : openFiles.values()) {
            try {
                file.close();
            } catch (IOException e) {
                if (thrown == null) {
                    thrown = e;
                } else {
                    thrown.addSuppressed(e);
                }
            }
        }
        openFiles.clear();
        if (thrown != null) {
            throw thrown;
        }
    }
}
