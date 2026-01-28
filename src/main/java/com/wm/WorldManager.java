package com.wm;

import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.WorldConfig;
import com.hypixel.hytale.server.core.universe.world.storage.provider.DefaultChunkStorageProvider;
import com.hypixel.hytale.server.core.universe.world.worldgen.provider.DummyWorldGenProvider;
import com.hypixel.hytale.server.core.universe.world.worldgen.provider.FlatWorldGenProvider;
import com.hypixel.hytale.server.core.universe.world.worldgen.provider.IWorldGenProvider;
import com.hypixel.hytale.server.core.universe.world.worldgen.provider.VoidWorldGenProvider;
import com.hypixel.hytale.server.worldgen.HytaleWorldGenProvider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class WorldManager {

    private WorldManager() {}

    /**
     * Creates a new world with default type (normal).
     */
    @Nonnull
    public static CompletableFuture<Boolean> createWorld(@Nonnull String worldName) {
        return createWorld(worldName, WorldType.NORMAL);
    }

    /**
     * Creates a new world with the specified type.
     */
    @Nonnull
    public static CompletableFuture<Boolean> createWorld(@Nonnull String worldName, @Nonnull WorldType type) {
        if (Universe.get().getWorld(worldName) != null) {
            return CompletableFuture.completedFuture(false);
        }

        Path savePath = Universe.get().getPath().resolve("worlds").resolve(worldName);

        WorldConfig config = new WorldConfig();
        config.setUuid(UUID.randomUUID());
        config.setWorldGenProvider(type.createProvider());
        config.setChunkStorageProvider(DefaultChunkStorageProvider.INSTANCE);

        return Universe.get().makeWorld(worldName, savePath, config)
                .thenApply(world -> true)
                .exceptionally(error -> false);
    }

    /**
     * Deletes a world by name.
     * @return true if deleted, false if cancelled, null if world doesn't exist
     */
    @Nullable
    public static Boolean deleteWorld(@Nonnull String worldName) {
        if (Universe.get().getWorld(worldName) == null) {
            return null;
        }
        try {
            return Universe.get().removeWorld(worldName);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Gets information about all loaded worlds.
     */
    @Nonnull
    public static List<WorldInfo> listWorlds() {
        List<WorldInfo> result = new ArrayList<>();
        Map<String, World> worlds = Universe.get().getWorlds();

        for (Map.Entry<String, World> entry : worlds.entrySet()) {
            World world = entry.getValue();
            result.add(new WorldInfo(
                    entry.getKey(),
                    world.getPlayerCount()
            ));
        }
        return result;
    }

    /**
     * Checks if a world exists.
     */
    public static boolean worldExists(@Nonnull String worldName) {
        return Universe.get().getWorld(worldName) != null;
    }

    /**
     * Gets the total number of loaded worlds.
     */
    public static int getWorldCount() {
        return Universe.get().getWorlds().size();
    }

    public enum WorldType {
        NORMAL,
        VOID,
        FLAT,
        DUMMY;

        public IWorldGenProvider createProvider() {
            return switch (this) {
                case NORMAL -> new HytaleWorldGenProvider();
                case VOID -> new VoidWorldGenProvider();
                case FLAT -> new FlatWorldGenProvider();
                case DUMMY -> new DummyWorldGenProvider();
            };
        }

        @Nullable
        public static WorldType fromString(String value) {
            if (value == null) return null;
            try {
                return valueOf(value.toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    public static class WorldInfo {
        private final String name;
        private final int playerCount;

        public WorldInfo(String name, int playerCount) {
            this.name = name;
            this.playerCount = playerCount;
        }

        public String getName() {
            return name;
        }

        public int getPlayerCount() {
            return playerCount;
        }
    }
}
