package com.flobi.floauction;

import me.virustotal.floauction.listeners.MobArenaListener; 
import me.virustotal.floauction.listeners.PvpArenaListener;
import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.api.PVPArenaAPI;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;

import com.garbagemule.MobArena.MobArena;
import com.tommytony.war.War;
import com.tommytony.war.Warzone;


/**
 * Utility class for managing MobArena, PVPArena and War arenas.
 * 
 * @author Joshua "flobi" Hatfield
 */
public class ArenaManager {
	
	private static MobArena mobArena = null;
	private static PVPArena pvpArena = null;
	private static War war = null;
	private static boolean mobArenaListenerEnabled = false;
	private static boolean pvpArenaListenerEnabled = false;
	
	/**
	 * Loads listeners for the Arena plugins.
	 * 
	 * @param plugin the FloAuction instance to pass into the listener
	 */
	public static void loadArenaListeners(FloAuction plugin) 
	{
		PluginManager pluginManager = Bukkit.getPluginManager();
		// Load plugins
		if (ArenaManager.mobArena == null)
		{
			mobArena = (MobArena) pluginManager.getPlugin("MobArena");
		}
		
		if (ArenaManager.mobArena != null) 
		{
			if(!ArenaManager.mobArenaListenerEnabled)
			{
				pluginManager.registerEvents(new MobArenaListener(), plugin);
				ArenaManager.mobArenaListenerEnabled = true;
			}
		}
		
		if (ArenaManager.pvpArena == null)
		{
			ArenaManager.pvpArena = (PVPArena) pluginManager.getPlugin("pvparena");
		}
		
		if (ArenaManager.pvpArena != null)
		{
			if(!ArenaManager.pvpArenaListenerEnabled)
			{
				pluginManager.registerEvents(new PvpArenaListener(), plugin);
				ArenaManager.pvpArenaListenerEnabled = true;
			}
		}
	}

	/**
	 * Attempts to load arena plugins.
	 */
	public static void loadArenaPlugins() 
	{
		PluginManager pluginManager = Bukkit.getPluginManager();
		// Load plugins
		if (mobArena == null) 
		{
			mobArena = (MobArena) pluginManager.getPlugin("MobArena");
		}
		if (pvpArena == null)
		{
			pvpArena = (PVPArena) pluginManager.getPlugin("pvparena");
		}
		if (war == null)
		{
			war = (War) pluginManager.getPlugin("MobDungeon");
		}

		// Unload if not enabled
		if (mobArena != null && !mobArena.isEnabled())
		{
			mobArena = null;
		}
		if (pvpArena != null && !pvpArena.isEnabled())
		{
			pvpArena = null;
		}
		if (war != null && !war.isEnabled())
		{
			war = null;
		}
		
	}
	
	/**
	 * Unloads arena plugins.
	 */
	public static void unloadArenaPlugins() 
	{
		mobArena = null;
		pvpArena = null;
		war = null;
	}
	
	/**
	 * Checks to see if the player is in any arena.
	 * 
	 * @param  player  the player for whom to check 
	 * @return         whether or not the player is in an arena 
	 */
	public static boolean isInArena(Player player) 
	{
		if (player == null)
		{
			return false;
		}
		else if (AuctionConfig.getBoolean("allow-arenas", AuctionScope.getPlayerScope(player)))
		{
			return false;
		}
		loadArenaPlugins();
		
		if (mobArena != null && mobArena.getArenaMaster() != null && mobArena.getArenaMaster().getArenaWithPlayer(player) != null) 
		{
			return true;
		}
		else if (pvpArena != null && !PVPArenaAPI.getArenaName(player).equals("")) 
		{
			return true;
		}
		else if (war != null && Warzone.getZoneByLocation(player) != null) 
		{
			return true;
		}

		return false;
	}
	
	/**
	 * Checks to see if the location is in any arena.
	 * 
	 * @param  location  the location for which to check 
	 * @return           whether or not the location is in an arena 
	 */
	public static boolean isInArena(Location location) 
	{
		if (location == null) 
		{
			return false;
		}
		else if (AuctionConfig.getBoolean("allow-arenas", AuctionScope.getLocationScope(location))) 
		{
			return false;
		}
		loadArenaPlugins();
		
		if (mobArena != null && mobArena.getArenaMaster() != null && mobArena.getArenaMaster().getArenaAtLocation(location) != null)
		{
			return true;
		}
		else if (pvpArena != null && !PVPArenaAPI.getArenaNameByLocation(location).equals(""))
		{
			return true;
		}
		else if (war != null && Warzone.getZoneByLocation(location) != null) 
		{
			return true;
		}

		return false;
	}	
}