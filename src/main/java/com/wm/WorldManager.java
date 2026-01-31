package com.wm;

import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class WorldManager {

    private WorldManager() {}

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
