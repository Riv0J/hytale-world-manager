package com.wm;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.component.Ref;

import javax.annotation.Nonnull;
import java.util.List;

public class WMCommand extends CommandBase {
    @Nonnull
    private final RequiredArg<String> actionArg = this.withRequiredArg(
            "action",
            "Action: create/delete/list/tp",
            ArgTypes.STRING
    );

    private final OptionalArg<String> nameArg = this.withOptionalArg(
            "name",
            "Name of the world to create/delete",
            ArgTypes.STRING
    );

    private final OptionalArg<String> typeArg = this.withOptionalArg(
            "type",
            "World type: normal/flat/void/dummy (default: normal)",
            ArgTypes.STRING
    );

    public WMCommand() {
        super("wm", "World manager command.");
        // Restrict to admins/owners only
        this.requirePermission("hytale.command.wm");
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        String action = context.get(this.actionArg);
        String name = context.get(this.nameArg);
        String type = context.get(this.typeArg);

        switch (action) {
            case "create":
                this.handleCreate(context, name, type);
                break;
            case "delete":
                this.handleDelete(context, name);
                break;
            case "list":
                this.handleList(context);
                break;
            case "tp":
                this.handleTeleport(context, name);
                break;
            default:
                this.send(context, "Available actions: create, delete, list, tp");
        }
    }

    private void handleCreate(CommandContext ctx, String worldName, String typeStr) {
        if (worldName == null) {
            this.send(ctx, "Usage: /wm create --name <world_name> [--type normal/flat/void/dummy]");
            return;
        }

        WorldManager.WorldType worldType = WorldManager.WorldType.NORMAL;
        if (typeStr != null) {
            WorldManager.WorldType parsed = WorldManager.WorldType.fromString(typeStr);
            if (parsed == null) {
                this.send(ctx, "Invalid world type '" + typeStr + "'. Available: normal, flat, void, dummy");
                return;
            }
            worldType = parsed;
        }

        final WorldManager.WorldType finalType = worldType;
        WorldManager.createWorld(worldName, worldType)
                .thenAccept(success -> {
                    if (success) {
                        this.send(ctx, "Created " + finalType.name().toLowerCase() + " world '" + worldName + "'!");
                    } else {
                        this.send(ctx, "World '" + worldName + "' already exists.");
                    }
                })
                .exceptionally(error -> {
                    this.send(ctx, "Error creating world: " + error.getMessage());
                    return null;
                });
    }

    private void handleDelete(CommandContext ctx, String worldName) {
        if (worldName == null) {
            this.send(ctx, "Usage: /wm delete --name <world_name>");
            return;
        }

        Boolean result = WorldManager.deleteWorld(worldName);
        if (result == null) {
            this.send(ctx, "World '" + worldName + "' does not exist.");
        } else if (result) {
            this.send(ctx, "Deleted world '" + worldName + "'.");
        } else {
            this.send(ctx, "Could not delete world '" + worldName + "'.");
        }
    }

    private void handleList(CommandContext ctx) {
        List<WorldManager.WorldInfo> worlds = WorldManager.listWorlds();

        if (worlds.isEmpty()) {
            this.send(ctx, "No worlds loaded.");
            return;
        }

        this.send(ctx, "Loaded worlds (" + worlds.size() + "):");
        for (WorldManager.WorldInfo info : worlds) {
            this.send(ctx, "  - " + info.getName() + " [" + info.getPlayerCount() + " players]");
        }
    }

    private void handleTeleport(CommandContext ctx, String worldName) {
        if (worldName == null) {
            this.send(ctx, "Usage: /wm tp --name <world_name>");
            return;
        }

        if (!ctx.isPlayer()) {
            this.send(ctx, "This command can only be used by players.");
            return;
        }

        Ref<EntityStore> entityRef = ctx.senderAsPlayerRef();

        WorldTeleportation.teleport(entityRef, worldName)
                .thenAccept(result -> {
                    switch (result) {
                        case SUCCESS:
                            this.send(ctx, "Teleported to world '" + worldName + "'!");
                            break;
                        case WORLD_NOT_FOUND:
                            this.send(ctx, "World '" + worldName + "' does not exist.");
                            break;
                        case ALREADY_IN_WORLD:
                            this.send(ctx, "You are already in world '" + worldName + "'.");
                            break;
                        case PLAYER_NOT_IN_WORLD:
                            this.send(ctx, "Error: Player is not in a world.");
                            break;
                        case ERROR:
                            this.send(ctx, "Error teleporting to world.");
                            break;
                    }
                });
    }

    private void send(CommandContext ctx, String text) {
        ctx.sender().sendMessage(Message.raw(text));
    }
}
