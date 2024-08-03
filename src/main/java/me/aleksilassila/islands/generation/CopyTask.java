package me.aleksilassila.islands.generation;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import me.aleksilassila.islands.Islands;
import me.aleksilassila.islands.IslandsConfig;
import me.aleksilassila.islands.utils.FastNoiseLite;
import me.aleksilassila.islands.utils.Messages;
import me.aleksilassila.islands.utils.Utils;

public class CopyTask extends Task {
    private Player player;
    private IslandsConfig.Entry island;

    CopyLocation copyLocation;
    CopyLocation clearLocation;
    private World sourceWorld;

    private boolean proceduralShapes;
    private double stalactiteSpacing;
    private int stalactiteHeight;
    static FastNoiseLite generalShape;
    static FastNoiseLite stalactite;

    private boolean clear;
    private boolean paste;
    private int index = 0;
    private int rowsBuiltPerDelay;
    private int rowsClearedPerDelay;
    private int buildDelay;

    private int radius;
    private int clearSize;

    private boolean useShapes = false;

    private int[][] randomPositions = null;

    // Noise offsets
    private int o1;
    private int o2;

    private final Map<Material, Material> replacementMap = new HashMap<>();

    protected CopyTask() {
        Islands plugin = Islands.instance;
        proceduralShapes = plugin.getConfig().getBoolean("useProceduralShapes", false);
        stalactiteHeight = plugin.getConfig().getInt("generation.stalactiteLength", 8);
        stalactiteSpacing = plugin.getConfig().getDouble("generation.stalactiteSpacing", 2);

        double delay = plugin.getConfig().getDouble("generation.generationDelayInTicks");

        if (delay < 1.0) {
            this.buildDelay = 1;
            this.rowsBuiltPerDelay = (int) Math.round(1 / delay);
        } else {
            this.buildDelay = (int) delay;
        }

        rowsClearedPerDelay = rowsBuiltPerDelay * plugin.getConfig().getInt("generation.clearSpeedMultiplier", 3);

        ConfigurationSection section = plugin.getConfig().getConfigurationSection("replaceOnGeneration");

        // Initialize block replacement according to config.yml
        if (section != null) {
            for (String material : section.getKeys(false)) {
                Material materialToReplace = Material.getMaterial(material.toUpperCase());
                Material newMaterial = plugin.getConfig().getString("replaceOnGeneration." + material) != null
                        ? Material.getMaterial(section.getString(material).toUpperCase())
                        : null;

                if (materialToReplace != null && newMaterial != null) {
                    replacementMap.put(materialToReplace, newMaterial);
                    plugin.getLogger().info("Replacing " + materialToReplace.name() + " with " + newMaterial.name());
                } else {
                    if (materialToReplace == null) {
                        plugin.getLogger().warning("Material not found: " + material);
                    }

                    if (newMaterial == null) {
                        plugin.getLogger().warning("Material not found: " + plugin.getConfig().getString("replaceOnGeneration." + material));
                    }
                }
            }
        }
    }

    public CopyTask(Player player, Location sourceLocation, IslandsConfig.Entry island, boolean paste, boolean clear, boolean useShapes, int clearSize) {
        this();

        int[][] corners = IslandsConfig.getIslandCorner(island.xIndex, island.zIndex, island.size);
        int[][] clearCorners = IslandsConfig.getIslandCorner(island.xIndex, island.zIndex, clearSize);
        this.sourceWorld = sourceLocation.getWorld();

        this.copyLocation = new CopyLocation(corners[0][0],
                island.y,
                corners[0][1],
                sourceLocation.getBlockX() - island.size / 2,
                sourceLocation.getBlockY() - island.size / 2,
                sourceLocation.getBlockZ() - island.size / 2);

        this.clearLocation = new CopyLocation(clearCorners[0][0],
                island.y,
                clearCorners[0][1],
                sourceLocation.getBlockX() - clearSize / 2,
                sourceLocation.getBlockY() - clearSize / 2,
                sourceLocation.getBlockZ() - clearSize / 2);

        this.player = player;
        this.island = island;
        this.radius = island.size / 2;
        this.clearSize = clearSize;

        this.clear = clear;
        this.paste = paste;

        this.useShapes = proceduralShapes && useShapes;

        // Stalactite positions + one in the middle
        int[][] items = Utils.randomStalactitePositions(island.size, stalactiteSpacing);
        this.randomPositions = new int[items.length + 1][2];
        System.arraycopy(items, 0, randomPositions, 0, items.length);
        randomPositions[randomPositions.length - 1] = new int[] {radius, radius};

        Random r = new Random();
        o1 = r.nextInt(50000);
        o2 = r.nextInt(50000);
    }

