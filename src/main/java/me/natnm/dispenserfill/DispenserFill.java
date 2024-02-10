package me.natnm.dispenserfill;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class DispenserFill extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic
        Objects.requireNonNull(this.getCommand("filldispensers")).setExecutor(new DispenserFillCommand(this)); // Register the command

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
