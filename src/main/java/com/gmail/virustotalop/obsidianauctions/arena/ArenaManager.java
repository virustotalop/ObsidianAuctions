package com.gmail.virustotalop.obsidianauctions.arena;

import com.clubobsidian.wrappy.Configuration;
import com.clubobsidian.wrappy.ConfigurationSection;
import com.gmail.virustotalop.obsidianauctions.AuctionConfig;
import com.gmail.virustotalop.obsidianauctions.ObsidianAuctions;
import com.gmail.virustotalop.obsidianauctions.arena.region.CuboidRegion;
import com.gmail.virustotalop.obsidianauctions.arena.region.GlobalRegion;
import com.gmail.virustotalop.obsidianauctions.arena.region.Point;
import com.gmail.virustotalop.obsidianauctions.arena.region.Region;
import com.gmail.virustotalop.obsidianauctions.auction.AuctionScope;
import com.google.inject.Inject;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;


/**
 * Utility class for managing MobArena, PVPArena and War arenas.
 *
 * @author Joshua "flobi" Hatfield
 * @author virustotalop
 */
public class ArenaManager {

    private final Map<String, Collection<Region>> regions;


    @Inject
    private ArenaManager(Configuration config, ObsidianAuctions plugin) {
        this.regions = new HashMap<>();
        this.loadRegions(config, plugin);
    }

    /*

    arenas:
    some-world-name-that-does-not-exist:
        regions:
        global-region:
            type: global
        cuboid-region:
            type: cuboid
            min: 0,0,0
         max: 64,64,64
     */

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
        } else if(AuctionConfig.getBoolean("allow-arenas", AuctionScope.getLocationScope(location))) {
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