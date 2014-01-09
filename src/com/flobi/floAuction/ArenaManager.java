package com.flobi.floAuction;

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

public class ArenaManager implements Listener {
	private static MobArena mobArena = null;
	private static PVPArena pVPArena = null;
	private static War war = null;
	
	// Call when loading plugin.
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
	
	// Call when unloading plugin.
	public static void unloadArenaPlugins() {
		mobArena = null;
		pVPArena = null;
		war = null;
	}
	
	public static boolean isInArena(Player player) {
		if (player == null) return false;
		if (AuctionConfig.getBoolean("allow-arenas", AuctionScope.getPlayerScope(player))) return false;
		loadArenaPlugins();
		
		if (mobArena != null && mobArena.getArenaMaster() != null && mobArena.getArenaMaster().getArenaWithPlayer(player) != null) return true;
		if (pVPArena != null && !PVPArenaAPI.getArenaName(player).equals("")) return true;
		if (war != null && Warzone.getZoneByLocation(player) != null) return true;

		return false;
	}
	
	public static boolean isInArena(Location location) {
		if (location == null) return false;
		if (AuctionConfig.getBoolean("allow-arenas", AuctionScope.getLocationScope(location))) return false;
		loadArenaPlugins();
		
		if (mobArena != null && mobArena.getArenaMaster() != null && mobArena.getArenaMaster().getArenaAtLocation(location) != null) return true;
		if (pVPArena != null && !PVPArenaAPI.getArenaNameByLocation(location).equals("")) return true;
		if (war != null && Warzone.getZoneByLocation(location) != null) return true;

		return false;
	}
	
	@EventHandler
	public void onMAPlayerJoin(ArenaPlayerJoinEvent event) {
		if (event.isCancelled()) return;
		Player player = event.getPlayer();
		if (player == null) return;
		if (!AuctionConfig.getBoolean("allow-arenas", AuctionScope.getPlayerScope(player)) && AuctionParticipant.isParticipating(player.getName())) {
			floAuction.sendMessage("arena-warning", player.getName(), null);
			event.setCancelled(true);
		}
	}
	
	@EventHandler
	public void onPAPlayerJoin(PAJoinEvent event) {
		if (event.isCancelled()) return;
		Player player = event.getPlayer();
		if (player == null) return;
		if (!AuctionConfig.getBoolean("allow-arenas", AuctionScope.getPlayerScope(player)) && AuctionParticipant.isParticipating(player.getName())) {
			floAuction.sendMessage("arena-warning", player.getName(), null);
			event.setCancelled(true);
		}
	}
	
}
