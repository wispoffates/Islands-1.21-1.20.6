package me.aleksilassila.islands.generation;

import me.aleksilassila.islands.Islands;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public enum Biomes {
    INSTANCE;
    private final Islands plugin;

    private List<Biome> allowedBiomes;
    private final int biggestIslandSize;

    int biomeSearchJumpBlocks;
    int biomeSearchSize;
    int maxLocationsPerBiome;
    List<String> biomeBlacklist;

    Biomes() {
        this.plugin = Islands.instance;
        this.biggestIslandSize = plugin.getConfig().getInt("generation.minBiomeSize");

        this.biomeSearchJumpBlocks = plugin.getConfig().getInt("generation.searchJump");
        this.biomeSearchSize = plugin.getConfig().getInt("generation.biomeSearchArea");
        this.maxLocationsPerBiome = plugin.getConfig().getInt("generation.maxVariationsPerBiome");
        this.biomeBlacklist = plugin.getConfig().getStringList("biomeBlacklist");
        this.allowedBiomes = new ArrayList<>();

        for(Biome b : Biome.values()) {
            if(!biomeBlacklist.contains(b.name())) {
                this.allowedBiomes.add(b);
            }
        }
    }

    @Nullable
    private static Biome getTargetBiome(String biome) {
         Biome targetBiome = null;

         for (Biome b : Biome.values()) {
             if (b.name().equalsIgnoreCase(biome)) {
                 targetBiome = b;
             }
         }

         return targetBiome;
    }

    public double isSuitableLocation(Location loc, Biome biome) {
        return isSuitableLocation(
            loc.getWorld(), 
            loc.getBlockX(), 
            loc.getBlockY(), 
            loc.getBlockZ(), 
            biome);
    }

    public double isSuitableLocation(World world, int xCorner, int zCorner, int rectSize, Biome biome) {
        double countWater = 0.0;
        double count = 0.0;
        for (int x = 0; x < rectSize; x += biomeSearchJumpBlocks) {
            for (int z = 0; z < rectSize; z += biomeSearchJumpBlocks) {
               if(world.getBiome(x,60,z) != biome) {
                    countWater++;
               }
               count++;
            }
        }
        return countWater/count;
    }

    public static Biome getRandomBiome() {
        int size = Biomes.INSTANCE.getBiomes().size();
        int item = new Random().nextInt(size);
        return Biomes.INSTANCE.getBiomes().get(item);
    }

    public List<Biome> getBiomes() {
        return this.allowedBiomes;
    }

    public int getBiomeSearchArea() {
        return this.biomeSearchSize;
    }
}
