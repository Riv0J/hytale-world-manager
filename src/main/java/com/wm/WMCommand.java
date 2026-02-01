package com.wm;

import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.FlagArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.entity.entities.Player;

import javax.annotation.Nonnull;
import java.util.List;

public class WMCommand extends AbstractCommandCollection {

    public WMCommand() {
        super("wm", "World manager commands");
        this.requirePermission("hytale.command.wm");

        this.addSubCommand(new AddCommand());
        this.addSubCommand(new RemoveCommand());
        this.addSubCommand(new ListCommand());
        this.addSubCommand(new TpCommand());
        this.addSubCommand(new DefaultCommand());
        this.addSubCommand(new SpawnCommand());
        this.addSubCommand(new ProtectCommand());
    }

    // /wm add <world> [--type] - wrapper for /world add
    private static class AddCommand extends CommandBase {
        private final RequiredArg<String> worldArg = this.withRequiredArg(
                "world", "World name", ArgTypes.STRING);
        private final OptionalArg<String> typeArg = this.withOptionalArg(
                "type", "World type: normal/flat/void/dummy", ArgTypes.STRING);

        AddCommand() {
            super("add", "Create a new world (wraps /world add)");
        }

        @Override
        protected void executeSync(@Nonnull CommandContext ctx) {
            String worldName = ctx.get(this.worldArg);
            String typeStr = ctx.get(this.typeArg);

            // Build command: /world add <name> [--gen <type>]
            String cmd = "world add " + worldName;
            if(typeStr != null){
                cmd += " --gen " + typeStr.substring(0, 1).toUpperCase() + typeStr.substring(1);
            }
            CommandManager.get().handleCommand(ctx.sender(), cmd);
        }

        private String mapToHytaleGen(String type) {
            if (type == null) return null;
            return switch (type.toLowerCase()) {
                case "flat" -> "Flat";
                case "void" -> "Void";
                case "dummy" -> "Dummy";
                case "normal" -> null;  // default, no need to specify
                default -> null;
            };
        }
    }

    // /wm remove <world> [--destroy]
    private static class RemoveCommand extends CommandBase {
        private final RequiredArg<String> worldArg = this.withRequiredArg(
                "world", "World name", ArgTypes.STRING);
        private final FlagArg destroyFlag = this.withFlagArg(
                "destroy", "To permanently delete world files from disk");

        RemoveCommand() {
            super("remove", "Unload world (add --destroy to permanently delete)");
        }

        @Override
        protected void executeSync(@Nonnull CommandContext ctx) {
            String worldName = ctx.get(this.worldArg);
            boolean destroy = ctx.get(this.destroyFlag);

            if (destroy) {
                // --destroy: permanently delete from disk
                destroyWorld(ctx, worldName);
            } else {
                // Default: just unload (wrapper for /world remove)
                CommandManager.get().handleCommand(ctx.sender(), "world remove " + worldName);
            }
        }

        private void destroyWorld(CommandContext ctx, String worldName) {
            World world = Universe.get().getWorld(worldName);

            if (world == null) {
                send(ctx, "World '" + worldName + "' not found");
                return;
            }

            String defaultWorld = HytaleServer.get().getConfig().getDefaults().getWorld();
            if (worldName.equalsIgnoreCase(defaultWorld)) {
                send(ctx, "Cannot destroy the default world. Change it first with /wm default <other>");
                return;
            }

            if (Universe.get().getWorlds().size() == 1) {
                send(ctx, "Cannot destroy the only loaded world");
                return;
            }

            // Set flag to delete files from disk
            world.getWorldConfig().setDeleteOnRemove(true);

            boolean removed = Universe.get().removeWorld(worldName);
            if (removed) {
                send(ctx, "Permanently deleted world '" + worldName + "'");
            } else {
                send(ctx, "Failed to destroy world '" + worldName + "'");
            }
        }
    }

    // /wm list
    private static class ListCommand extends CommandBase {
        ListCommand() {
            super("list", "List all worlds");
        }

        @Override
        protected void executeSync(@Nonnull CommandContext ctx) {
            List<WorldManager.WorldInfo> worlds = WorldManager.listWorlds();

            if (worlds.isEmpty()) {
                send(ctx, "No worlds loaded");
                return;
            }

            String defaultWorld = HytaleServer.get().getConfig().getDefaults().getWorld();

            send(ctx, "Worlds (" + worlds.size() + "):");
            for (WorldManager.WorldInfo info : worlds) {
                String name = info.getName();
                boolean isDefault = name.equalsIgnoreCase(defaultWorld);
                String line = "  - " + name + " [" + info.getPlayerCount() + " players]";
                if (isDefault) {
                    line += " [default]";
                }
                send(ctx, line);
            }
        }
    }

    // /wm tp <world> - wrapper for /tp world <name>
    private static class TpCommand extends CommandBase {
        private final RequiredArg<String> worldArg = this.withRequiredArg(
                "world", "World name", ArgTypes.STRING);

        TpCommand() {
            super("tp", "Teleport to a world (wraps /tp world)");
        }

        @Override
        protected void executeSync(@Nonnull CommandContext ctx) {
            String worldName = ctx.get(this.worldArg);
            CommandManager.get().handleCommand(ctx.sender(), "tp world " + worldName);
        }
    }

    // /wm default <world> - wrapper for /world setdefault
    private static class DefaultCommand extends CommandBase {
        private final RequiredArg<String> worldArg = this.withRequiredArg(
                "world", "World name", ArgTypes.STRING);

        DefaultCommand() {
            super("default", "Set the default world (wraps /world setdefault)");
        }

        @Override
        protected void executeSync(@Nonnull CommandContext ctx) {
            String worldName = ctx.get(this.worldArg);
            // Delegate to Hytale's built-in command
            CommandManager.get().handleCommand(ctx.sender(), "world setdefault " + worldName);
        }
    }

    // /wm spawn - wrapper for /world config setspawn
    private static class SpawnCommand extends CommandBase {
        SpawnCommand() {
            super("spawn", "Set world spawn at current position (wraps /world config setspawn)");
        }

        @Override
        protected void executeSync(@Nonnull CommandContext ctx) {
            CommandManager.get().handleCommand(ctx.sender(), "world config setspawn");
        }
    }

    // /wm protect [--off]
    private static class ProtectCommand extends CommandBase {
        private final FlagArg offFlag = this.withFlagArg(
                "off", "Disable protection (allow block breaking)");

        ProtectCommand() {
            super("protect", "Enable/disable block protection for current world");
        }

        @Override
        protected void executeSync(@Nonnull CommandContext ctx) {
            if (!(ctx.sender() instanceof Player player)) {
                send(ctx, "This command can only be run by a player");
                return;
            }

            boolean disableProtection = ctx.get(this.offFlag);
            World world = player.getWorld();
            String worldName = world.getName();
            String gameplayConfig = disableProtection ? "Default" : "WMProtected";

            world.getWorldConfig().setGameplayConfig(gameplayConfig);
            world.getWorldConfig().markChanged();

            if (disableProtection) {
                send(ctx, "Protection DISABLED for '" + worldName + "'");
            } else {
                send(ctx, "Protection ENABLED for '" + worldName + "'");
            }
        }
    }

    private static void send(CommandContext ctx, String text) {
        ctx.sender().sendMessage(Message.raw(text));
    }
}
