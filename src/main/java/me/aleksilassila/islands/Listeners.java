package me.aleksilassila.islands;

import me.aleksilassila.islands.utils.ChatUtils;
import me.aleksilassila.islands.utils.Messages;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.world.TimeSkipEvent;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Listeners extends ChatUtils implements Listener {
    private final Islands plugin;

    private final boolean disableMobs;
    private final boolean voidTeleport;
    private final boolean useVelocity;
    private final String velocityServer;
    private final boolean onLoginTPToSpawn;
    private final String spawnWorld;
    private final Location spawnLocation;
    private final boolean islandDamage;
    private final boolean restrictFlow;
    private final boolean syncTime;
    private final boolean overrideBedSpawns;
    private final boolean preserveWildernessPositions;

    private final Map<UUID,Integer> teleportAttempts = new HashMap<>();

    public Listeners() {
        this.plugin = Islands.instance;

        this.voidTeleport = plugin.getConfig().getBoolean("voidTeleport");
        this.restrictFlow = plugin.getConfig().getBoolean("restrictIslandBlockFlows");
        this.disableMobs = plugin.getConfig().getBoolean("disableMobsOnIslands");
        this.islandDamage = plugin.getConfig().getBoolean("islandDamage");
        this.syncTime = plugin.getConfig().getBoolean("syncTime");
        this.overrideBedSpawns = plugin.getConfig().getBoolean("overrideBedSpawns");
        this.preserveWildernessPositions = plugin.getConfig().getBoolean("preserveWildernessPositions");
        
        this.onLoginTPToSpawn = plugin.getConfig().getBoolean("onLoginTPToSpawn",false);
        this.spawnWorld = plugin.getConfig().getString("spawnWorld","");        
        List<Double> spawnLocInts = plugin.getConfig().getDoubleList("spawnLocation");
        if(!spawnLocInts.isEmpty()) {       
            //handle if the location is missing yaw and pitch
            if(spawnLocInts.size() == 3) {
                this.spawnLocation = new Location(Bukkit.getWorld(spawnWorld), spawnLocInts.get(0), spawnLocInts.get(1), spawnLocInts.get(2));
            } else {
                this.spawnLocation = new Location(Bukkit.getWorld(spawnWorld), spawnLocInts.get(0), spawnLocInts.get(1), spawnLocInts.get(2), spawnLocInts.get(3).floatValue(), spawnLocInts.get(4).floatValue());
            }
        } else {
            //fall back to spawn worlds spawn location
            if(this.spawnWorld.isEmpty()) {
                //no spawn world use IslandConfig spawn location
                if(IslandsConfig.spawnIsland != null) {
                    this.spawnLocation = IslandsConfig.spawnIsland.getIslandSpawn();
                } else {
                    this.spawnLocation = Islands.islandsWorld.getSpawnLocation();        
                }
            } else {
                World w = Bukkit.getWorld(this.spawnWorld);
                if(w != null) {
                    this.spawnLocation = w.getSpawnLocation();
                } else {
                    Islands.instance.getLogger().warning("Specified spawn world does not exist! :: " + spawnWorld);
                    this.spawnLocation = Islands.islandsWorld.getSpawnLocation();  
                }
            }
        }
        //velocity
        this.useVelocity = plugin.getConfig().getBoolean("voidTeleportVelocity",false);
        this.velocityServer = plugin.getConfig().getString("voidTeleportVelocityServer","");
        if(this.useVelocity) {
            plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, "BungeeCord");
        }

        //register event listener
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!event.getPlayer().hasPlayedBefore() && IslandsConfig.spawnIsland != null) {
            IslandsConfig.spawnIsland.teleport(event.getPlayer());
        }
        World landingWorld = event.getPlayer().getLocation().getWorld();
        if(this.onLoginTPToSpawn && (landingWorld.equals(Islands.islandsWorld) || landingWorld.equals(Islands.wildernessWorld))) {
            if(this.spawnWorld.isEmpty() && this.spawnLocation == null) {
                IslandsConfig.spawnIsland.teleport(event.getPlayer());
            } else if (this.spawnLocation != null) {
                event.getPlayer().teleport(this.spawnLocation);
            } else {
                event.getPlayer().teleport(Bukkit.getWorld(this.spawnWorld).getSpawnLocation());
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerLeave(PlayerQuitEvent event) {
        //make sure the player is removed from the list
        this.teleportAttempts.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (event.isBedSpawn() && !overrideBedSpawns) return;
        if (IslandsConfig.spawnIsland != null) {
            event.setRespawnLocation(IslandsConfig.spawnIsland.getIslandSpawn());
            if (islandDamage) plugin.playersWithNoFall.add(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPortalEvent(PlayerPortalEvent event) {
        if (event.getTo() != null && Islands.islandsWorld.equals(event.getTo().getWorld()) && Islands.wildernessWorld != null) {
            Location to = event.getTo();
            to.setWorld(Islands.wildernessWorld);
            event.setTo(to);
        }
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if ((event.getSpawnReason().equals(CreatureSpawnEvent.SpawnReason.NATURAL) || event.getSpawnReason().equals(CreatureSpawnEvent.SpawnReason.RAID))
            && event.getEntity().getWorld().equals(Islands.islandsWorld) && disableMobs) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockFromTo(BlockFromToEvent event) {
        if (!event.getBlock().getWorld().equals(Islands.islandsWorld) || !restrictFlow) return;
        boolean canFlow = IslandsConfig.isBlockInWaterFlowArea(
                event.getToBlock().getX(), event.getToBlock().getY(), event.getToBlock().getZ());

        if(!canFlow) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST) // Player teleportation in void, damage restrictions
    public void onDamageEvent(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player) {
            Player player = (Player) e.getEntity();
            if (e.getCause().equals(EntityDamageEvent.DamageCause.VOID) && player.getWorld().equals(Islands.islandsWorld) && voidTeleport) {
                World targetWorld;

                synchronized(player) {
                    //see if the player has all ready transfered
                    if(!player.isOnline() && player.getWorld().equals(Islands.islandsWorld)) {
                        e.setCancelled(true);
                        return;
                    }
                    //check if we should use velocity
                    if(this.useVelocity && !this.velocityServer.isEmpty()) {
                        //failsafe - If the server we are transporting too is down or too slow teleport the player to spawn
                        this.teleportAttempts.putIfAbsent(player.getUniqueId(), 0);
                        Integer attempts = this.teleportAttempts.get(player.getUniqueId());
                        if(attempts.intValue() > 30) {
                            player.sendMessage("Failed to transfer to the Wilderness server!");
                            player.teleport(this.spawnLocation);
                            this.teleportAttempts.remove(player.getUniqueId());
                            e.setCancelled(true);
                            return;
                        }
                        this.teleportAttempts.put(player.getUniqueId(), attempts+1);
                        //send message to veloicty server as player start transfer
                        ByteArrayDataOutput out = ByteStreams.newDataOutput();
                        out.writeUTF("Connect");
                        out.writeUTF(this.velocityServer);
                        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
                        e.setCancelled(true);
                        return;
                    }

                    if (Islands.wildernessWorld == null) {
                        targetWorld = plugin.getServer().getWorlds().get(0);
                    } else {
                        targetWorld = Islands.wildernessWorld;
                    }

                    Location location;
                    if (preserveWildernessPositions && Islands.instance.wildernessPositions.containsKey(player)) {
                        location = Islands.instance.wildernessPositions.get(player);
                    } else {
                        location = player.getLocation();

                        double teleportMultiplier = plugin.getConfig().getInt("wildernessCoordinateMultiplier") <= 0
                                ? 4.0 : plugin.getConfig().getInt("wildernessCoordinateMultiplier");

                        location.setX(location.getBlockX() * teleportMultiplier);
                        location.setZ(location.getBlockZ() * teleportMultiplier);
                    }

                    location.setWorld(targetWorld);
                    location.setY(targetWorld.getHighestBlockYAt(location) + 40.0);

                    player.teleport(location);

                    player.sendTitle(Messages.get("success.WILDERNESS_TELEPORT_TITLE"),
                            Messages.get("success.WILDERNESS_TELEPORT_SUBTITLE"), (int)(20 * 0.5), 20 * 5, (int)(20 * 0.5));

                    plugin.playersWithNoFall.add(player);
                }
                e.setCancelled(true);
            } else if (e.getCause().equals(EntityDamageEvent.DamageCause.FALL) && plugin.playersWithNoFall.contains(player)) {
                plugin.playersWithNoFall.remove(player);
                e.setCancelled(true);
            } else if (player.getWorld().equals(Islands.islandsWorld) && !islandDamage) {
                e.setCancelled(true);
            } else {
                plugin.teleportCooldowns.put(player.getUniqueId().toString(), new Date().getTime());
            }
        }
    }

    @EventHandler
    private void checkIfPlayerLandsInWater(PlayerMoveEvent event) {
        Location l;
        if (event.getTo() == null) return;
        else l = event.getTo();

        if (plugin.playersWithNoFall.contains(event.getPlayer()) && 
            l.getWorld() == Islands.wildernessWorld || (islandDamage && 
            l.getWorld() == Islands.islandsWorld) &&
            l.getBlock().isLiquid()) {
                plugin.playersWithNoFall.remove(event.getPlayer());
        }
    }

    // Sync clocks
    @EventHandler
    private void onTimeSkip(TimeSkipEvent event) {
        if (!syncTime) return;
        if (!event.getSkipReason().equals(TimeSkipEvent.SkipReason.NIGHT_SKIP)) return;

        long targetTime = event.getWorld().getTime() + event.getSkipAmount();

        if (event.getWorld().equals(Islands.islandsWorld)) {
            Islands.wildernessWorld.setTime(targetTime);
        } else if (event.getWorld().equals(Islands.wildernessWorld)) {
            Islands.islandsWorld.setTime(targetTime);
        }
    }

    @EventHandler
    private void onWorldChange(PlayerTeleportEvent event) {
        if (!preserveWildernessPositions) return;
        if (Islands.wildernessWorld == event.getFrom().getWorld() && event.getTo() != null && Islands.wildernessWorld != event.getTo().getWorld()) {
            Islands.instance.wildernessPositions.put(event.getPlayer(), event.getFrom());
        }
    }
}
