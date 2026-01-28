package com.wm;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

public class WorldManagerPlugin extends JavaPlugin {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public WorldManagerPlugin(JavaPluginInit init) {
        super(init);
        LOGGER.atInfo().log("Loading %s version %s", this.getName(), this.getManifest().getVersion().toString());
    }

    @Override
    protected void setup() {
        this.getCommandRegistry().registerCommand(new WMCommand());
        LOGGER.atInfo().log("Registered /wm command");
    }

    @Override
    protected void start() {
        LOGGER.atInfo().log("%s started successfully!", this.getName());
    }

    @Override
    protected void shutdown() {
        LOGGER.atInfo().log("%s shutting down", this.getName());
    }
}
