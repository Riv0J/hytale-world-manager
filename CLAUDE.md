# Hytale Server Plugin Development Guide

## Project: WorldManager

## Project Structure

```
src/main/java/
└── com/wm/
    ├── WorldManagerPlugin.java # Main plugin entrypoint
    ├── WMCommand.java          # World manager command (/wm)
    ├── WorldManager.java       # World CRUD operations
    └── WorldTeleportation.java # Player teleportation logic
```

**Gradle entrypoint:** `com.wm.WorldManagerPlugin`

Server source code for reference: `HytaleServer-source/`

## Commands Overview

| Command | Target | Description |
|---------|--------|-------------|
| `/warp` | Players | Built-in Hytale command for player teleportation between worlds |
| `/wm` | Admins/Owners | World Manager - create, delete, list, and teleport to worlds |

### /wm Command Usage
- `/wm create --name <world> [--type normal/flat/void/dummy]` - Create a new world
- `/wm delete --name <world>` - Delete a world
- `/wm list` - List all loaded worlds
- `/wm tp --name <world>` - Teleport to a world (spawns at surface level)

**Permission:** `hytale.command.wm` (admins/owners only)

## Server Architecture

- **Main package**: `com.hypixel.hytale.server.core`
- **Plugin system**: `com.hypixel.hytale.server.core.plugin`
- **Universe/Worlds**: `com.hypixel.hytale.server.core.universe`
- **Entities**: `com.hypixel.hytale.server.core.entity`
- **Commands**: `com.hypixel.hytale.server.core.command`

## Plugin Basics

### Main Plugin Class

```java
public class MyPlugin extends JavaPlugin {
    public MyPlugin(JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        // Register commands, events, etc.
        this.getCommandRegistry().registerCommand(new MyCommand());
    }

    @Override
    protected void start() {
        // Called after all plugins are setup
    }

    @Override
    protected void shutdown() {
        // Cleanup
    }
}
```

### Plugin Lifecycle
1. `setup()` - Register commands, events, blocks, entities
2. `start()` - Dependencies ready, can interact with other plugins
3. `shutdown()` - Cleanup on server stop

## Commands

### Basic Command

```java
public class MyCommand extends CommandBase {
    @Nonnull
    private final RequiredArg<String> nameArg = this.withRequiredArg(
            "name", "Description", ArgTypes.STRING);

    private final OptionalArg<Integer> countArg = this.withOptionalArg(
            "count", "Description", ArgTypes.INTEGER);

    public MyCommand() {
        super("mycommand", "Command description");
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        String name = context.get(this.nameArg);
        Integer count = context.get(this.countArg); // Can be null

        context.sender().sendMessage(Message.raw("Hello " + name));
    }
}
```

### Argument Types
- `RequiredArg<T>` - Must be provided
- `OptionalArg<T>` - Uses `--name value` syntax, can be null
- `DefaultArg<T>` - Optional with default value

### Common ArgTypes
- `ArgTypes.STRING`
- `ArgTypes.INTEGER`
- `ArgTypes.DOUBLE`
- `ArgTypes.PLAYER_REF`
- `ArgTypes.WORLD`

### Getting Player from Command

```java
if (context.isPlayer()) {
    Player player = context.senderAs(Player.class);
    PlayerRef playerRef = player.getPlayerRef();
}
```

### Command Permissions

Restrict commands to admins/owners:

```java
public MyCommand() {
    super("mycommand", "Description");
    // Require specific permission
    this.requirePermission("hytale.command.mycommand");
}
```

**Permission format:** `hytale.command.<command_name>`

Players without the permission will see an error message when trying to use the command.

### Admin/OP System

The built-in `OP` group has wildcard permission `"*"` (all permissions).

**Grant admin via command:**
```
/op self          # Toggle your own OP status (requires --allow-op flag in multiplayer)
/op add <uuid>    # Add another player as OP (requires existing OP)
```

**Grant admin via code:**
```java
PermissionsModule.get().addUserToGroup(playerUuid, "OP");
```

**Grant admin via permissions.json:**
```json
{
  "users": {
    "player-uuid-here": {
      "groups": ["OP"]
    }
  },
  "groups": {
    "OP": ["*"],
    "Default": []
  }
}
```

**Permission wildcards:**
- `"*"` - All permissions
- `"hytale.command.*"` - All command permissions
- `"-hytale.command.wm"` - Deny specific permission (prefix with `-`)

## World Management

### Universe API

```java
Universe universe = Universe.get();

// Get world
World world = universe.getWorld("worldName");

// List worlds
Map<String, World> worlds = universe.getWorlds();

// Create world (DEPRECATED but works)
universe.addWorld("name", "Void", "Empty")
    .thenAccept(world -> { ... });

// Delete world
boolean removed = universe.removeWorld("worldName");
```

