# WorldManager Plugin

## Structure

```
src/main/java/com/wm/
├── WorldManagerPlugin.java  # Entrypoint
├── WMCommand.java           # /wm commands (wrappers)
└── WorldManager.java        # listWorlds() helper
```

**Entrypoint:** `com.wm.WorldManagerPlugin`

## Commands

```
/wm add <world> [--type flat/void/dummy]
/wm remove <world> [--destroy]
/wm list
/wm tp <world>
/wm default <world>
/wm spawn
```

### Examples
```
/wm add lobby --type flat
/wm add arena --type void
/wm add myworld                 # defaults to normal
/wm remove arena                # unloads from memory
/wm remove arena --destroy      # permanently deletes files
/wm tp lobby
/wm default lobby
/wm list                        # shows [default] marker
```

### Wrappers

Most commands are wrappers around Hytale's built-in commands:

| /wm command | Wraps |
|-------------|-------|
| `/wm add <world>` | `/world add <world>` |
| `/wm remove <world>` | `/world remove <world>` |
| `/wm tp <world>` | `/tp world <world>` |
| `/wm default <world>` | `/world setdefault <world>` |
| `/wm spawn` | `/world config spawn` |
| `/wm list` | (own implementation) |

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

### List Worlds
```java
List<WorldManager.WorldInfo> worlds = WorldManager.listWorlds();
```

### Execute Hytale Commands Programmatically
```java
CommandManager.get().handleCommand(sender, "world add myworld");
CommandManager.get().handleCommand(sender, "world remove myworld");
CommandManager.get().handleCommand(sender, "tp world myworld");
```

### Permanently Delete World
```java
World world = Universe.get().getWorld("name");
world.getWorldConfig().setDeleteOnRemove(true);
Universe.get().removeWorld("name");
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
