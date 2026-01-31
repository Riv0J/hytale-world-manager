# Summary
Adds /wm commands (better /world commands) for advanced and more comfortable world management.

# Description
Features:

Adds commands for server admins (require /op to execute)

- `/wm add <world> [--type normal/flat/void/dummy]` - Create a new world
- `/wm remove <world> [--destroy]` - Unload world (--destroy to permanently delete)
- `/wm list` - List all loaded worlds
- `/wm tp <world>` - Teleport to a world
- `/wm default <world>` - Set the default spawn world
- `/wm spawn` - Set world spawn at current position

Using these commands, you will be able to easily manage multiple worlds.
This is specially useful, as normal players will be able to travel between them using the native /warp command.


For example, you have a normal world and want to create a resource world (where players go to farm resources): 

- Make sure you run "/op self" if you are not already an operator
- Create it using "/wm add Resources"
- TP to it using "/wm tp Resources"
- [optional] Set a warp for players to travel to this world: "/warp set Resources". Players will be able to use "/warp Resources"

Additionally, you can specify the world type you want to create, for example:
/wm add new_world --type [normal/flat/void/dummy]