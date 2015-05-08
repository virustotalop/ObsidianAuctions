package com.flobi.floAuction;

import com.flobi.floAuction.utility.CArrayList;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.api.PVPArenaAPI;
import net.slipcor.pvparena.events.PAJoinEvent;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;

import com.garbagemule.MobArena.MobArena;
import com.garbagemule.MobArena.events.ArenaPlayerJoinEvent;
import com.tommytony.war.War;
import com.tommytony.war.Warzone;


/**
 * Utility class for managing MobArena, PVPArena and War arenas.
 * 
 * @author Joshua "flobi" Hatfield
 */
public class ArenaManager {
	private static MobArena mobArena = null;
	private static PVPArena pVPArena = null;
	private static War war = null;
	
	/**
	 * Loads listeners for the Arena plugins.
	 * 
	 * @param plugin the floAuction instance to pass into the listener
	 */
	public static void loadArenaListeners(floAuction plugin) {
		PluginManager pluginManager = Bukkit.getPluginManager();
		// Load plugins
		if (mobArena == null) mobArena = (MobArena) pluginManager.getPlugin("MobArena");
		if (mobArena != null) pluginManager.registerEvents(new Listener() {
			/**
			 * Listener for MobArena.  Cancels arena entry if player is participating in an auction.
			 * 
			 * @param  event  the location for which to check 
			 */
			@EventHandler
			public void onMAPlayerJoin(ArenaPlayerJoinEvent event) {
				if (event.isCancelled()) return;
				Player player = event.getPlayer();
				if (player == null) return;
				String playerName = player.getName();
				if (!AuctionConfig.getBoolean("allow-arenas", AuctionScope.getPlayerScope(player)) && AuctionParticipant.isParticipating(playerName)) {
					floAuction.getMessageManager().sendPlayerMessage(new CArrayList<String>(new String[] {"arena-warning"}), playerName, (AuctionScope) null);
					event.setCancelled(true);
				}
			}
		}, plugin);
		
		if (pVPArena == null) pVPArena = (PVPArena) pluginManager.getPlugin("pvparena");
		if (pVPArena != null) pluginManager.registerEvents(new Listener() {
			/**
			 * Listener for PVPArena.  Cancels arena entry if player is participating in an auction.
			 * 
			 * @param  event  the location for which to check 
			 */
			@EventHandler
			public void onPAPlayerJoin(PAJoinEvent event) {
				if (event.isCancelled()) return;
				Player player = event.getPlayer();
				if (player == null) return;
				String playerName = player.getName();
				if (!AuctionConfig.getBoolean("allow-arenas", AuctionScope.getPlayerScope(player)) && AuctionParticipant.isParticipating(playerName)) {
					floAuction.getMessageManager().sendPlayerMessage(new CArrayList<String>(new String[] {"arena-warning"}), playerName, (AuctionScope) null);
					event.setCancelled(true);
				}
			}
		}, plugin);
	}
	
	/**
	 * Attempts to load arena plugins.
	 */
	public static void loadArenaPlugins() {
		PluginManager pluginManager = Bukkit.getPluginManager();
		// Load plugins
		if (mobArena == null) mobArena = (MobArena) pluginManager.getPlugin("MobArena");
		if (pVPArena == null) pVPArena = (PVPArena) pluginManager.getPlugin("pvparena");
		if (war == null) war = (War) pluginManager.getPlugin("MobDungeon");

		// Unload if not enabled
		if (mobArena != null && !mobArena.isEnabled()) mobArena = null;
		if (pVPArena != null && !pVPArena.isEnabled()) pVPArena = null;
		if (war != null && !war.isEnabled()) war = null;
		
	}
	
	/**
	 * Unloads arena plugins.
	 */
	public static void unloadArenaPlugins() {
		mobArena = null;
		pVPArena = null;
		war = null;
	}
	
	/**
	 * Checks to see if the player is in any arena.
	 * 
	 * @param  player  the player for whom to check 
	 * @return         whether or not the player is in an arena 
	 */
	public static boolean isInArena(Player player) {
		if (player == null) return false;
		if (AuctionConfig.getBoolean("allow-arenas", AuctionScope.getPlayerScope(player))) return false;
		loadArenaPlugins();
		
		if (mobArena != null && mobArena.getArenaMaster() != null && mobArena.getArenaMaster().getArenaWithPlayer(player) != null) return true;
		if (pVPArena != null && !PVPArenaAPI.getArenaName(player).equals("")) return true;
		if (war != null && Warzone.getZoneByLocation(player) != null) return true;

		return false;
	}
	
	/**
	 * Checks to see if the location is in any arena.
	 * 
	 * @param  location  the location for which to check 
	 * @return           whether or not the location is in an arena 
	 */
	public static boolean isInArena(Location location) {
		if (location == null) return false;
		if (AuctionConfig.getBoolean("allow-arenas", AuctionScope.getLocationScope(location))) return false;
		loadArenaPlugins();
		
		if (mobArena != null && mobArena.getArenaMaster() != null && mobArena.getArenaMaster().getArenaAtLocation(location) != null) return true;
		if (pVPArena != null && !PVPArenaAPI.getArenaNameByLocation(location).equals("")) return true;
		if (war != null && Warzone.getZoneByLocation(location) != null) return true;

		return false;
	}	
}
