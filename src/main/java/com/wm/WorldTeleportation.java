package com.wm;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletableFuture;

public final class WorldTeleportation {

    public static final double DEFAULT_X = 0;
    public static final double DEFAULT_Z = 0;
    public static final double FALLBACK_Y = 100;

    private WorldTeleportation() {}

    /**
     * Teleports a player to a world at surface level (0, surface, 0).
     */
    @Nonnull
    public static CompletableFuture<TeleportResult> teleport(@Nonnull Ref<EntityStore> entityRef, @Nonnull String worldName) {
        return teleportToSurface(entityRef, worldName, DEFAULT_X, DEFAULT_Z);
    }

    /**
     * Teleports a player to surface level at specific X,Z coordinates in a world.
     */
    @Nonnull
    public static CompletableFuture<TeleportResult> teleportToSurface(@Nonnull Ref<EntityStore> entityRef, @Nonnull String worldName,
                                                                       double x, double z) {
        World targetWorld = Universe.get().getWorld(worldName);
        if (targetWorld == null) {
            return CompletableFuture.completedFuture(TeleportResult.WORLD_NOT_FOUND);
        }

        // Get current world from the entity store's external data
        Store<EntityStore> store = entityRef.getStore();
        if (store == null) {
            return CompletableFuture.completedFuture(TeleportResult.PLAYER_NOT_IN_WORLD);
        }

        World currentWorld = store.getExternalData().getWorld();
        if (currentWorld == null) {
            return CompletableFuture.completedFuture(TeleportResult.PLAYER_NOT_IN_WORLD);
        }

        if (currentWorld.getName().equalsIgnoreCase(worldName)) {
            return CompletableFuture.completedFuture(TeleportResult.ALREADY_IN_WORLD);
        }

        CompletableFuture<TeleportResult> result = new CompletableFuture<>();

        // Execute EVERYTHING on the current world's thread first
        currentWorld.execute(() -> {
            // Get PlayerRef on the correct thread
            PlayerRef playerRef = store.getComponent(entityRef, PlayerRef.getComponentType());
            if (playerRef == null) {
                result.complete(TeleportResult.PLAYER_NOT_IN_WORLD);
                return;
            }

            playerRef.removeFromStore();

            // Then execute addPlayer on TARGET world's thread
            targetWorld.execute(() -> {
                // Get surface height from chunk
                double surfaceY = getSurfaceHeight(targetWorld, (int) x, (int) z);

                Vector3d position = new Vector3d(x, surfaceY, z);
                Vector3f rotation = new Vector3f(0, 0, 0);
                Transform transform = new Transform(position, rotation);

                targetWorld.addPlayer(playerRef, transform)
                        .thenRun(() -> result.complete(TeleportResult.SUCCESS))
                        .exceptionally(error -> {
                            result.complete(TeleportResult.ERROR);
                            return null;
                        });
            });
        });

        return result;
    }

    /**
     * Teleports a player to specific coordinates in a world (exact Y position).
     */
    @Nonnull
    public static CompletableFuture<TeleportResult> teleport(@Nonnull Ref<EntityStore> entityRef, @Nonnull String worldName,
                                                              double x, double y, double z) {
        World targetWorld = Universe.get().getWorld(worldName);
        if (targetWorld == null) {
            return CompletableFuture.completedFuture(TeleportResult.WORLD_NOT_FOUND);
        }

        // Get current world from the entity store's external data
        Store<EntityStore> store = entityRef.getStore();
        if (store == null) {
            return CompletableFuture.completedFuture(TeleportResult.PLAYER_NOT_IN_WORLD);
        }

        World currentWorld = store.getExternalData().getWorld();
        if (currentWorld == null) {
            return CompletableFuture.completedFuture(TeleportResult.PLAYER_NOT_IN_WORLD);
        }

        if (currentWorld.getName().equalsIgnoreCase(worldName)) {
            return CompletableFuture.completedFuture(TeleportResult.ALREADY_IN_WORLD);
        }

        Vector3d position = new Vector3d(x, y, z);
        Vector3f rotation = new Vector3f(0, 0, 0);
        Transform transform = new Transform(position, rotation);

        CompletableFuture<TeleportResult> result = new CompletableFuture<>();

        // Execute EVERYTHING on the current world's thread first
        currentWorld.execute(() -> {
            // Get PlayerRef on the correct thread
            PlayerRef playerRef = store.getComponent(entityRef, PlayerRef.getComponentType());
            if (playerRef == null) {
                result.complete(TeleportResult.PLAYER_NOT_IN_WORLD);
                return;
            }

            playerRef.removeFromStore();

            // Then execute addPlayer on TARGET world's thread
            targetWorld.execute(() -> {
                targetWorld.addPlayer(playerRef, transform)
                        .thenRun(() -> result.complete(TeleportResult.SUCCESS))
                        .exceptionally(error -> {
                            result.complete(TeleportResult.ERROR);
                            return null;
                        });
            });
        });

        return result;
    }

    /**
     * Gets the surface height at the given world coordinates.
     * Must be called on the world's thread.
     */
    private static double getSurfaceHeight(World world, int x, int z) {
        long chunkIndex = ChunkUtil.indexChunkFromBlock(x, z);
        WorldChunk chunk = world.getNonTickingChunk(chunkIndex);

        if (chunk != null) {
            // Convert to local chunk coordinates (0-31)
            int localX = x & 31;
            int localZ = z & 31;
            short height = chunk.getHeight(localX, localZ);
            // Add 1 to spawn above the surface block
            return height + 1;
        }

        // Fallback if chunk not loaded
        return FALLBACK_Y;
    }

    public enum TeleportResult {
        SUCCESS,
        WORLD_NOT_FOUND,
        ALREADY_IN_WORLD,
        PLAYER_NOT_IN_WORLD,
        ERROR;

        public boolean isSuccess() {
            return this == SUCCESS;
        }
    }
}