### World Generators
- `"Void"` - Empty void world
- `"Flat"` - Flat world
- `"Dummy"` - Dummy generator

### Chunk Storage Types
- `"Empty"` - No persistence
- `"Hytale"` - Default storage

### Save/Universe Folder Structure

```
/saves/                          <- Saves created from launcher
  └── MyGame/                    <- A single "save" (game profile)
      └── universe/              <- Universe folder (contains all worlds)
          ├── default/           <- Main world (created automatically)
          └── myCustomWorld/     <- Worlds created with /wm create
```

- **Saves** (launcher): Game profiles visible in the launcher menu
- **Universe**: Container for all worlds within a save
- **Worlds**: Individual worlds created via plugin live inside `universe/`

Worlds created with `Universe.makeWorld()` are stored in `universe/<worldName>/` and persist across server restarts. They won't appear as separate "saves" in the launcher - they exist within the active save's universe.

## Player Teleportation

### IMPORTANT: Threading Rules

Operations on players/entities MUST run on the correct world thread!

```java
// WRONG - will throw threading assertion error
playerRef.removeFromStore();

// CORRECT - execute on world thread
World currentWorld = reference.getStore().getExternalData().getWorld();
currentWorld.execute(() -> {
    playerRef.removeFromStore();
    // ... then add to new world
});
```

### Getting Player's Current World

```java
Ref<EntityStore> reference = playerRef.getReference();
if (reference != null) {
    World currentWorld = reference.getStore().getExternalData().getWorld();
}
```

### Teleporting Player to Another World

Use `WorldTeleportation` class for player teleportation:

```java
// Simple teleport to default spawn (0, 64, 0)
WorldTeleportation.teleport(playerRef, "worldName")
    .thenAccept(result -> {
        if (result.isSuccess()) {
            // Teleported!
        }
    });

// Teleport to specific coordinates
WorldTeleportation.teleport(playerRef, "worldName", 100, 64, -50)
    .thenAccept(result -> { ... });
```

**TeleportResult enum:**
- `SUCCESS` - Teleported successfully
- `WORLD_NOT_FOUND` - Target world doesn't exist
- `ALREADY_IN_WORLD` - Player is already in that world
- `PLAYER_NOT_IN_WORLD` - Player has no current world reference
- `ERROR` - Teleportation failed

### Internal Teleportation Logic

```java
// MUST execute on current world's thread
currentWorld.execute(() -> {
    playerRef.removeFromStore();
    targetWorld.addPlayer(playerRef, transform)
            .thenRun(() -> result.complete(true))
            .exceptionally(error -> {
                result.complete(false);
                return null;
            });
});
```

## Key Classes Reference

| Class | Location | Purpose |
|-------|----------|---------|
| `JavaPlugin` | `server.core.plugin` | Base plugin class |
| `CommandBase` | `server.core.command.system.basecommands` | Sync command base |
| `Universe` | `server.core.universe` | World manager singleton |
| `World` | `server.core.universe.world` | World instance |
| `Player` | `server.core.entity.entities` | Player entity component |
| `PlayerRef` | `server.core.universe` | Player reference/connection |
| `Message` | `server.core` | Chat messages |

## Common Imports

```java
// Plugin
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

// Commands
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;

// World/Universe
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

// Entity/Player
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.component.Ref;

// Math
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Transform;

// Messaging
import com.hypixel.hytale.server.core.Message;

// Async
import com.hypixel.hytale.common.util.CompletableFutureUtil;
```

## Threading Model

- Each `World` runs on its own thread (`WorldThread`)
- Entity/player operations MUST run on the owning world's thread
- Use `world.execute(() -> { ... })` to run code on world thread
- `CompletableFuture` operations may run on ForkJoinPool - be careful!

## Async Patterns

### Wrapping async operations safely

```java
CompletableFutureUtil._catch(
    someAsyncOperation()
        .thenRun(() -> { ... })
        .exceptionally(error -> {
            // Handle error
            return null;
        })
);
```

## Events (TODO)

```java
// In setup() or start()
this.getEventRegistry().listen(PlayerConnectEvent.class, event -> {
    PlayerRef playerRef = event.getPlayerRef();
    // Handle event
});
```

## Notes

- `Universe.addWorld(name, generator, storage)` is deprecated but still works
- Alternative: `Universe.makeWorld(name, path, worldConfig)` for more control
- Player must be removed from current world before adding to another
- Always check `context.isPlayer()` before casting to `Player`