    // For clear command only
    public CopyTask(Player player, IslandsConfig.Entry island) {
        this();
        
        int[][] corners = IslandsConfig.getIslandCorner(island.xIndex, island.zIndex, island.size);
        this.clearLocation = new CopyLocation(corners[0][0],
                island.y,
                corners[0][1],
                0, 0, 0);

        this.player = player;
        this.island = island;
        this.radius = island.size / 2;
        this.clearSize = island.size;

        this.clear = true;
        this.paste = false;
    }

    @Override
    public Player getPlayer() {
        return player;
    }

    @Override
    public String getIslandId() {
        return island.islandId;
    }

    @Override
    public void run() {
        int maxYAdd = (int) (island.size / 2d + 4 * 0.7 + stalactiteHeight);
        int maxYAddClear = (int) (clearSize / 2d + 4 * 0.7 + stalactiteHeight);

        if (clear) { // Clear the area first if necessary
            for (int count = 0; count < rowsClearedPerDelay; count++) {
                int x = index % clearSize;
                int z = index / clearSize;

                boolean skipDelay = true;
                for (int y = -maxYAddClear; y <= clearSize; y++) {
                    CopyLocation l = clearLocation.add(x, y, z);
                    Block ib = l.getIslandBlock();

                    if (!ib.getType().isAir()) { // If there's block there, clear it
                        ib.setType(Material.AIR);
                        skipDelay = false; // Don't skip the delay between iterations, normally true
                    }
                }
                clearBiome(clearLocation.add(x, 0, z).getIslandBlock(), Biome.PLAINS);

                if (skipDelay) count--;

                if (z >= clearSize) {
                    Messages.send(player, "success.CLEARING_DONE");

                    index = 0;
                    clear = false;

                    break;
                } else if (index == IslandsConfig.INSTANCE.islandSpacing * IslandsConfig.INSTANCE.islandSpacing / 4) {
                    Messages.send(player, "info.CLEARING_STATUS", 25);
                } else if (index == IslandsConfig.INSTANCE.islandSpacing * IslandsConfig.INSTANCE.islandSpacing / 2) {
                    Messages.send(player, "info.CLEARING_STATUS", 50);
                } else if (index == IslandsConfig.INSTANCE.islandSpacing * IslandsConfig.INSTANCE.islandSpacing / 4 * 3) {
                    Messages.send(player, "info.CLEARING_STATUS", 75);
                }

                index++;
            }

            return;
        }

        if (!paste) {
             IslandGeneration.queue.remove(this);

            if ( !IslandGeneration.queue.isEmpty()) {
                Task nextTask =  IslandGeneration.queue.get(0);
                nextTask.runTaskTimer(Islands.instance, 0, buildDelay);
            }

            this.cancel();
            return;
        }

        // Paste the blocks
        for (int count = 0; count < rowsBuiltPerDelay; count++) {
            int x = index % island.size;
            int z = index / island.size;

            for (int y = island.size; y >= -maxYAdd; y--) {
                CopyLocation l = copyLocation.add(x, y, z);
                Block ib = l.getIslandBlock();
                Block sb = l.getSourceBlock();

                if(!sb.getChunk().isLoaded()) {
                    Islands.instance.getLogger().info("Chunk was not loaded!");
                    sb.getChunk().load(true);
                }

                BlockData data = sb.getBlockData();

                // Check if block should be replaced according to config.yml
                if (replacementMap.containsKey(sb.getType())) {
                    data = replacementMap.get(sb.getType()).createBlockData();
                }

                double yAdd = getShapeNoise(Islands.islandsWorld, x, z, randomPositions, island.size);

                if (!useShapes || (y > radius)) {
                    if (isBlockInIslandShape(x, y, z, island.size))
                        ib.setBlockData(data);
                    else ib.setType(Material.AIR);
                } else if (radius - y < yAdd) {
                    ib.setBlockData(data);
                    if (data.getMaterial().isAir()) // Remove floating stalactite here
                        if (y <= 0 && radius - y - yAdd < 8) break;
                } else ib.setType(Material.AIR);

                ib.setBiome(sb.getBiome());
            }

            // Extend biomes up and down
            Biome upBiome = copyLocation.add(x, island.size, z).getSourceBlock().getBiome();
            Biome downBiome = copyLocation.add(x, -maxYAdd, z).getSourceBlock().getBiome();
            Block ib = copyLocation.add(x, 0, z).getIslandBlock();
            for (int y = ib.getY() + island.size; y < ib.getWorld().getMaxHeight(); y++)
                Islands.islandsWorld.setBiome(ib.getX(), y, ib.getZ(), upBiome);
            for (int y = ib.getY() - maxYAdd; y > 0; y--)
                Islands.islandsWorld.setBiome(ib.getX(), y, ib.getZ(), downBiome);



            // If done
            if (index >= island.size * island.size) {
                // Update lighting
                Islands.islandsWorld.getChunkAt(copyLocation.getBlockX() + radius, copyLocation.getBlockZ() + radius);
                //update spawn location
                Location spawn = island.getIslandSpawn();
                int y = Islands.islandsWorld.getHighestBlockYAt(spawn);
                spawn.setY(y);
                island.setSpawnPosition(spawn);

                player.sendMessage(Messages.get("success.GENERATION_DONE"));
                paste = false;
                break;
            } else if (index == island.size * island.size / 4) {
                player.sendMessage(Messages.get("info.GENERATION_STATUS", 25));
            } else if (index == island.size * island.size / 2) {
                player.sendMessage(Messages.get("info.GENERATION_STATUS", 50));
            } else if (index == island.size * island.size / 4 * 3) {
                player.sendMessage(Messages.get("info.GENERATION_STATUS", 75));
            }

            index++;
        }
    }

