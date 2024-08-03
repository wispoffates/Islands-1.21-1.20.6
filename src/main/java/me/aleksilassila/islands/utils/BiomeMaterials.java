package me.aleksilassila.islands.utils;

import org.bukkit.Material;

public enum BiomeMaterials {
    OCEAN(Material.WATER_BUCKET),
    PLAINS(Material.GRASS_BLOCK),
    DESERT(Material.SAND),
    MOUNTAINS(Material.STONE),
    FOREST(Material.OAK_LOG),
    TAIGA(Material.SPRUCE_LOG),
    SWAMP(Material.LILY_PAD),
    RIVER(Material.WATER_BUCKET),
    NETHER_WASTES(Material.NETHERRACK),
    THE_END(Material.END_STONE),
    FROZEN_OCEAN(Material.ICE),
    FROZEN_RIVER(Material.ICE),
    SNOWY_TUNDRA(Material.SNOW_BLOCK),
    SNOWY_MOUNTAINS(Material.SNOW_BLOCK),
    MUSHROOM_FIELDS(Material.RED_MUSHROOM_BLOCK),
    MUSHROOM_FIELD_SHORE(Material.RED_MUSHROOM_BLOCK),
    BEACH(Material.SAND),
    DESERT_HILLS(Material.SAND),
    WOODED_HILLS(Material.OAK_LOG),
    TAIGA_HILLS(Material.SPRUCE_LOG),
    MOUNTAIN_EDGE(Material.STONE),
    JUNGLE(Material.JUNGLE_LOG),
    JUNGLE_HILLS(Material.JUNGLE_LOG),
    JUNGLE_EDGE(Material.JUNGLE_LOG),
    DEEP_OCEAN(Material.WATER_BUCKET),
    STONE_SHORE(Material.STONE),
    SNOWY_BEACH(Material.SAND),
    BIRCH_FOREST(Material.BIRCH_LOG),
    BIRCH_FOREST_HILLS(Material.BIRCH_LOG),
    DARK_FOREST(Material.DARK_OAK_LOG),
    SNOWY_TAIGA(Material.SPRUCE_LOG),
    SNOWY_TAIGA_HILLS(Material.SPRUCE_LOG),
    GIANT_TREE_TAIGA(Material.PODZOL),
    GIANT_TREE_TAIGA_HILLS(Material.PODZOL),
    WOODED_MOUNTAINS(Material.OAK_WOOD),
    SAVANNA(Material.ACACIA_LOG),
    SAVANNA_PLATEAU(Material.ACACIA_LOG),
    BADLANDS(Material.TERRACOTTA),
    WOODED_BADLANDS_PLATEAU(Material.TERRACOTTA),
    BADLANDS_PLATEAU(Material.TERRACOTTA),
    SMALL_END_ISLANDS(Material.END_STONE),
    END_MIDLANDS(Material.END_STONE),
    END_HIGHLANDS(Material.END_STONE),
    END_BARRENS(Material.END_STONE),
    WARM_OCEAN(Material.WATER_BUCKET),
    LUKEWARM_OCEAN(Material.FIRE_CORAL),
    COLD_OCEAN(Material.WATER_BUCKET),
    DEEP_WARM_OCEAN(Material.WATER_BUCKET),
    DEEP_LUKEWARM_OCEAN(Material.FIRE_CORAL),
    DEEP_COLD_OCEAN(Material.WATER_BUCKET),
    DEEP_FROZEN_OCEAN(Material.WATER_BUCKET),
    THE_VOID(Material.BREAD),
    SUNFLOWER_PLAINS(Material.SUNFLOWER),
    DESERT_LAKES(Material.SAND),
    GRAVELLY_MOUNTAINS(Material.STONE),
    FLOWER_FOREST(Material.ROSE_BUSH),
    TAIGA_MOUNTAINS(Material.SPRUCE_LOG),
    SWAMP_HILLS(Material.LILY_PAD),
    ICE_SPIKES(Material.ICE),
    MODIFIED_JUNGLE(Material.JUNGLE_LOG),
    MODIFIED_JUNGLE_EDGE(Material.JUNGLE_LOG),
    TALL_BIRCH_FOREST(Material.BIRCH_LOG),
    TALL_BIRCH_HILLS(Material.BIRCH_LOG),
    DARK_FOREST_HILLS(Material.BIRCH_LOG),
    SNOWY_TAIGA_MOUNTAINS(Material.SPRUCE_LOG),
    GIANT_SPRUCE_TAIGA(Material.PODZOL),
    GIANT_SPRUCE_TAIGA_HILLS(Material.PODZOL),
    MODIFIED_GRAVELLY_MOUNTAINS(Material.STONE),
    SHATTERED_SAVANNA(Material.ACACIA_LOG),
    SHATTERED_SAVANNA_PLATEAU(Material.ACACIA_LOG),
    ERODED_BADLANDS(Material.ORANGE_TERRACOTTA),
    MODIFIED_WOODED_BADLANDS_PLATEAU(Material.TERRACOTTA),
    MODIFIED_BADLANDS_PLATEAU(Material.TERRACOTTA),
    BAMBOO_JUNGLE(Material.BAMBOO),
    BAMBOO_JUNGLE_HILLS(Material.BAMBOO),
    SOUL_SAND_VALLEY(Material.SOUL_SAND),
    CRIMSON_FOREST(Material.CRIMSON_STEM),
    WARPED_FOREST(Material.WARPED_STEM),
    BASALT_DELTAS(Material.BASALT),
    WINDSWEPT_HILLS(Material.STONE),
    SNOWY_PLAINS(Material.SNOW_BLOCK),
    SPARSE_JUNGLE(Material.JUNGLE_LOG),
    STONY_SHORE(Material.STONE),
    OLD_GROWTH_PINE_TAIGA(Material.SPRUCE_LOG),
    WINDSWEPT_FOREST(Material.GRASS_BLOCK),
    WOODED_BADLANDS(Material.OAK_LOG),
    WINDSWEPT_GRAVELLY_HILLS(Material.GRAVEL),
    OLD_GROWTH_BIRCH_FOREST(Material.BIRCH_LOG),
    OLD_GROWTH_SPRUCE_TAIGA(Material.SPRUCE_LOG),
    WINDSWEPT_SAVANNA(Material.ACACIA_LOG),
    DRIPSTONE_CAVES(Material.POINTED_DRIPSTONE),
    LUSH_CAVES(Material.VINE),
    MEADOW(Material.GRASS_BLOCK),
    GROVE(Material.SPRUCE_LOG),
    SNOWY_SLOPES(Material.SNOW_BLOCK),
    FROZEN_PEAKS(Material.ICE),
    JAGGED_PEAKS(Material.STONE),
    STONY_PEAKS(Material.STONE),
    MANGROVE_SWAMP(Material.MANGROVE_ROOTS),
    CHERRY_GROVE(Material.CHERRY_LOG),
    DEFAULT(Material.DIRT),
    CUSTOM(Material.DIRT);
    //DEFAULT(null),
    //CUSTOM(null);

    Material material;

    BiomeMaterials(Material material) {
        this.material = material;
    }

    public Material getMaterial() {
        return this.material != null ? this.material : Material.DIRT;
    }

    public static BiomeMaterials of(String name) {
        try {
            return BiomeMaterials.valueOf(name);
        } catch(IllegalArgumentException e) {
            return DEFAULT;
        }
    }
}
