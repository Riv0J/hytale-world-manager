package com.wm;

import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.system.WorldConfigSaveSystem;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.component.ChunkSavingSystems;
import com.hypixel.hytale.server.core.entity.entities.Player;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.CompletableFuture;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.UUID;

/**
 * /wm clone <new_name> - Clone the current world to a new world
 */
public class CloneCommand extends CommandBase {

    private final RequiredArg<String> nameArg = this.withRequiredArg(
            "clone_name", "Name for the cloned world", ArgTypes.STRING);

    public CloneCommand() {
        super("clone", "Clone current world to a new world");
    }

    @Override
    protected void executeSync(@Nonnull CommandContext ctx) {
        if (!(ctx.sender() instanceof Player player)) {
            send(ctx, "This command can only be run by a player");
            return;
        }

        String newName = ctx.get(this.nameArg);
        World sourceWorld = player.getWorld();
        String sourceName = sourceWorld.getName();

        // Check if target world already exists
        if (Universe.get().getWorld(newName) != null) {
            send(ctx, "World '" + newName + "' already exists");
            return;
        }

        // Get worlds directory path
        Path worldsDir = Paths.get("universe", "worlds");
        Path sourcePath = worldsDir.resolve(sourceName);
        Path targetPath = worldsDir.resolve(newName);

        // Check if target already exists on disk
        if (Files.exists(targetPath)) {
            send(ctx, "World folder '" + newName + "' already exists on disk");
            return;
        }

        send(ctx, "Cloning world '" + sourceName + "' to '" + newName + "'...");

        // Save the source world first to ensure all chunks are written (must run on world thread)
        CompletableFuture.runAsync(() ->
                CompletableFuture.allOf(
                        WorldConfigSaveSystem.saveWorldConfigAndResources(sourceWorld),
                        ChunkSavingSystems.saveChunksInWorld(sourceWorld.getChunkStore().getStore())
                ).join(),
                sourceWorld
        ).join();

        try {
            // Copy the world folder
            copyDirectory(sourcePath, targetPath);

            // Update the config.json with new UUID
            Path configPath = targetPath.resolve("config.json");
            if (Files.exists(configPath)) {
                String config = Files.readString(configPath);
                // Generate new UUID and encode as base64
                UUID newUuid = UUID.randomUUID();
                String base64Uuid = java.util.Base64.getEncoder().encodeToString(uuidToBytes(newUuid));

                // Replace the UUID in config (the $binary field)
                config = config.replaceFirst(
                    "\"\\$binary\":\\s*\"[^\"]+\"",
                    "\"\\$binary\": \"" + base64Uuid + "\""
                );
                Files.writeString(configPath, config);
            }

            // Load the new world
            CommandManager.get().handleCommand(ctx.sender(), "world load " + newName);

            send(ctx, "World '" + newName + "' cloned successfully!");

        } catch (IOException e) {
            send(ctx, "Failed to clone world: " + e.getMessage());
            // Try to clean up partial copy
            try {
                deleteDirectory(targetPath);
            } catch (IOException ignored) {}
        }
    }

    private void copyDirectory(Path source, Path target) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path targetDir = target.resolve(source.relativize(dir));
                Files.createDirectories(targetDir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.copy(file, target.resolve(source.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void deleteDirectory(Path path) throws IOException {
        if (!Files.exists(path)) return;

        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private byte[] uuidToBytes(UUID uuid) {
        byte[] bytes = new byte[16];
        long msb = uuid.getMostSignificantBits();
        long lsb = uuid.getLeastSignificantBits();
        for (int i = 0; i < 8; i++) {
            bytes[i] = (byte) (msb >>> (8 * (7 - i)));
            bytes[8 + i] = (byte) (lsb >>> (8 * (7 - i)));
        }
        return bytes;
    }

    private void send(CommandContext ctx, String text) {
        ctx.sender().sendMessage(Message.raw(text));
    }
}
