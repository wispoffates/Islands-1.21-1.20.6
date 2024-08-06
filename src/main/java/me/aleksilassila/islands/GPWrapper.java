package me.aleksilassila.islands;

import me.aleksilassila.islands.IslandsConfig.IslandEntry;
import me.aleksilassila.islands.utils.Permissions;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.ClaimPermission;
import me.ryanhamshire.GriefPrevention.ClaimsMode;
import me.ryanhamshire.GriefPrevention.CreateClaimResult;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.concurrent.ConcurrentHashMap;

public class GPWrapper {
    private static boolean enabled = false;
    private static GriefPrevention gp;

    private GPWrapper(){}

    public static void initialise() {
        Islands islands = Islands.instance;
        enabled = islands.getConfig().getBoolean("enableIslandProtection", true);
        if (!enabled) return;
        if (Bukkit.getPluginManager().getPlugin("GriefPrevention") == null) {
            islands.getLogger().severe("No GriefPrevention found. Island protection disabled.");
            enabled = false;
        } else {
            gp = (GriefPrevention) Bukkit.getPluginManager().getPlugin("GriefPrevention");
        }

        if (enabled && islands.getConfig().getBoolean("overrideGriefPreventionWorlds")) {
            ConcurrentHashMap<World, ClaimsMode> modes = gp.config_claims_worldModes;

            modes.put(Islands.islandsWorld, ClaimsMode.SurvivalRequiringClaims);

            if (Islands.wildernessWorld != null)
                modes.put(Islands.wildernessWorld, ClaimsMode.Survival);
        }
    }


    private static void addClaimManager(Claim claim, String uuid) {
        if (!claim.managers.contains(uuid)) {
            claim.managers.add(uuid);
            GPWrapper.gp.dataStore.saveClaim(claim);
        }
    }

    public static long createClaims(IslandEntry entry) {
        if(!enabled) {
            return -1;
        }

        int[][] ipc = IslandsConfig.getIslandPlotCorner(entry.xIndex, entry.zIndex);
        CreateClaimResult r = GPWrapper.gp.dataStore.createClaim(Islands.islandsWorld,
            ipc[0][0], ipc[1][0],
            0, Islands.islandsWorld.getMaxHeight(),
            ipc[0][1], ipc[1][1],
            null, null, null, null);

        if (r.succeeded) {
            long claimId = r.claim.getID();
            int[][] ic = IslandsConfig.getIslandCorner(entry.xIndex, entry.zIndex, entry.size);


            Claim subClaim = GPWrapper.gp.dataStore.createClaim(Islands.islandsWorld,
            ic[0][0], ic[1][0],
            0, Islands.islandsWorld.getMaxHeight(),
            ic[0][1], ic[1][1],
            null, r.claim, null, null).claim;
            if (entry.uuid != null) {
                subClaim.setPermission(entry.uuid.toString(), ClaimPermission.Build);
                addClaimManager(subClaim, entry.uuid.toString());

                Player p = Bukkit.getOfflinePlayer(entry.uuid).getPlayer();

                if (Islands.instance.getConfig().getBoolean("GPAccessWholePlot") ||
                        (p != null && p.hasPermission(Permissions.bypass.interactInPlot))) {
                    r.claim.setPermission(entry.uuid.toString(), ClaimPermission.Build);
                    addClaimManager(r.claim, entry.uuid.toString());
                }
            }

            return claimId;
        } else {
            String reason = (r.claim == null) ? "Claim overlaps a WG region?" : "Claim overlaps existing claim " + r.claim.getID();
            Islands.instance.getLogger().severe("Error creating claim for island at plot " + entry.xIndex + ", " + entry.zIndex + " :: " + reason );
            return -1;
        }
    }

    public static void deleteClaims(IslandEntry entry) {
        if(!enabled) {
            return;
        }

        Claim c = GPWrapper.gp.dataStore.getClaim(entry.claimId);

        if (c == null) {
            int[][] ic = IslandsConfig.getIslandCorner(entry.xIndex, entry.zIndex, entry.size);
            c = GPWrapper.gp.dataStore.getClaimAt(new Location(Islands.islandsWorld, ic[0][0], 50, ic[0][1]), true, true, null);
        }

        if (c != null) {
            GPWrapper.gp.dataStore.deleteClaim(c);
        }
        entry.claimId = -1;
    }
        
}
