package com.flobi.floAuction;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.google.common.collect.Lists;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

/**
 * Class to manage different auction areas or scopes.  
 * 
 * @author Joshua "flobi" Hatfield
 */
public class AuctionScope {
	private Auction activeAuction = null;
	private List<Auction> otherPluginsAuctions = null;
	private String scopeId = null;
	private String name = null;
	private String type = null;
	private ArrayList<Auction> auctionQueue = new ArrayList<Auction>();
	private long lastAuctionDestroyTime = 0;
	
	// Definitions
	private List<String> worlds = null;
	private Location minHouseLocation = null;
	private Location maxHouseLocation = null;
	private String regionId = null;
	private boolean locationChecked = false;
	
	private ConfigurationSection config = null;
	private ConfigurationSection textConfig = null;

	public static List<String> auctionScopesOrder = new ArrayList<String>();
	public static Map<String, AuctionScope> auctionScopes = new HashMap<String, AuctionScope>();
	private static WorldGuardPlugin worldGuardPlugin = null;

	/**
	 * Constructor to make new scopes from the name, config and language config files.
	 * 
	 * @param name name of scope
	 * @param config configuration
	 * @param textConfig language config
	 */
	private AuctionScope(String scopeId, ConfigurationSection config, ConfigurationSection textConfig) {
		this.scopeId = scopeId;
		name = config.getString("name");
		if (name == null) name = scopeId;

		type = config.getString("type");
		this.config = config;
		this.textConfig = textConfig;
	}
	
	/**
	 * Checks whether the scopes definition is valid.  
	 * 
	 * @return true if valid location, false if invalid
	 */
	private boolean scopeLocationIsValid() {
		if (locationChecked) return worlds != null || minHouseLocation != null || maxHouseLocation != null || regionId != null;
		if (type.equalsIgnoreCase("worlds")) {
			worlds = config.getStringList("worlds");
		} else if (type.equalsIgnoreCase("house")) {
			String world = config.getString("house-world");
			if (world == null || world.isEmpty()) {
				minHouseLocation = null;
				maxHouseLocation = null;
			} else {
				minHouseLocation = new Location(Bukkit.getWorld(world), config.getDouble("house-min-x"), config.getDouble("house-min-y"), config.getDouble("house-min-z"));
				maxHouseLocation = new Location(Bukkit.getWorld(world), config.getDouble("house-max-x"), config.getDouble("house-max-y"), config.getDouble("house-max-z"));
			}
		} else if (type.equalsIgnoreCase("worldguardregion")) {
			if (worldGuardPlugin == null) {
				// get the list of regions that contain the given location
			    Plugin plugin = Bukkit.getPluginManager().getPlugin("WorldGuard");
			    
			    // WorldGuard may not be loaded
			    if (plugin != null && plugin instanceof WorldGuardPlugin) {
				    worldGuardPlugin = (WorldGuardPlugin) plugin;
			    }
			}
			regionId = config.getString("region-id");
		}
		locationChecked = true;
		return worlds != null || minHouseLocation != null || maxHouseLocation != null || regionId != null;
	}
	
	/**
	 * Retrieves and instance of the active auction from this scope.
	 * 
	 * @return active auction instance
	 */
	public Auction getActiveAuction() {
		return activeAuction;
	}
	
	/**
	 * Gets the number of queued auctions.
	 * 
	 * @return length of auction queue
	 */
	public int getAuctionQueueLength() {
		return auctionQueue.size();
	}
	
	/**
	 * Sets the passed in auction instance as the active auction.  
	 * 
	 * @param auction auction instance to set as active
	 */
	public void setActiveAuction(Auction auction) {
		if (activeAuction != null && auction == null) {
			lastAuctionDestroyTime = System.currentTimeMillis();
			checkAuctionQueue();
		}
		activeAuction = auction;
	}
	
