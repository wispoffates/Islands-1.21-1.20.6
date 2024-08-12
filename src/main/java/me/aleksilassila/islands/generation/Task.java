package me.aleksilassila.islands.generation;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import me.aleksilassila.islands.generation.IslandGeneration.QueueRunnerTask;


public abstract class Task extends BukkitRunnable {

    QueueRunnerTask parent;

    public void setParent(QueueRunnerTask parent) {
        this.parent = parent;
    }

    public abstract Player getPlayer();
    public abstract String getIslandId();
    public abstract long getDelay();
}