    void clearBiome(Block block, Biome biome) {
        for (int y = 0; y < block.getWorld().getMaxHeight(); y++) {
            block.getWorld().setBiome(block.getX(), y, block.getZ(), biome);
        }
    }

    private static final double curvature = 5;

    double getShapeNoise(World world, int x, int z, int[][] positions, int size)  {
        double factor = Math.max(0, 1 - Math.sqrt((Math.pow(x - size / 2d, 2) + Math.pow(z - size / 2d, 2)) / (Math.pow(size, 2) / 4d)));
        if (factor <= 0) return 0;

        if (generalShape == null) { // Randomize the general shape
            generalShape = new FastNoiseLite((int) Math.round(world.getSeed() / (double) Long.MAX_VALUE * Integer.MAX_VALUE)); // *Troll face*
            generalShape.SetNoiseType(FastNoiseLite.NoiseType.Perlin);
            generalShape.SetFrequency(0.09f);
            generalShape.SetFractalOctaves(2);
        }

        if (stalactite == null) { // Randomize stalactite
            stalactite = new FastNoiseLite((int) Math.round(world.getSeed() / (double) Long.MAX_VALUE * Integer.MAX_VALUE)); // *Troll face*
            stalactite.SetNoiseType(FastNoiseLite.NoiseType.ValueCubic);
            stalactite.SetFrequency(0.2f);
            stalactite.SetFractalOctaves(2);
            stalactite.SetFractalGain(0.2f);
        }

        double base = size / 2d * (0.5 * Math.pow(factor, 2.5 / curvature) + 0.5 * Math.pow(factor, curvature / 1.4));
        double generalDetails = generalShape.GetNoise(x + o1, z + o2) * 4;

        double dist = stalactiteSpacing;
        for (int[] pos : positions) {
            double d = Math.sqrt(Math.pow(x - pos[0], 2) + Math.pow(z - pos[1], 2));
            dist = Math.min(d / stalactiteSpacing, dist);
        }

        double fineDetails = stalactiteHeight * (1 / Math.pow(dist + 1, 2)) * (1 + stalactite.GetNoise(x + o1, z + o2));

        return base + Math.pow(factor, 0.2) * generalDetails + Math.pow(factor, 0.1) * fineDetails;
    }