	/**
	 * Adds an auction instance to the auction queue for this scope.
	 * 
	 * @param auctionToQueue auction instance to queue
	 * @param player player initiating queue request
	 * @param currentAuction the auction that's currently running
	 */
    public void queueAuction(Auction auctionToQueue) {
		String playerName = auctionToQueue.getOwner();
		MessageManager messageManager = auctionToQueue.messageManager;

		if (activeAuction == null) {
			// Queuing because of interval not yet timed out.
			// Allow a queue of 1 to override if 0 for this condition.
	    	if (Math.max(AuctionConfig.getInt("max-auction-queue-length", this), 1) <= auctionQueue.size()) {
	    		messageManager.sendPlayerMessage(Lists.newArrayList("auction-queue-fail-full"), playerName, auctionToQueue);
				return;
			}
		} else {
	    	if (AuctionConfig.getInt("max-auction-queue-length", this) <= 0) {
	    		messageManager.sendPlayerMessage(Lists.newArrayList("auction-fail-auction-exists"), playerName, auctionToQueue);
				return;
			}
			if (activeAuction.getOwner().equalsIgnoreCase(playerName)) {
	    		messageManager.sendPlayerMessage(Lists.newArrayList("auction-queue-fail-current-auction"), playerName, auctionToQueue);
				return;
			}
			if (AuctionConfig.getInt("max-auction-queue-length", this) <= auctionQueue.size()) {
	    		messageManager.sendPlayerMessage(Lists.newArrayList("auction-queue-fail-full"), playerName, auctionToQueue);
				return;
			}
		}
		for(int i = 0; i < auctionQueue.size(); i++) {
			if (auctionQueue.get(i) != null) {
				Auction queuedAuction = auctionQueue.get(i);
				if (queuedAuction.getOwner().equalsIgnoreCase(playerName)) {
		    		messageManager.sendPlayerMessage(Lists.newArrayList("auction-queue-fail-in-queue"), playerName, auctionToQueue);
					return;
				}
			}
		}
		if ((auctionQueue.size() == 0 && System.currentTimeMillis() - lastAuctionDestroyTime >= AuctionConfig.getInt("min-auction-interval-secs", this) * 1000) || auctionToQueue.isValid()) {
			auctionQueue.add(auctionToQueue);
			AuctionParticipant.addParticipant(playerName, this);
			checkAuctionQueue();
			if (auctionQueue.contains(auctionToQueue)) {
	    		messageManager.sendPlayerMessage(Lists.newArrayList("auction-queue-enter"), playerName, auctionToQueue);
			}
		}
    }

    /**
     * Checks the auction queue to see if the next auction is ready to start. 
     */
	private void checkThisAuctionQueue() {
		if (activeAuction != null) {
			return;
		}
		if (System.currentTimeMillis() - lastAuctionDestroyTime < AuctionConfig.getInt("min-auction-interval-secs", this) * 1000) {
			return;
		}
		if (auctionQueue.size() == 0) {
			return;
		}
		Auction auction = auctionQueue.remove(0);
		if (auction == null) {
			return;
		}
		MessageManager messageManager = auction.messageManager;
		
		String playerName = auction.getOwner();
		Player player = Bukkit.getPlayer(playerName);
		if (player == null || !player.isOnline()) {
			return;
		}
		
		if (AuctionProhibition.isOnProhibition(auction.getOwner(), false)) {
    		messageManager.sendPlayerMessage(Lists.newArrayList("remote-plugin-prohibition-reminder"), playerName, auction);
			return;
		}
		
		if (!AuctionConfig.getBoolean("allow-gamemode-creative", this) && player.getGameMode() == GameMode.CREATIVE) {
    		messageManager.sendPlayerMessage(Lists.newArrayList("auction-fail-gamemode-creative"), playerName, auction);
			return;
		}
		
		if (!floAuction.perms.has(player, "auction.start")) {
    		messageManager.sendPlayerMessage(Lists.newArrayList("auction-fail-permissions"), playerName, auction);
			return;
		}
		if (!auction.isValid()) {
			return;
		}
		activeAuction = auction;
		if (!auction.start()) {
			activeAuction = null;
		}
	}
	
	/**
	 * Checks to see if the player is inside the scope boundaries.
	 * 
	 * @param player player to check
	 * @return whether he's in the scope
	 */
	private boolean isPlayerInScope(Player player) {
		if (player == null) return false;
		return isLocationInScope(player.getLocation());
	}
	
