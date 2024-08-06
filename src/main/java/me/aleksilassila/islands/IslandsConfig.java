package me.aleksilassila.islands;

import me.aleksilassila.islands.generation.CopyTask;
import me.aleksilassila.islands.utils.BiomeMaterials;
import me.aleksilassila.islands.utils.Permissions;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.ClaimPermission;
import me.ryanhamshire.GriefPrevention.CreateClaimResult;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Biome;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.*;

public enum IslandsConfig {
    INSTANCE;

    public final int islandSpacing;
    public final int verticalSpacing;
    public final boolean islandDamage;

    public static Map<String, IslandEntry> entries;
    public static IslandEntry spawnIsland = null;

    private static FileConfiguration config;
    private static File configFile;

    IslandsConfig() {
        this.islandSpacing = Islands.instance.getConfig().getInt("generation.islandGridSpacing");
        this.verticalSpacing = Islands.instance.getConfig().getInt("generation.islandGridVerticalSpacing");
        this.islandDamage = Islands.instance.getConfig().getBoolean("islandDamage", false);
    }

    public static FileConfiguration getConfig() {
        if (config != null) return config;

        configFile = new File(Islands.instance.getDataFolder(), "islands.yml");
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            Islands.instance.saveResource("islands.yml", false);
         }

        config = new YamlConfiguration();
        try {
            config.load(configFile);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }

        entries = loadEntries();

