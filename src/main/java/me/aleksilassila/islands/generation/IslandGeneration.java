package me.aleksilassila.islands.generation;

import me.aleksilassila.islands.Islands;
import me.aleksilassila.islands.IslandsConfig;
import me.aleksilassila.islands.utils.Messages;
import me.aleksilassila.islands.utils.Permissions;

import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.block.Biome;
import org.bukkit.block.data.type.Switch.Face;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;

public enum IslandGeneration {
    INSTANCE;

    private final Islands plugin;

    public static final List<Task> queue = new ArrayList<>();
    private final int buildDelay;

    IslandGeneration() {
        this.plugin = Islands.instance;

        double delay = plugin.getConfig().getDouble("generation.generationDelayInTicks");

        if (delay < 1.0) {
            this.buildDelay = 1;
        } else {
            this.buildDelay = (int) delay;
        }
    }

    /** Create a source world for the given biome or load one if it all ready exists. */
    public World createSourceWorld(Biome biome) {
        String safeName = "IslandSource_"+biome.name();

        WorldCreator wc = new WorldCreator(safeName);
        wc.environment(World.Environment.NORMAL);
        wc.type(WorldType.LARGE_BIOMES);
        wc.generateStructures(false);
        wc.keepSpawnInMemory(false);
        wc.biomeProvider(new SingleBiomeProvider(biome));
        World world = wc.createWorld();
        world.setDifficulty(Difficulty.PEACEFUL);
        
        return world;
    }

    public boolean copyIsland(Player player, IslandsConfig.Entry updatedIsland, boolean shouldClearArea, boolean noShape, int oldSize) {
        if (!canAddQueueItem(player))
            return false;

        //create a world to use as the source
        plugin.getLogger().info("Creating Island for " + player.getName() + " loading spawn world with biome " + updatedIsland.biome);
        World sourceWorld = this.createSourceWorld(updatedIsland.biome);
        Random random = new Random(System.currentTimeMillis());

        //randomize the location with an range to give some variety.
        Location sourceLocation = null;
        for(int i=0; i<1000; i++) {
            sourceLocation = new Location(sourceWorld, random.nextInt(Biomes.INSTANCE.getBiomeSearchArea())-updatedIsland.size, 0, random.nextInt(Biomes.INSTANCE.getBiomeSearchArea())-updatedIsland.size);
            //check if we have a suitable location break out of the loop
            double waterPercent = Biomes.INSTANCE.isSuitableLocation(sourceLocation,updatedIsland.biome);
            if(waterPercent<0.5) {
                plugin.getLogger().info("Found suitable location after " + i+1 + " attempts.");
                break;
            } else {
                plugin.getLogger().info("Rejected location " + i + " with water percentage of " + waterPercent);
            }
        }

        //check if we failed to find a suitable location 
        if(sourceLocation == null) {
            player.sendMessage(Messages.get("error.NO_LOCATIONS_FOR_BIOME"));
            //TODO: probably should delete the world here so it can recreated.
            return false;
        }
        

        // Get island center y. Center block will be in the middle the first block
        // that is not burnable
        //Start the height search at the hightest block and not 100
        int centerY = sourceLocation.getWorld().getHighestBlockYAt(sourceLocation);
        while (true) {
            int centerX = (int) (sourceLocation.getBlockX() + updatedIsland.size / 2.0);
            int centerZ = (int) (sourceLocation.getBlockZ() + updatedIsland.size / 2.0);

            Material material = sourceWorld.getBlockAt(centerX, centerY, centerZ).getBlockData().getMaterial();
            if (!material.isAir() && !material.isBurnable())
                if (material != Material.MUSHROOM_STEM
                        && material != Material.BROWN_MUSHROOM_BLOCK
                        && material != Material.RED_MUSHROOM_BLOCK) {
                    break;
                }

            centerY--;
        }

        sourceLocation.setY(centerY);
        plugin.getLogger().info("Creating Island for " + player.getName() + " copying from location: " + sourceLocation.toString() + " to location: " + updatedIsland.getIslandSpawn());

        CopyTask task = new CopyTask(player, sourceLocation, updatedIsland, true, shouldClearArea, !noShape, oldSize);

        if (queue.isEmpty()) {
            task.runTaskTimer(plugin, 0, buildDelay);
        }

        addToQueue(task);

        return true;
    }

    public boolean clearIsland(Player player, IslandsConfig.Entry island) {
        if (!canAddQueueItem(player))
            return false;

        CopyTask task = new CopyTask(player, island);

        if (queue.isEmpty()) {
            task.runTaskTimer(plugin, 0, buildDelay);
        }

        addToQueue(task);
        return true;
    }

    public void addToQueue(Task task) {
        if (queueContainsPlayer(task.getPlayer()) && !task.getPlayer().hasPermission(Permissions.bypass.queueLimit)) {
            removeFromQueue(task.getPlayer());
        }

        if (task.getPlayer().hasPermission(Permissions.bypass.queue)) {
            int index = getBypassIndex(task.getPlayer());
            queue.add(index, task);
            if (queue.size() > 1) {
                Messages.send(task.getPlayer(), "info.QUEUE_STATUS", index);
            }
        } else {
            queue.add(task);
            if (queue.size() > 1) {
                Messages.send(task.getPlayer(), "info.QUEUE_STATUS", queue.size() - 1);
            }
        }
    }

    public boolean canAddQueueItem(Player player) {
        if (queue.isEmpty()) return true;
        return !queue.get(0).getPlayer().equals(player) || player.hasPermission(Permissions.bypass.queueLimit);
    }

    public void removeFromQueue(Player player) {
        int index = 0;
        for (Task task : queue) {
            if (index != 0 && task.getPlayer().getUniqueId().equals(player.getUniqueId())) {
                queue.remove(task);

                return;
            }
            index++;
        }
    }

    public int getBypassIndex(Player player) {
        if (queue.size() < 2) return queue.size();
        else {
            for (int i = 1; i < queue.size(); i++) {
                if (!queue.get(i).getPlayer().getUniqueId().equals(player.getUniqueId()))
                    return i;
            }

            return queue.size();
        }
    }

    private boolean queueContainsPlayer(Player player) {
        for (Task item : queue) {
            if (item.getPlayer().getUniqueId().equals(player.getUniqueId())) return true;
        }

        return false;
    }

    public boolean worldExists(String name) {
        try {
            for (File f : Bukkit.getWorldContainer().listFiles()) {
                if (f.getName().equals(name)) return true;
            }
        } catch (NullPointerException e) {
            return false;
        }

        return false;
    }
}
