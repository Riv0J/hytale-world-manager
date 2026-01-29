# WorldManager Plugin

## Structure

```
src/main/java/com/wm/
├── WorldManagerPlugin.java  # Entrypoint
├── WMCommand.java           # /wm command
├── WorldManager.java        # World CRUD
└── WorldTeleportation.java  # Teleport logic
```

**Entrypoint:** `com.wm.WorldManagerPlugin`

## Commands

```
/wm create --name <world> [--type normal/flat/void/dummy]
/wm delete --name <world>
/wm list
/wm tp --name <world>
```

**Permission:** `hytale.command.wm`

## Build & Deploy

```bash
gradlew.bat build
```

Output: `build/libs/WorldManager-x.x.x.jar` → copy to `mods/`

## Save Structure

```
/saves/MyGame/universe/
├── default/        # Main world
└── customWorld/    # Created with /wm
```

## Quick Reference

### Create World
```java
WorldManager.createWorld("name", WorldType.NORMAL);
```

### Teleport Player
```java
WorldTeleportation.teleport(playerRef, "worldName");
```

### Threading (IMPORTANT)
```java
// Entity ops MUST run on world thread
world.execute(() -> {
    playerRef.removeFromStore();
    targetWorld.addPlayer(playerRef, transform);
});
```

## Key APIs

| Class | Purpose |
|-------|---------|
| `Universe.get()` | World manager singleton |
| `universe.getWorld(name)` | Get world |
| `universe.makeWorld(name, path, config)` | Create world |
| `universe.removeWorld(name)` | Delete world |

## Server Packages

- `com.hypixel.hytale.server.core.plugin` - Plugins
- `com.hypixel.hytale.server.core.universe` - Worlds
- `com.hypixel.hytale.server.core.entity` - Entities
- `com.hypixel.hytale.server.core.command` - Commands
