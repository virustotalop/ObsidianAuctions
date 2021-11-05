package com.gmail.virustotalop.obsidianauctions.auction;

import com.clubobsidian.wrappy.Configuration;
import com.clubobsidian.wrappy.ConfigurationSection;
import com.gmail.virustotalop.obsidianauctions.AuctionConfig;
import com.gmail.virustotalop.obsidianauctions.ObsidianAuctions;
import com.gmail.virustotalop.obsidianauctions.inject.annotation.Config;
import com.gmail.virustotalop.obsidianauctions.region.CuboidRegion;
import com.gmail.virustotalop.obsidianauctions.region.GlobalRegion;
import com.gmail.virustotalop.obsidianauctions.region.Point;
import com.gmail.virustotalop.obsidianauctions.region.Region;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.ApiStatus;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;


/**
 * Utility class for managing MobArena, PVPArena and War arenas.
 *
 * @author Joshua "flobi" Hatfield
 * @author virustotalop
 */
public class AuctionLocationManager {

    private final Map<String, Collection<Region>> regions = new HashMap<>();
    private final AuctionManager auctionManager;

    @Inject
    private AuctionLocationManager(@Config Configuration config, ObsidianAuctions plugin, AuctionManager auctionManager) {
        this.auctionManager = auctionManager;
        this.loadRegions(config, plugin);
    }

    private void loadRegions(Configuration config, ObsidianAuctions plugin) {
        //Run one tick after load, this is to ensure that all worlds are actually loaded
        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            //Don't run if the plugin isn't even enabled
            if(Bukkit.getServer().getPluginManager().getPlugin(plugin.getName()) == null  || !plugin.isEnabled()) {
                return;
            }
            ConfigurationSection arenas = config.getConfigurationSection("arenas");
            for(String worldName : arenas.getKeys()) {
                World bukkitWorld = plugin.getServer().getWorld(worldName);
                if(bukkitWorld != null) {
                    Collection<Region> regionList = new ArrayList<>();
                    this.regions.put(worldName, regionList);
                    ConfigurationSection world = arenas.getConfigurationSection(worldName);
                    for(String regionName : world.getKeys()) {
                        ConfigurationSection region = world.getConfigurationSection(regionName);
                        String regionType = region.getString("type").toLowerCase();
                        if(regionType.equals("global")) {
                            regionList.add(new GlobalRegion(regionName, bukkitWorld));
                        } else if(regionType.equals("cuboid")) {
                            Point min = Point.create(region.getString("min"));
                            Point max = Point.create(region.getString("max"));
                            regionList.add(new CuboidRegion(regionName, bukkitWorld, min, max));
                        }
                    }
                } else {
                    plugin.getLogger().log(Level.INFO, "Skipping " + worldName + " from loading arenas");
                }
            }
        });
    }

    /**
     * Check to see if the participant is currently located within the AuctionScope in which he's participating. Nonparticipating players always return true.
     *
     * @param playerUUID player name to check
     * @return whether he is in the appropriate scope
     */
    @ApiStatus.Internal
    public boolean checkLocation(UUID playerUUID) {
        AuctionParticipant participant = this.auctionManager.getParticipant(playerUUID);
        if(participant == null) {
            return true;
        }
        Player player = Bukkit.getPlayer(playerUUID);
        return participant.getAuctionScope().equals(this.auctionManager.getPlayerScope(player));
    }

    /**
     * Check to see if the participant would be located within the AuctionScope in which he's participating if he were located elsewhere. Nonparticipating players always return true.
     *
     * @param playerUUID player name to check
     * @param location   location to check
     * @return whether he would be in the appropriate scope
     */
    @ApiStatus.Internal
    public boolean checkLocation(UUID playerUUID, Location location) {
        AuctionParticipant participant = this.auctionManager.getParticipant(playerUUID);
        if(participant == null) {
            return true;
        }
        return participant.getAuctionScope().equals(this.auctionManager.getLocationScope(location));
    }

    /**
     * Force a player back into the AuctionScope in which he's participating at the last known location they were spotted
     * inside the scope. Sends a one time message when moving the player. If a locationForGaze is included, it will make
     * the player look the direction that location is looking. Does nothing to nonparticipating players or players
     * already in their scope.
     *
     * @param playerUUID      player to force
     * @param locationForGaze location for gaze
     */
    @ApiStatus.Internal
    public void forceLocation(UUID playerUUID, Location locationForGaze) {
        AuctionParticipant participant = this.auctionManager.getParticipant(playerUUID);
        if(participant == null) {
            return;
        } else if(!participant.isParticipating()) {
            return;
        }

        Player player = Bukkit.getPlayer(playerUUID);
        Location location = player.getLocation();
        if(locationForGaze != null) {
            location.setDirection(new Vector(0, 0, 0));
            location.setPitch(locationForGaze.getPitch());
            location.setYaw(locationForGaze.getYaw());
        } else if(!this.checkLocation(playerUUID)) {
            player.teleport(participant.getLastKnownGoodLocation());
            participant.sendEscapeWarning();
            return;
        } else if(this.isInArena(player)) { //Can't get rid of this due to circular dependencies
            player.teleport(participant.getLastKnownGoodLocation());
            participant.sendArenaWarning();
            return;
        }
        participant.setLastKnownGoodLocation(location);
    }

    /**
     * Checks whether to teleport a participant based on whether the destination would be outside the participants AuctionScope.  Sends a one time notification if it's not okay to teleport.
     *
     * @param playerUUID name of player to check
     * @param location   teleport destination to check
     * @return true if it IS okay to teleport this player
     */
    @ApiStatus.Internal
    public boolean checkTeleportLocation(UUID playerUUID, Location location) {
        AuctionParticipant participant = this.auctionManager.getParticipant(playerUUID);
        if(participant == null) {
            return true;
        } else if(!participant.isParticipating()) {
            return true;
        } else if(!this.checkLocation(playerUUID, location)) {
            participant.sendEscapeWarning();
            return false;
        } else if(this.isInArena(location)) {
            participant.sendArenaWarning();
            return false;
        }
        return true;
    }

    /**
     * Checks to see if the player is in any arena.
     *
     * @param player the player for whom to check
     * @return whether or not the player is in an arena
     */
    public boolean isInArena(Player player) {
        if(player == null) {
            return false;
        }
        return this.isInArena(player.getLocation());
    }

    /**
     * Checks to see if the location is in any arena.
     *
     * @param location the location for which to check
     * @return whether or not the location is in an arena
     */
    public boolean isInArena(Location location) {
        if(location == null) {
            return false;
        } else if(AuctionConfig.getBoolean("allow-arenas", this.auctionManager.getLocationScope(location))) {
            return false;
        }
        Collection<Region> regions = this.regions.get(location.getWorld().getName());
        if(regions != null) {
            for(Region region : regions) {
                if(region.isWithin(location)) {
                    return true;
                }
            }
        }
        return false;
    }
}