	/**
	 * Checks to see if a location is inside the scope boundaries.
	 * 
	 * @param location location to check
	 * @return whether it's in the scope
	 */
	private boolean isLocationInScope(Location location) {
		if (location == null) return false;
		World world = location.getWorld();
		if (world == null) return false;
		String worldName = world.getName();
		if (!scopeLocationIsValid()) return false;
		if (type.equalsIgnoreCase("worlds")) {
			for (int i = 0; i < worlds.size(); i++) {
				if (worlds.get(i).equalsIgnoreCase(worldName) || worlds.get(i).equalsIgnoreCase("*")) return true;
			}
		} else if (type.equalsIgnoreCase("house")) {
			if (minHouseLocation == null || maxHouseLocation == null) return false;
			if (!location.getWorld().equals(minHouseLocation.getWorld())) return false;
			if (location.getX() > Math.max(minHouseLocation.getX(), maxHouseLocation.getX()) || location.getX() < Math.min(minHouseLocation.getX(), maxHouseLocation.getX())) return false;
			if (location.getZ() > Math.max(minHouseLocation.getZ(), maxHouseLocation.getZ()) || location.getZ() < Math.min(minHouseLocation.getZ(), maxHouseLocation.getZ())) return false;
			if (location.getY() > Math.max(minHouseLocation.getY(), maxHouseLocation.getY()) || location.getY() < Math.min(minHouseLocation.getY(), maxHouseLocation.getY())) return false;
			return true;
		} else if (type.equalsIgnoreCase("worldguardregion")) {
			if (worldGuardPlugin == null) return false;
			RegionManager regionManager =  worldGuardPlugin.getRegionManager( location.getWorld() );
			ApplicableRegionSet applicableRegions = regionManager.getApplicableRegions(location);
			for (ProtectedRegion region : applicableRegions) {
				if (region.getId().equalsIgnoreCase(regionId)) return true;
			}
		}
		return false;
	}

	/**
	 * Retrieves the configuration for this scope.
	 * 
	 * @return config for the scope
	 */
	public ConfigurationSection getConfig() {
		return config.getConfigurationSection("config");
	}

	/**
	 * Gets the name of the scope.
	 * 
	 * @return name of the scope
	 */
	public String getName() {
		return name;
	}


	/**
	 * Gets the id of the scope.
	 * 
	 * @return id of the scope
	 */
	public String getScopeId() {
		return scopeId;
	}

	/**
	 * Retrieves the language configuration for this scope.
	 * 
	 * @return language config for the scope
	 */
	public ConfigurationSection getTextConfig() {
		return textConfig;
	}

	/**
	 * Retrieves the list of auctions queued.
	 * 
	 * @return auction queue
	 */
	public ArrayList<Auction> getAuctionQueue() {
		return auctionQueue;
	}
	
	/**
	 * Checks the auction queues for each scope, starting any auctions which are ready.
	 */
	public static void checkAuctionQueue() {
		for (Map.Entry<String, AuctionScope> auctionScopesEntry : auctionScopes.entrySet()) {
			auctionScopesEntry.getValue().checkThisAuctionQueue();
		}
	}
	
	/**
	 * Gets the position of the named player's auction in the queue or zero if not in queue.
	 * 
	 * @param playerName name of player
	 * @return players position in queue or zero if not in queue
	 */
	public int getQueuePosition(String playerName) {
		for (int t = 0; t < auctionQueue.size(); t++) {
			Auction auction = auctionQueue.get(t);
			if (auction.getOwner().equalsIgnoreCase(playerName)) return t + 1;
		}
		return 0;
	}

	/**
	 * Gets the AuctionScope instance in which the player is.
	 * 
	 * @param player player to check
	 * @return scope where the player is
	 */
	public static AuctionScope getPlayerScope(Player player) {
		if (player == null) return null;
		for (int i = 0; i < auctionScopesOrder.size(); i++) {
			String auctionScopeId = auctionScopesOrder.get(i);
			AuctionScope auctionScope = auctionScopes.get(auctionScopeId);
			if (auctionScope.isPlayerInScope(player)) return auctionScope;
		}
		return null;
	}

	/**
	 * Gets the AuctionScope instance in which the location is.
	 * 
	 * @param player location to check
	 * @return scope where the location is
	 */
	public static AuctionScope getLocationScope(Location location) {
		if (location == null) return null;
		for (int i = 0; i < auctionScopesOrder.size(); i++) {
			String auctionScopeId = auctionScopesOrder.get(i);
			AuctionScope auctionScope = auctionScopes.get(auctionScopeId);
			if (auctionScope.isLocationInScope(location)) return auctionScope;
		}
		return null;
	}
	
