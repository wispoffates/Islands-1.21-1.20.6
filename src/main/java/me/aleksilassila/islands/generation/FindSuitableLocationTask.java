package me.aleksilassila.islands.generation;

import java.util.Random;

import org.bukkit.Difficulty;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;

import me.aleksilassila.islands.Islands;
import me.aleksilassila.islands.IslandsConfig.IslandEntry;
import net.kyori.adventure.util.TriState;

/**
 * This task searches for a location in the source world matching the wanted biome for the island.
 * 
 * This should probable use a callback to the IslandGeneration rather then spawning the next task itself.
 */
public class FindSuitableLocationTask extends Task {

    private Player player;
    private IslandEntry island;
    private int maxSearchAttempts;
    private int searchAttemptsPerTick;
    private double maxAllowedIncorrectBiome;
    private boolean shouldClearArea;
    private boolean noShape;
    private long buildDelay;

    private World sourceWorld;
    private Random random;
    private Location sourceLocation;
    private double bestPercentSoFar = 1.0;
    private int currentSearchAttempts = 0;

    protected FindSuitableLocationTask(){}

    public FindSuitableLocationTask(IslandEntry island, Player player, boolean shouldClearArea, boolean noShape, long buildDelay) {
        this.player = player;
        this.island = island;
        this.random = new Random(System.currentTimeMillis());

        this.maxSearchAttempts = Islands.instance.getConfig().getInt("generation.maxSearchAttempts",1000);
        this.searchAttemptsPerTick= Islands.instance.getConfig().getInt("generation.maxSearchAttempts",4);
        this.maxAllowedIncorrectBiome = Islands.instance.getConfig().getDouble("generation.maxSearchAttempts",0.1);

        this.shouldClearArea = shouldClearArea;
        this.noShape = noShape;
        this.buildDelay = buildDelay;
    }

    @Override
    public void run() {
        //onle create the source world on the first go around
        if(sourceWorld == null) {
            //create a world to use as the source
            Islands.instance.getLogger().info("Creating Island for " + player.getName() + " loading spawn world with biome " + island.biome);
            sourceWorld = this.createSourceWorld(island.biome);
        }

        //loop through the attempts for this tick
        for(int i=0; i < this.searchAttemptsPerTick; i++) {
            currentSearchAttempts++;
            Location tempLocation = new Location(sourceWorld, random.nextInt(Biomes.INSTANCE.getBiomeSearchArea())-island.size, 0, random.nextInt(Biomes.INSTANCE.getBiomeSearchArea())-island.size);
            //check if we have a suitable location break out of the loop
            double waterPercent = Biomes.INSTANCE.isSuitableLocation(tempLocation,island.biome);
            if(waterPercent < bestPercentSoFar) {
                sourceLocation = tempLocation;
                bestPercentSoFar = waterPercent;
            }
        }
        //havent found a suitable biome and we are not past max attempts return so we can be run again next tick
        if( (bestPercentSoFar > this.maxAllowedIncorrectBiome) && (this.maxSearchAttempts>this.currentSearchAttempts)) {
            Islands.instance.getLogger().info("Rejected locations so far " + currentSearchAttempts + " with best biome percent so far " + (bestPercentSoFar*100) + "%");
            return;
        }
        Islands.instance.getLogger().info("Found suitable location after " + currentSearchAttempts + " attempts with " + (bestPercentSoFar*100) + "% incorrect biome.");
    
        // Get island center y. Center block will be in the middle the first block that is not burnable
        //Start the height search at the hightest block and not 100
        int centerY = sourceLocation.getWorld().getHighestBlockYAt(sourceLocation);
        while (true) {
            int centerX = (int) (sourceLocation.getBlockX() + island.size / 2.0);
            int centerZ = (int) (sourceLocation.getBlockZ() + island.size / 2.0);

            Material material = sourceWorld.getBlockAt(centerX, centerY, centerZ).getBlockData().getMaterial();
            if (!material.isAir() && !material.isBurnable()
                && material != Material.MUSHROOM_STEM
                && material != Material.BROWN_MUSHROOM_BLOCK
                && material != Material.RED_MUSHROOM_BLOCK) {
                    break;
                }

            centerY--;
        }

        sourceLocation.setY(centerY);
        Islands.instance.getLogger().info("Creating Island for " + player.getName() + " copying from location: " + sourceLocation.toString() + " to location: " + island.getIslandSpawn());

        //remove from queue and cancel this task
        IslandGeneration.INSTANCE.removeFromQueue(player);
        this.cancel();

        CopyTask task = new CopyTask(player, sourceLocation, island, true, shouldClearArea, !noShape);
        if (IslandGeneration.INSTANCE.queueIsEmpty()) {
            task.runTaskTimer(Islands.instance, 0, buildDelay);
        }

        IslandGeneration.INSTANCE.addToQueue(task);
    }

    @Override
    public Player getPlayer() {
        return this.player;
    }

    @Override
    public String getIslandId() {
        return this.island.islandId;
    }

    /** Create a source world for the given biome or load one if it all ready exists. */
    private World createSourceWorld(Biome biome) {
        String safeName = "IslandSource_"+biome.name();

        WorldCreator wc = new WorldCreator(safeName);
        wc.environment(World.Environment.NORMAL);
        wc.type(WorldType.NORMAL);
        wc.generateStructures(false);
        wc.keepSpawnLoaded(TriState.FALSE);
        wc.biomeProvider(new IslandBiomeProvider(biome,Islands.wildernessWorld.vanillaBiomeProvider()));
        World world = wc.createWorld();
        world.setDifficulty(Difficulty.PEACEFUL);
        
        return world;
    }
    
}