        return config;
    }

    public static void saveIslandsConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            Islands.instance.getLogger().severe("Unable to save islandsConfig");
        }
    }

    public static Map<String, IslandEntry> loadEntries() {
        HashMap<String, IslandEntry> entries = new HashMap<>();
        for (String islandId : getConfig().getKeys(false)) {
            IslandEntry e = new IslandEntry(islandId);
            entries.put(islandId, e);
            if (e.isSpawn) spawnIsland = e;
        }

        return entries;
    }

    public static void updateEntries() {
        for (String islandId : entries.keySet()) {
            IslandEntry e = entries.get(islandId);
            if (e.shouldUpdate) {
                e.writeToConfig();
                e.shouldUpdate = false;
            }
        }

        saveIslandsConfig();
    }

    @Nullable
    public static IslandEntry getEntry(int x, int z, boolean useRawCoordinates) {
        if (!useRawCoordinates) return getEntry(x, z);

        int xIndex = x / INSTANCE.islandSpacing;
        int zIndex = z / INSTANCE.islandSpacing;

        return getEntry(xIndex, zIndex);
    }

    @Nullable
    public static IslandEntry getEntry(int xIndex, int zIndex) { // fixme not finding some islands by raw coordinates
        for (IslandEntry e : entries.values()) {
            if (e.xIndex == xIndex && e.zIndex == zIndex) return e;
        }

        return null;
    }

    public static IslandEntry createIsland(UUID uuid, int islandSize, int height, Biome biome) {
        int index = 0;

        Set<String> islands = entries.keySet();

        while (true) {
            int[] pos = Placement.getIslandPos(index);

            if (!islands.contains(posToIslandId(pos[0], pos[1]))) {
                return addIsland(pos[0], pos[1], islandSize, height, uuid, String.valueOf(getNewHomeId(uuid)), biome);
            }

            index++;
        }
    }

    @NotNull
    private static IslandEntry addIsland(int xIndex, int zIndex, int islandSize, int height, UUID uuid, String name, Biome biome) {
        String islandId = posToIslandId(xIndex, zIndex);
        IslandEntry e = new IslandEntry(xIndex, zIndex, islandSize, height, uuid, name, biome);
        entries.put(islandId, e);
        e.writeToConfig();
        saveIslandsConfig();
        return e;
    }

    @NotNull
    public static List<IslandEntry> getOwnedIslands(UUID uuid) {
        List<IslandEntry> islands = new ArrayList<>();

        for (IslandEntry e : entries.values()) {
            if (uuid.equals(e.uuid)) islands.add(e);
        }

        return islands;
    }

    @NotNull
    public static Map<String, Map<String, String>> getIslandsInfo(boolean publicOnly) {
        Map<String, Map<String, String>> islands = new HashMap<>();

        for (String islandId : entries.keySet()) {
            IslandEntry e = entries.get(islandId);

            if (!publicOnly || e.isPublic) {
                String name = e.isPublic ? e.name : islandId;

                Map<String, String> values = new HashMap<>();
                values.put("name", name);
                values.put("owner", e.uuid != null ? e.uuid.toString() : "Server");

                try {
                    String biome = e.biome.toString();
                    values.put("material", BiomeMaterials.valueOf(biome).name());
                } catch (Exception exception) {
                    values.put("material", BiomeMaterials.DEFAULT.name());
                }

                values.put("public", String.valueOf(e.isPublic ? 1 : 0));

                islands.put(islandId, values);
            }
        }

        return islands;
    }

    @NotNull
    public static Map<String, Map<String, String>> getIslandsInfo(UUID uuid) {
        Map<String, Map<String, String>> islands = getIslandsInfo(false);
        Map<String, Map<String, String>> finalIslands = new HashMap<>();

        for (String islandId : entries.keySet()) {
            IslandEntry e = entries.get(islandId);
            if (islands.containsKey(islandId) && uuid.equals(e.uuid))
                finalIslands.put(islandId, islands.get(islandId));
        }

        return finalIslands;
    }

    @NotNull
    public static Map<UUID, Integer> getIslandOwners() {
        Map<UUID, Integer> players = new HashMap<>();

        for (IslandEntry e : entries.values()) {
            if (e.uuid != null) {
                if (players.containsKey(e.uuid)) {
                    players.put(e.uuid, players.get(e.uuid) + 1);
                } else {
                    players.put(e.uuid, 1);
                }
            }
        }

        return players;
    }

    @Nullable
    public static IslandEntry getIslandByName(String name) {
        for (IslandEntry e : entries.values()) {
            if (name.equalsIgnoreCase(e.name) && e.isPublic) {
                return e;
            }
        }

        return null;
    }

    @Nullable
    public static IslandEntry getHomeIsland(UUID uuid, int homeId) {
        List<IslandEntry> allIslands = getOwnedIslands(uuid);

        for (IslandEntry e : allIslands) {
            if (e.homeId == homeId) {
                return e;
            }
        }

        return null;
    }

    public static int getLowestHome(UUID uuid) {
        List<IslandEntry> allIslands = getOwnedIslands(uuid);

        int lowestHome = -1;

        for (IslandEntry e : allIslands) {
            if (e.homeId != -1 && (e.homeId < lowestHome || lowestHome == -1)) {
                lowestHome = e.homeId;
            }
        }

        return lowestHome;
    }

    /**
     * Checks if block (relative to the position of
     * the island) is inside water flow are.
     *
     * From bottom to up the area is first a half sphere with diameter
     * of the island width and then a cylinder with same diameter.
     */
    public static boolean isBlockInWaterFlowArea(int x, int y, int z) {
        int xIndex = x / INSTANCE.islandSpacing;
        int zIndex = z / INSTANCE.islandSpacing;

        IslandEntry e = getEntry(xIndex, zIndex);
        if (e == null)
            return false;

        int[][] ic = getIslandCorner(xIndex, zIndex, e.size);

        int relativeX = x - ic[0][0];
        int relativeZ = z - ic[0][1];
        int relativeY = y - getIslandY(xIndex, zIndex);

        if (relativeY <= e.size / 2d) {
            return CopyTask.isBlockInIslandSphere(relativeX, relativeY, relativeZ, e.size);
        } else {
            return CopyTask.isBlockInIslandCylinder(relativeX, relativeZ, e.size);
        }
    }

    public static int getNewHomeId(UUID uuid) {
        List<Integer> homeIds = new ArrayList<>();

        for (IslandEntry e : getOwnedIslands(uuid)) {
            homeIds.add(e.homeId);
        }

        for (int i = 1; i < Integer.MAX_VALUE; i++) {
            if (!homeIds.contains(i)) return i;
        }

        return 0;
    }

    // UTILS

    private static int getIslandY(int xIndex, int zIndex) {
        return 10 + ((xIndex + zIndex) % 3) * INSTANCE.verticalSpacing;
    }

    static String posToIslandId(int xIndex, int zIndex) {
        return xIndex + "x" + zIndex;
    }

    public static int[][] getIslandCorner(int xIndex, int zIndex, int size) {
        return new int[][] {
                new int[] {
                        xIndex * INSTANCE.islandSpacing + INSTANCE.islandSpacing / 2 - size / 2,
                        zIndex * INSTANCE.islandSpacing + INSTANCE.islandSpacing / 2 - size / 2
                }, new int[] {
                        xIndex * INSTANCE.islandSpacing + INSTANCE.islandSpacing / 2 + size / 2 - 1,
                        zIndex * INSTANCE.islandSpacing + INSTANCE.islandSpacing / 2 + size / 2 - 1
                }
        };
    }

    public static int[][] getIslandPlotCorner(int xIndex, int zIndex) {
        return new int[][] {
                new int[] {
                        xIndex * INSTANCE.islandSpacing,
                        zIndex * INSTANCE.islandSpacing
                }, new int[] {
                        xIndex * INSTANCE.islandSpacing + INSTANCE.islandSpacing - 1,
                        zIndex * INSTANCE.islandSpacing + INSTANCE.islandSpacing - 1
                }
        };
    }

    public static class IslandEntry {
        public String islandId;
        public int xIndex;
        public int zIndex;
        public int size;
        public int height;
        public long claimId;
        public Biome biome;
        public UUID uuid;
        public String name;
        public int homeId;
        public boolean isPublic;
        public int y;
        private Location spawnPosition;
        public boolean isSpawn;

        boolean shouldUpdate = false;

        public IslandEntry(String islandId) {
            FileConfiguration fc = getConfig();
            this.islandId = islandId;

            this.xIndex = fc.getInt(islandId + ".xIndex");
            this.zIndex = fc.getInt(islandId + ".zIndex");

            try {
                this.uuid = UUID.fromString(fc.getString(islandId + ".UUID"));
            } catch (Exception ignored) {}
            this.homeId = fc.getInt(islandId + ".home");
            this.name = fc.getString(islandId + ".name", String.valueOf(homeId));
            this.size = fc.getInt(islandId + ".size");
            this.height = fc.getInt(islandId + ".height");
            this.isPublic = fc.getBoolean(islandId + ".public", false);
            this.biome = Biome.valueOf(fc.getString(islandId + ".biome", "PLAINS"));
            this.homeId = fc.getInt(islandId + ".home", -1);
            this.spawnPosition = new Location(Islands.islandsWorld, 
                fc.getInt(islandId + ".spawnPoint.x", 0),  
                fc.getInt(islandId + ".spawnPoint.y", Islands.islandsWorld.getMinHeight()), 
                fc.getInt(islandId + ".spawnPoint.z", 0),
                fc.getInt(islandId + ".spawnPoint.yaw", 0),
                fc.getInt(islandId + ".spawnPoint.pitch", 0) 
            );
            //failsafe in case spawn Y isnt set
            if(this.spawnPosition.getY() == Islands.islandsWorld.getMinHeight()) {
                this.spawnPosition.setY(Islands.islandsWorld.getHighestBlockYAt(this.spawnPosition.getBlockX(), this.spawnPosition.getBlockZ()));
            }
            this.y = fc.getInt(islandId + ".y");
            this.isSpawn = fc.getBoolean(islandId + ".isSpawn", false);

            this.claimId = fc.getLong(islandId + ".claimId", -1);
            //TODO: Check if claim is in database if not try and recreate it
            //Fix missing claims.  Usually trigged if GP is added after the island was created.
            if (this.claimId == -1) {
                GPWrapper.deleteClaims(this);
                this.claimId = GPWrapper.createClaims(this);
                if(this.claimId != -1) {
                    this.shouldUpdate = true;
                }
            }
        }

        public IslandEntry(int xIndex, int zIndex, int size, int height, UUID uuid, String name, Biome biome) {
            this.islandId = posToIslandId(xIndex, zIndex);
            this.xIndex = xIndex;
            this.zIndex = zIndex;
            this.size = size;
            this.height = height;
            this.uuid = uuid;
            this.name = name;
            this.biome = biome;
            this.homeId = getNewHomeId(uuid);

            this.claimId = GPWrapper.createClaims(this);

            int[][] ic = getIslandCorner(xIndex, zIndex, size);
            this.spawnPosition = new Location(Islands.islandsWorld, 
                ic[0][0] + size / 2.0,  
                Islands.islandsWorld.getMaxHeight(), //start at max heigh here  
                ic[0][1] + size / 2.0
            );

            this.y = getIslandY(xIndex, zIndex);

            this.isPublic = false;
            this.isSpawn = false;

        }

        public void delete() {
            getConfig().set(islandId, null);
            GPWrapper.deleteClaims(this);
            entries.remove(islandId);

            saveIslandsConfig();
        }

        public void writeToConfig() {
            //why do we recompute these?
            int[][] ic = getIslandCorner(xIndex, zIndex, size);
            String islandId = posToIslandId(xIndex, zIndex);

            getConfig().set(islandId + ".xIndex", xIndex);
            getConfig().set(islandId + ".zIndex", zIndex);

            getConfig().set(islandId + ".x", ic[0][0]);
            getConfig().set(islandId + ".y", y);
            getConfig().set(islandId + ".z", ic[0][1]);

            getConfig().set(islandId + ".spawnPoint.x", spawnPosition.getX());
            getConfig().set(islandId + ".spawnPoint.y", spawnPosition.getY());
            getConfig().set(islandId + ".spawnPoint.z", spawnPosition.getZ());
            getConfig().set(islandId + ".spawnPoint.yaw", spawnPosition.getYaw());
            getConfig().set(islandId + ".spawnPoint.pitch", spawnPosition.getPitch());

            getConfig().set(islandId + ".UUID", uuid == null ? "Server" : uuid.toString());
            getConfig().set(islandId + ".name", name);
            getConfig().set(islandId + ".home", homeId);
            getConfig().set(islandId + ".size", size);
            getConfig().set(islandId + ".height", height);
            getConfig().set(islandId + ".public", isPublic);
            getConfig().set(islandId + ".biome", biome.name());

            getConfig().set(islandId + ".claimId", claimId);
            getConfig().set(islandId + ".isSpawn", isSpawn);
        }

        public void setSpawnPosition(Location position) {
            spawnPosition = position;
            shouldUpdate = true;
        }

        @NotNull
        public Location getIslandSpawn() {
            return spawnPosition.clone();
        }

        public void unnameIsland() {
            name = String.valueOf(homeId);
            isPublic = false;

            shouldUpdate = true;
        }

        public void nameIsland(String name) {
            this.isPublic = true;
            this.name = name;

            shouldUpdate = true;
        }

        public void giveIsland(OfflinePlayer player) {
            this.uuid = player.getUniqueId();
            this.homeId = getNewHomeId(player.getUniqueId());
            GPWrapper.deleteClaims(this);
            this.claimId = GPWrapper.createClaims(this);

            shouldUpdate = true;
        }

        public void giveToServer() {
            this.uuid = null;
            this.homeId = -1;
            GPWrapper.deleteClaims(this);
            GPWrapper.createClaims(this);
            shouldUpdate = true;
        }

        public void setSpawnIsland() {
            isSpawn = !isSpawn;
            IslandsConfig.spawnIsland = isSpawn ? this : null;

            shouldUpdate = true;
        }

        public void teleport(Player player) {
            if (INSTANCE.islandDamage)
                Islands.instance.playersWithNoFall.add(player);
            player.teleport(getIslandSpawn());
        }
    }

    public static class Placement {
        private Placement(){}

        public static int getLayer(int index) {
            return (int) Math.floor(Math.sqrt(index));
        }

        public static int getLayerSize(int layer) {
            return 2 * layer + 1;
        }

        public static int firstOfLayer(int layer) {
            return layer * layer;
        }

        public static int[] getIslandPos(int index) {
            int layer = getLayer(index);

            int x = Math.min(index - firstOfLayer(layer), layer);
            int z = (index - firstOfLayer(layer) < layer + 1) ? layer : firstOfLayer(layer) + getLayerSize(layer) - 1 - index;

            return new int[]{x, z};
        }

        // TODO: Optimize
        public static int getIslandIndex(int[] pos) {
            int index = 0;
            while (!Arrays.equals(getIslandPos(index), pos)) {
                index++;
            }

            return index;
        }

        public static int getIslandIndex(String islandId) {
            try {
                return getIslandIndex(new int[]{Integer.parseInt(islandId.split("x")[0]), Integer.parseInt(islandId.split("x")[1])});
            } catch (NumberFormatException e) {
                return -1;
            }
        }
    }
}