	/**
	 * Builds list of AuctionScope instances based on the configuration loaded by the plugin.
	 * 
	 * @param auctionScopesConfig
	 * @param dataFolder
	 */
	public static void setupScopeList(ConfigurationSection auctionScopesConfig, File dataFolder) {
	    auctionScopes.clear();
	    auctionScopesOrder.clear();
		if (auctionScopesConfig != null) {
			for (String scopeName : auctionScopesConfig.getKeys(false)) {
				auctionScopesOrder.add(scopeName);
				ConfigurationSection auctionScopeConfig = auctionScopesConfig.getConfigurationSection(scopeName);
		    	File scopeTextConfigFile = new File(dataFolder, "language-"+scopeName+".yml");
		    	YamlConfiguration scopeTextConfig = null;
		    	if (scopeTextConfigFile.exists()) {
				    scopeTextConfig = YamlConfiguration.loadConfiguration(scopeTextConfigFile);
		    	}
				AuctionScope auctionScope = new AuctionScope(scopeName, auctionScopeConfig, scopeTextConfig);
				auctionScopes.put(scopeName, auctionScope);
			}
		} else {
			
		}
	}
	
	/**
	 * Big red button.
	 */
	public static void cancelAllAuctions() {
		for (Map.Entry<String, AuctionScope> auctionScopesEntry : auctionScopes.entrySet()) {
			AuctionScope auctionScope = auctionScopesEntry.getValue();
			auctionScope.auctionQueue.clear();
			if (auctionScope.activeAuction != null) {
				auctionScope.activeAuction.cancel();
			}
		}
	}

	/**
	 * Checks to see if any auctions are running.
	 * 
	 * @return
	 */
	public static boolean areAuctionsRunning() {
		for (Map.Entry<String, AuctionScope> auctionScopesEntry : auctionScopes.entrySet()) {
			AuctionScope auctionScope = auctionScopesEntry.getValue();
			if (auctionScope.getActiveAuction() != null || auctionScope.getAuctionQueueLength() > 0) {
				return true;
			}
		}
		return false;
	}

	public int getOtherPluginsAuctionsLength() {
		if (otherPluginsAuctions == null) return 0;
		return otherPluginsAuctions.size();
	}

	public static void sendFairwellMessages() {
		Iterator<String> playerIterator = floAuction.playerScopeCache.keySet().iterator();
		while (playerIterator.hasNext()) {
			String playerName = playerIterator.next();
			if (!AuctionParticipant.isParticipating(playerName)) {
				Player player = Bukkit.getPlayer(playerName);
				if (player != null && player.isOnline()) {
					String oldScopeId = floAuction.playerScopeCache.get(playerName);
					AuctionScope oldScope = AuctionScope.auctionScopes.get(oldScopeId);
					AuctionScope playerScope = AuctionScope.getPlayerScope(player);
					String playerScopeId = null;
					if (playerScope != null) {
						playerScopeId = playerScope.getScopeId();
					}
					if (playerScopeId == null || playerScopeId.isEmpty() || !playerScopeId.equalsIgnoreCase(oldScopeId)) {
							floAuction.getMessageManager().sendPlayerMessage(Lists.newArrayList("auctionscope-fairwell"), playerName, oldScope);
							playerIterator.remove();
							floAuction.playerScopeCache.remove(playerName);
					}
				}
			}
		}
	}

	public static void sendWelcomeMessages() {
		Player[] players = Bukkit.getOnlinePlayers();
		for (Player player : players) {
			sendWelcomeMessage(player, false);
		}
	}
	
	public static void sendWelcomeMessage(Player player, boolean isOnJoin) {
		String welcomeMessageKey = "auctionscope-welcome";
		if (isOnJoin) {
			welcomeMessageKey += "-onjoin";
		}
		String playerName = player.getName();
		if (!AuctionParticipant.isParticipating(playerName)) {
			AuctionScope playerScope = AuctionScope.getPlayerScope(player);
			String oldScopeId = floAuction.playerScopeCache.get(playerName);
			if (playerScope == null) {
				if (oldScopeId != null) {
					floAuction.playerScopeCache.remove(playerName);
				}
			} else {
				if (oldScopeId == null || oldScopeId.isEmpty() || !oldScopeId.equalsIgnoreCase(playerScope.getScopeId())) {
					floAuction.getMessageManager().sendPlayerMessage(Lists.newArrayList(welcomeMessageKey), playerName, playerScope);
					floAuction.playerScopeCache.put(playerName, playerScope.getScopeId());
				}
			}
		}
	}
}

