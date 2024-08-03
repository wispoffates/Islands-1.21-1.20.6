package me.aleksilassila.islands.generation;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import me.aleksilassila.islands.utils.Messages;

public abstract class Task extends BukkitRunnable {
    public abstract Player getPlayer();
    public abstract String getIslandId();

    @Override
    public synchronized BukkitTask runTaskTimer(Plugin plugin, long delay, long period) throws IllegalArgumentException, IllegalStateException {
        Messages.send(getPlayer(), "info.GENERATION_STARTED");

        return super.runTaskTimer(plugin, delay, period);
    }
}