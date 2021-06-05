package com.gmail.virustotalop.obsidianauctions.area;

import com.gmail.virustotalop.obsidianauctions.AuctionConfig;
import com.gmail.virustotalop.obsidianauctions.ObsidianAuctions;
import com.gmail.virustotalop.obsidianauctions.auction.AuctionScope;
import com.gmail.virustotalop.obsidianauctions.listener.MobArenaListener;
import com.garbagemule.MobArena.MobArena;
import com.tommytony.war.War;
import com.tommytony.war.Warzone;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;


/**
 * Utility class for managing MobArena, PVPArena and War arenas.
 *
 * @author Joshua "flobi" Hatfield
 */
public class AreaManager {

    private static MobArena mobArena = null;
    private static War war = null;
    private static boolean mobArenaListenerEnabled = false;
    private static final boolean pvpArenaListenerEnabled = false;

    /**
     * Loads listeners for the Arena plugins.
     *
     * @param plugin the floAuction instance to pass into the listener
     */
    public static void loadArenaListeners(ObsidianAuctions plugin) {
        PluginManager pluginManager = Bukkit.getPluginManager();
        // Load plugins
        if(AreaManager.mobArena == null) {
            mobArena = (MobArena) pluginManager.getPlugin("MobArena");
        }

        if(AreaManager.mobArena != null) {
            if(!AreaManager.mobArenaListenerEnabled) {
                pluginManager.registerEvents(new MobArenaListener(), plugin);
                AreaManager.mobArenaListenerEnabled = true;
            }
        }
    }

    /**
     * Attempts to load arena plugins.
     */
    public static void loadArenaPlugins() {
        PluginManager pluginManager = Bukkit.getPluginManager();
        // Load plugins
        if(mobArena == null) {
            mobArena = (MobArena) pluginManager.getPlugin("MobArena");
        }
        if(war == null) {
            war = (War) pluginManager.getPlugin("MobDungeon");
        }

        // Unload if not enabled
        if(mobArena != null && !mobArena.isEnabled()) {
            mobArena = null;
        }
        if(war != null && !war.isEnabled()) {
            war = null;
        }

    }

    /**
     * Unloads arena plugins.
     */
    public static void unloadArenaPlugins() {
        mobArena = null;
        war = null;
    }

    /**
     * Checks to see if the player is in any arena.
     *
     * @param player the player for whom to check
     * @return whether or not the player is in an arena
     */
    public static boolean isInArena(Player player) {
        if(player == null) {
            return false;
        } else if(AuctionConfig.getBoolean("allow-arenas", AuctionScope.getPlayerScope(player))) {
            return false;
        }
        loadArenaPlugins();

        if(mobArena != null && mobArena.getArenaMaster() != null && mobArena.getArenaMaster().getArenaWithPlayer(player) != null) {
            return true;
        } else return war != null && Warzone.getZoneByLocation(player) != null;
    }

    /**
     * Checks to see if the location is in any arena.
     *
     * @param location the location for which to check
     * @return whether or not the location is in an arena
     */
    public static boolean isInArena(Location location) {
        if(location == null) {
            return false;
        } else if(AuctionConfig.getBoolean("allow-arenas", AuctionScope.getLocationScope(location))) {
            return false;
        }
        loadArenaPlugins();

        if(mobArena != null && mobArena.getArenaMaster() != null && mobArena.getArenaMaster().getArenaAtLocation(location) != null) {
            return true;
        } else return war != null && Warzone.getZoneByLocation(location) != null;
    }
}