        /**
     * Check if the block is inside the egg-shape (not sphere!!) of the island,
     * the blocks should be in range is 0<=x<=islandSize
     *
     * @param x x coordinate relative to the position of the island.
     * @param y y coordinate relative to the position of the island.
     * @param z z coordinate relative to the position of the island.
     * @param islandSize Size of the island (diameter of the sphere)
     * @return true if the block is inside
     */
    public static boolean isBlockInIslandShape(int x, int y, int z, int islandSize) {
        return (Math.pow(x - islandSize / 2.0, 2) + (islandSize / Math.pow(y, 2) + 1.3) * Math.pow(y - islandSize / 2.0, 2) + Math.pow(z - islandSize / 2.0, 2))
                <= Math.pow(islandSize / 2.0, 2);
    }

    /**
     * Check if the block is inside sphere with diameter of islandSize,
     * the blocks should be in range is 0<=x<=islandSize
     *
     * @param x x coordinate relative to the position of the island.
     * @param y y coordinate relative to the position of the island.
     * @param z z coordinate relative to the position of the island.
     * @param islandSize Size of the island (diameter of the sphere)
     * @return true if the block is inside
     */
    public static boolean isBlockInIslandSphere(int x, int y, int z, int islandSize) {
        return (Math.pow(x - islandSize / 2.0, 2) + Math.pow(y - islandSize / 2.0, 2) + Math.pow(z - islandSize / 2.0, 2))
                <= Math.pow(islandSize / 2.0, 2);
    }

    //

    /**
     * Check if the block is inside cylinder with diameter of islandSize,
     * ignoring height (y), the blocks should be in range is 0<=x<=islandSize
     *
     * @param relativeX x coordinate relative to the position of the island.
     * @param relativeZ z coordinate relative to the position of the island.
     * @param islandSize Size of the island (diameter of the cylinder)
     * @return true if the block is inside
     */
    public static boolean isBlockInIslandCylinder(int relativeX, int relativeZ, int islandSize) {
        return (Math.pow(relativeX - islandSize / 2.0, 2) + Math.pow(relativeZ - islandSize / 2.0, 2))
                <= Math.pow(islandSize / 2.0, 2);
    }


    private class CopyLocation extends Location {
        double sx, sy, sz;

        /**
         * @param ix islandsWorld x
         * @param sx islandsSourceWorld x
         */
        public CopyLocation(double ix, double iy, double iz, double sx, double sy, double sz) {
            super(Islands.islandsWorld, ix, iy, iz);
            this.sx = sx;
            this.sy = sy;
            this.sz = sz;
        }

        @Override
        public CopyLocation add(double x, double y, double z) {
            return new CopyLocation(getX() + x, getY() + y, getZ() + z, sx + x, sy + y, sz + z);
        }

        public Block getIslandBlock() {
            return Islands.islandsWorld.getBlockAt(this);
        }

        public Block getSourceBlock() {
            return sourceWorld.getBlockAt((int) Math.round(sx), (int) Math.round(sy), (int) Math.round(sz));
        }
    }

}
