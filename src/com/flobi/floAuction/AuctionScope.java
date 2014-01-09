package com.flobi.floAuction;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class AuctionScope {
	private Auction activeAuction = null;
	private String name = null;
	private String type = null;
	private ArrayList<Auction> auctionQueue = new ArrayList<Auction>();
	private long lastAuctionDestroyTime = 0;
	private List<String> worlds = null;
	private Location minHouseLocation = null;
	private Location maxHouseLocation = null;
	private ConfigurationSection config = null;
	private FileConfiguration textConfig = null;

	public static List<String> auctionScopesOrder = new ArrayList<String>();
	public static Map<String, AuctionScope> auctionScopes = new HashMap<String, AuctionScope>();

	private AuctionScope(String name, ConfigurationSection config, YamlConfiguration textConfig) {
		this.name = name;
		this.textConfig = textConfig;

		type = config.getString("type");
		if (type.equalsIgnoreCase("worlds")) {
			worlds = config.getStringList("worlds");
		} else if (type.equalsIgnoreCase("house")) {
			String world = config.getString("house-world");
			if (world.isEmpty()) {
				minHouseLocation = null;
				maxHouseLocation = null;
			} else {
				minHouseLocation = new Location(Bukkit.getWorld(world), config.getDouble("house-min-x"), config.getDouble("house-min-y"), config.getDouble("house-min-z"));
				maxHouseLocation = new Location(Bukkit.getWorld(world), config.getDouble("house-max-x"), config.getDouble("house-max-y"), config.getDouble("house-max-z"));
			}
		}
		this.config = config.getConfigurationSection("config");
	}
	
	public Auction getActiveAuction() {
		return activeAuction;
	}
	
	public int getAuctionQueueLength() {
		return auctionQueue.size();
	}
	
	public void setActiveAuction(Auction auction) {
		if (activeAuction != null && auction == null) {
			lastAuctionDestroyTime = System.currentTimeMillis();
			checkAuctionQueue();
		}
		activeAuction = auction;
	}
	
    public void queueAuction(Auction auctionToQueue, Player player, Auction currentAuction) {
		String playerName = player.getName();

		if (currentAuction == null) {
			// Queuing because of interval not yet timed out.
			// Allow a queue of 1 to override if 0 for this condition.
	    	if (Math.max(AuctionConfig.getInt("max-auction-queue-length", this), 1) <= auctionQueue.size()) {
	    		floAuction.sendMessage("auction-queue-fail-full", player, this, false);
				return;
			}
		} else {
	    	if (AuctionConfig.getInt("max-auction-queue-length", this) <= 0) {
	    		floAuction.sendMessage("auction-fail-auction-exists", player, this, false);
				return;
			}
			if (currentAuction.getOwner().equalsIgnoreCase(playerName)) {
				floAuction.sendMessage("auction-queue-fail-current-auction", player, this, false);
				return;
			}
			if (AuctionConfig.getInt("max-auction-queue-length", this) <= auctionQueue.size()) {
				floAuction.sendMessage("auction-queue-fail-full", player, this, false);
				return;
			}
		}
		for(int i = 0; i < auctionQueue.size(); i++) {
			if (auctionQueue.get(i) != null) {
				Auction queuedAuction = auctionQueue.get(i);
				if (queuedAuction.getOwner().equalsIgnoreCase(playerName)) {
					floAuction.sendMessage("auction-queue-fail-in-queue", player, this, false);
					return;
				}
			}
		}
		if ((auctionQueue.size() == 0 && System.currentTimeMillis() - lastAuctionDestroyTime >= AuctionConfig.getInt("min-auction-interval-secs", this) * 1000) || auctionToQueue.isValid()) {
			auctionQueue.add(auctionToQueue);
			Participant.addParticipant(playerName, this);
			checkAuctionQueue();
			if (auctionQueue.contains(auctionToQueue)) {
				floAuction.sendMessage("auction-queue-enter", player, this, false);
			}
		}
    }

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
		
		Player player = Bukkit.getPlayer(auction.getOwner());
		if (player == null || !player.isOnline()) {
			return;
		}
		if (!AuctionConfig.getBoolean("allow-gamemode-creative", this) && player.getGameMode() == GameMode.CREATIVE) {
			floAuction.sendMessage("auction-fail-gamemode-creative", player, null, false);
			return;
		}
			
		if (!floAuction.perms.has(player, "auction.start")) {
			floAuction.sendMessage("no-permission", player, null, false);
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
	
	private boolean isPlayerInScope(Player player) {
		if (player == null) return false;
		World playerWorld = player.getWorld();
		if (playerWorld == null) return false;
		String playerWorldName = playerWorld.getName();
		if (type.equalsIgnoreCase("worlds")) {
			for (int i = 0; i < worlds.size(); i++) {
				if (worlds.get(i).equalsIgnoreCase(playerWorldName) || worlds.get(i).equalsIgnoreCase("*")) return true;
			}
		} else if (type.equalsIgnoreCase("house")) {
			if (minHouseLocation == null || maxHouseLocation == null) return false;
			Location currentLocation = player.getLocation();
			if (!currentLocation.getWorld().equals(minHouseLocation.getWorld())) return false;
			if (currentLocation.getX() > Math.max(minHouseLocation.getX(), maxHouseLocation.getX()) || currentLocation.getX() < Math.min(minHouseLocation.getX(), maxHouseLocation.getX())) return false;
			if (currentLocation.getZ() > Math.max(minHouseLocation.getZ(), maxHouseLocation.getZ()) || currentLocation.getZ() < Math.min(minHouseLocation.getZ(), maxHouseLocation.getZ())) return false;
			if (currentLocation.getY() > Math.max(minHouseLocation.getY(), maxHouseLocation.getY()) || currentLocation.getY() < Math.min(minHouseLocation.getY(), maxHouseLocation.getY())) return false;
			return true;
		}
		return false;
	}
	
	private boolean isLocationInScope(Location location) {
		if (location == null) return false;
		World playerWorld = location.getWorld();
		if (playerWorld == null) return false;
		String playerWorldName = playerWorld.getName();
		if (type.equalsIgnoreCase("worlds")) {
			for (int i = 0; i < worlds.size(); i++) {
				if (worlds.get(i).equalsIgnoreCase(playerWorldName) || worlds.get(i).equalsIgnoreCase("*")) return true;
			}
		} else if (type.equalsIgnoreCase("house")) {
			if (minHouseLocation == null || maxHouseLocation == null) return false;
			if (!location.getWorld().equals(minHouseLocation.getWorld())) return false;
			if (location.getX() > Math.max(minHouseLocation.getX(), maxHouseLocation.getX()) || location.getX() < Math.min(minHouseLocation.getX(), maxHouseLocation.getX())) return false;
			if (location.getZ() > Math.max(minHouseLocation.getZ(), maxHouseLocation.getZ()) || location.getZ() < Math.min(minHouseLocation.getZ(), maxHouseLocation.getZ())) return false;
			if (location.getY() > Math.max(minHouseLocation.getY(), maxHouseLocation.getY()) || location.getY() < Math.min(minHouseLocation.getY(), maxHouseLocation.getY())) return false;
			return true;
		}
		return false;
	}

	public ConfigurationSection getConfig() {
		return config;
	}

	public String getName() {
		return name;
	}

	public FileConfiguration getTextConfig() {
		return textConfig;
	}

	public ArrayList<Auction> getAuctionQueue() {
		return auctionQueue;
	}
	
	public static void checkAuctionQueue() {
		for (Map.Entry<String, AuctionScope> auctionScopesEntry : auctionScopes.entrySet()) {
			auctionScopesEntry.getValue().checkThisAuctionQueue();
		}
	}
	public static AuctionScope getPlayerScope(Player player) {
		if (player == null) return null;
		for (int i = 0; i < auctionScopesOrder.size(); i++) {
			String auctionScopeName = auctionScopesOrder.get(i);
			AuctionScope auctionScope = auctionScopes.get(auctionScopeName);
			if (auctionScope.isPlayerInScope(player)) return auctionScope;
		}
		return null;
	}
	public static AuctionScope getLocationScope(Location location) {
		if (location == null) return null;
		for (int i = 0; i < auctionScopesOrder.size(); i++) {
			String auctionScopeName = auctionScopesOrder.get(i);
			AuctionScope auctionScope = auctionScopes.get(auctionScopeName);
			if (auctionScope.isLocationInScope(location)) return auctionScope;
		}
		return null;
	}
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
	public static void cancelAllAuctions() {
		for (Map.Entry<String, AuctionScope> auctionScopesEntry : auctionScopes.entrySet()) {
			AuctionScope auctionScope = auctionScopesEntry.getValue();
			if (auctionScope.activeAuction != null) {
				auctionScope.activeAuction.cancel();
			}
			auctionScope.auctionQueue.clear();
		}
	}

	public static boolean areAuctionsRunning() {
		for (Map.Entry<String, AuctionScope> auctionScopesEntry : auctionScopes.entrySet()) {
			AuctionScope auctionScope = auctionScopesEntry.getValue();
			if (auctionScope.getActiveAuction() != null || auctionScope.getAuctionQueueLength() > 0) {
				return true;
			}
		}
		return false;
	}
}

