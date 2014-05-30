package com.flobi.floAuction;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.FileUtil;

import com.flobi.floAuction.foreign.MetricsLite;
import com.flobi.floAuction.foreign.Updater;
import com.flobi.floAuction.utility.functions;
import com.google.common.collect.Lists;

/**
 * A Bukkit based Minecraft plugin to facilitate auctions.
 * 
 * @author Joshua "flobi" Hatfield
 */
public class floAuction extends JavaPlugin {
	private static final Logger log = Logger.getLogger("Minecraft");

	// Got to figure out a better way to store these:
	public static int decimalPlaces = 0;
	public static String decimalRegex = "^[0-9]{0,13}(\\.[0-9]{1," + decimalPlaces + "})?$";
	public static boolean loadedDecimalFromVault = false;
	private static File auctionLog = null;
	private static boolean suspendAllAuctions = false;
	public static boolean useWhatIsIt = true;
	public static List<AuctionParticipant> auctionParticipants = new ArrayList<AuctionParticipant>();
	public static Map<String, String[]> userSavedInputArgs = new HashMap<String, String[]>();

	// Config files info.
	public static FileConfiguration config = null;
	public static FileConfiguration textConfig = null;
	private static File dataFolder;
	private static int queueTimer;
	static floAuction plugin;
	
	private static int playerScopeCheckTimer;
	static Map<String, String> playerScopeCache = new HashMap<String, String>();
	
	private static ArrayList<AuctionLot> orphanLots = new ArrayList<AuctionLot>();
	private static ArrayList<String> voluntarilyDisabledUsers = new ArrayList<String>();
	private static ArrayList<String> suspendedUsers = new ArrayList<String>();
	
	private static MessageManager messageManager = new AuctionMessageManager();
	
	/**
	 * Used by AuctinLot to store auction lots which could not be given to players because they were offline.
	 * 
	 * @param auctionLot AuctionLot to save.
	 */
	public static void saveOrphanLot(AuctionLot auctionLot) {
		floAuction.orphanLots.add(auctionLot);
		saveObject(floAuction.orphanLots, "orphanLots.ser");		
	}
	
	/**
	 * Saves an object to a file.
	 * 
	 * @param object object to save
	 * @param filename name of file
	 */
	private static void saveObject(Object object, String filename) {
    	File saveFile = new File(dataFolder, filename);
    	
    	try {
			//use buffering
    		if (saveFile.exists()) saveFile.delete();
    		FileOutputStream file = new FileOutputStream(saveFile.getAbsolutePath());
			OutputStream buffer = new BufferedOutputStream(file);
			ObjectOutput output = new ObjectOutputStream(buffer);
			try {
				output.writeObject(object);
			}
			finally {
				output.close();
			}
  	    }  
  	    catch(IOException ex){
    		return;
  	    }
	}
	
	/**
	 * Load a String array from a file.
	 * 
	 * @param filename where the file is
	 * @return the resulting string array
	 */
	@SuppressWarnings({ "unchecked", "finally" })
	private static ArrayList<String> loadArrayListString(String filename) {
    	File saveFile = new File(dataFolder, filename);
    	ArrayList<String> importedObjects = new ArrayList<String>();
    	try {
			//use buffering
			InputStream file = new FileInputStream(saveFile.getAbsolutePath());
			InputStream buffer = new BufferedInputStream(file);
			ObjectInput input = new ObjectInputStream (buffer);
			importedObjects = (ArrayList<String>) input.readObject();
			input.close();
  	    }  
		finally {
  	    	return importedObjects;
		}
	}
	
	/**
	 * Load a String String map from a file.
	 * 
	 * @param filename where the file is
	 * @return the resulting string string map
	 */
	@SuppressWarnings({ "unchecked", "finally" })
	private static Map<String, String[]> loadMapStringStringArray(String filename) {
    	File saveFile = new File(dataFolder, filename);
    	Map<String, String[]> importedObjects = new HashMap<String, String[]>();
    	try {
			//use buffering
			InputStream file = new FileInputStream(saveFile.getAbsolutePath());
			InputStream buffer = new BufferedInputStream(file);
			ObjectInput input = new ObjectInputStream (buffer);
			importedObjects = (Map<String, String[]>) input.readObject();
			input.close();
  	    }  
		finally {
  	    	return importedObjects;
		}
	}
	
	/**
	 * Load a list of AuctionLot from a file.
	 * 
	 * @param filename where the file is
	 * @return the loaded list
	 */
	@SuppressWarnings("unchecked")
	private static ArrayList<AuctionLot> loadArrayListAuctionLot(String filename) {
    	File saveFile = new File(dataFolder, filename);
    	ArrayList<AuctionLot> importedObjects = new ArrayList<AuctionLot>();
    	try {
			//use buffering
			InputStream file = new FileInputStream(saveFile.getAbsolutePath());
			InputStream buffer = new BufferedInputStream(file);
			ObjectInput input = new ObjectInputStream (buffer);
			importedObjects = (ArrayList<AuctionLot>) input.readObject();
			input.close();
  	    } catch (IOException e) {
			// This is okay, send a blank file.
//			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// This is okay, send a blank file.
//			e.printStackTrace();
		}  
		finally {
		}
    	return importedObjects;
	}
	
	/**
	 * Attempts to give lost AuctionLots back to their intended destination.
	 * 
	 * @param player the player to check for missing items
	 */
	// Eliminate orphan lots (i.e. try to give the items to a player again).
	public static void killOrphan(Player player) {
		if (orphanLots != null && orphanLots.size() > 0) {
			Iterator<AuctionLot> iter = orphanLots.iterator();
			while (iter.hasNext()) {
				AuctionLot lot = iter.next();
			    if (lot.getOwner().equalsIgnoreCase(player.getName())) {
			    	lot.cancelLot();
			        iter.remove();
			    }
			}
			saveObject(orphanLots, "orphanLots.ser");
		}
	}
	

	// Vault objects
	public static Economy econ = null;
    public static Permission perms = null;
    public static Chat chat = null;

    /**
     * Called by Bukkit when initializing.  Sets up basic plugin settings.
     */
	public void onEnable() {
		dataFolder = getDataFolder();
		plugin = this;
    	auctionLog = new File(dataFolder, "auctions.log");
		
        loadConfig();

        if (config.getBoolean("auto-update")) {
            new Updater(this, 38103, this.getFile(), Updater.UpdateType.DEFAULT, false);
        }
		
		if (Bukkit.getPluginManager().getPlugin("WhatIsIt") == null) {
			if (config.getBoolean("allow-inferior-item-name-logic")) {
				logToBukkit("recommended-whatisit", Level.WARNING);
				useWhatIsIt = false;
			} else {
				logToBukkit("plugin-disabled-no-whatisit", Level.SEVERE);
				Bukkit.getPluginManager().disablePlugin(this);
	            return;
			}
		} else {
			useWhatIsIt = true;
		}
		if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
			logToBukkit("plugin-disabled-no-vault", Level.SEVERE);
			Bukkit.getPluginManager().disablePlugin(this);
            return;
		}

		setupEconomy();
        setupPermissions();
        setupChat();

		if (econ == null) {
			logToBukkit("plugin-disabled-no-economy", Level.SEVERE);
			Bukkit.getPluginManager().disablePlugin(this);
            return;
		}
        
		ArenaManager.loadArenaListeners(this);
		
		Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void playerJoin(PlayerJoinEvent event) {
            	Player player = event.getPlayer();
        	    floAuction.killOrphan(player);
        	    AuctionScope.sendWelcomeMessage(player, true);
            }
            @EventHandler
            public void onPlayerChangedWorld(PlayerChangedWorldEvent event){
            	// Hopefully the teleport and portal things I just added will make this obsolete, but I figure I'll keep it just to make sure.
        		AuctionParticipant.forceLocation(event.getPlayer().getName(), null);
            }
            @EventHandler
            public void onPlayerChangedGameMode(PlayerGameModeChangeEvent event){
            	if (event.isCancelled()) return;
            	Player player = event.getPlayer();
            	String playerName = player.getName();
            	AuctionScope playerScope = AuctionScope.getPlayerScope(player);
            	Auction playerAuction = getPlayerAuction(player);
            	if (AuctionConfig.getBoolean("allow-gamemode-change", playerScope) || playerAuction == null) return;
            	
            	if (AuctionParticipant.isParticipating(playerName)) {
                	event.setCancelled(true);
                	messageManager.sendPlayerMessage(Lists.newArrayList("gamemodechange-fail-participating"), playerName, (AuctionScope) null);
            	}
            }
            @EventHandler(priority = EventPriority.LOWEST)
            public void onPlayerPreprocessCommand(PlayerCommandPreprocessEvent event){

            	if (event.isCancelled()) return;
            	Player player = event.getPlayer();
            	if (player == null) return;
            	String playerName = player.getName();
            	String message = event.getMessage();
            	if (message == null || message.isEmpty()) return;

            	AuctionScope playerScope = AuctionScope.getPlayerScope(player);
            	
            	// Check inscope disabled commands, doesn't matter if participating:
            	List<String> disabledCommands = AuctionConfig.getStringList("disabled-commands-inscope", playerScope);
        		for (int i = 0; i < disabledCommands.size(); i++) {
        			String disabledCommand = disabledCommands.get(i);
        			if (disabledCommand.isEmpty()) continue;
        			if (message.toLowerCase().startsWith(disabledCommand.toLowerCase())) {
    	            	event.setCancelled(true);
    	            	messageManager.sendPlayerMessage(Lists.newArrayList("disabled-command-inscope"), playerName, (AuctionScope) null);
        				return;
        			}
        		}
            	
            	// Check participating disabled commands
            	if (playerScope == null) return;
            	if (!AuctionParticipant.isParticipating(player.getName())) return;

            	disabledCommands = AuctionConfig.getStringList("disabled-commands-participating", playerScope);
        		for (int i = 0; i < disabledCommands.size(); i++) {
        			String disabledCommand = disabledCommands.get(i);
        			if (disabledCommand.isEmpty()) continue;
        			if (message.toLowerCase().startsWith(disabledCommand.toLowerCase())) {
    	            	event.setCancelled(true);
    	            	messageManager.sendPlayerMessage(Lists.newArrayList("disabled-command-participating"), playerName, (AuctionScope) null);
        				return;
        			}
        		}
            }
        	@EventHandler()
        	public void onPlayerMove(PlayerMoveEvent event) {
        		if (event.isCancelled()) return;
        		AuctionParticipant.forceLocation(event.getPlayer().getName(), event.getTo());
        	}
        	@EventHandler()
        	public void onPlayerTeleport(PlayerTeleportEvent event) {
        		if (event.isCancelled()) return;
        		if (!AuctionParticipant.checkTeleportLocation(event.getPlayer().getName(), event.getTo())) event.setCancelled(true);
        	}
        	@EventHandler()
        	public void onPlayerPortalEvent(PlayerPortalEvent event) {
        		if (event.isCancelled()) return;
        		if (!AuctionParticipant.checkTeleportLocation(event.getPlayer().getName(), event.getTo())) event.setCancelled(true);
        	}
        }, this);
		
		BukkitScheduler bukkitScheduler = getServer().getScheduler();
		if (queueTimer > 0) bukkitScheduler.cancelTask(queueTimer);
		queueTimer = bukkitScheduler.scheduleSyncRepeatingTask(this, new Runnable() {
		    public void run() {
		    	AuctionScope.checkAuctionQueue();
		    }
		}, 20L, 20L);
		
		long playerScopeCheckInterval = config.getLong("auctionscope-change-check-interval");
		if (playerScopeCheckTimer > 0) bukkitScheduler.cancelTask(playerScopeCheckTimer);
		if (playerScopeCheckInterval > 0) {
			playerScopeCheckTimer = bukkitScheduler.scheduleSyncRepeatingTask(this, new Runnable() {
			    public void run() {
			    	AuctionScope.sendFairwellMessages();
			    	AuctionScope.sendWelcomeMessages();
			    }
			}, playerScopeCheckInterval, playerScopeCheckInterval);
		}
		
		orphanLots = loadArrayListAuctionLot("orphanLots.ser");
		floAuction.voluntarilyDisabledUsers = loadArrayListString("voluntarilyDisabledUsers.ser");
		suspendedUsers = loadArrayListString("suspendedUsers.ser");
		userSavedInputArgs = loadMapStringStringArray("userSavedInputArgs.ser");

        // Load up the Plugin metrics
        try {
            MetricsLite metrics = new MetricsLite(this);
            metrics.start();
        } catch (IOException e) {
            // Failed to submit the stats :-(
        }
        messageManager.sendPlayerMessage(Lists.newArrayList("plugin-enabled"), null, (AuctionScope) null);
		
	}
    /**
	 * Loads config.yml and language.yml configuration files.
	 */
    private static void loadConfig() {
		File configFile = new File(dataFolder, "config.yml");
    	InputStream defConfigStream = plugin.getResource("config.yml");;
    	File textConfigFile = new File(dataFolder, "language.yml");
    	InputStream defTextConfigStream = plugin.getResource("language.yml");;
    	YamlConfiguration defConfig = null;
    	YamlConfiguration defTextConfig = null;
		
		config = null;
	    config = YamlConfiguration.loadConfiguration(configFile);
	 
	    // Look for defaults in the jar
	    if (defConfigStream != null) {
	        defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
	        defConfigStream = null;
	    }
	    if (defConfig != null) {
	    	config.setDefaults(defConfig);
	    }
	    
	    textConfig = null;
	    
	    // Check to see if this needs converstion from floAuction version 2:
	    // suppress-auction-start-info was added in 2.6 and removed in 3.0.
	    if (config.contains("suppress-auction-start-info")) {
	    	// I want to save a copy of the config and language files for them.
	    	FileUtil.copy(configFile, new File(dataFolder, "config.v2-backup.yml"));
	    	FileUtil.copy(textConfigFile, new File(dataFolder, "language.v2-backup.yml"));
	    	
	    	// Late version 2's also had an auction house.  If it has this, it needs to be converted.
	    	String houseWorld = config.getString("auctionhouse-world");
	    	if (houseWorld != null && !houseWorld.isEmpty()) {
	    		YamlConfiguration house = new YamlConfiguration();
	    		house.set("name", "Auction House");
	    		house.set("type", "house");
	    		house.set("house-world", houseWorld);
	    		house.set("house-min-x", config.get("auctionhouse-min-x"));
	    		house.set("house-min-y", config.get("auctionhouse-min-y"));
	    		house.set("house-min-z", config.get("auctionhouse-min-z"));
	    		house.set("house-max-x", config.get("auctionhouse-max-x"));
	    		house.set("house-max-y", config.get("auctionhouse-max-y"));
	    		house.set("house-max-z", config.get("auctionhouse-max-z"));
	    		YamlConfiguration scopes = new YamlConfiguration();
	    		scopes.set("house", house);
	    		config.set("auction-scopes", scopes);
	    	}
	    	config.set("disabled-commands-participating", config.get("disabled-commands"));
	    	// The unused rows will be removed through the cleaning process.
	    	// The entire language file needs to be purged though.
		    textConfig = new YamlConfiguration();
	    } else {
		    textConfig = YamlConfiguration.loadConfiguration(textConfigFile);
	    }

	    // Look for defaults in the jar
	    if (defTextConfigStream != null) {
	        defTextConfig = YamlConfiguration.loadConfiguration(defTextConfigStream);
	        defTextConfigStream = null;
	    }
	    if (defTextConfig != null) {
	        textConfig.setDefaults(defTextConfig);
	    }
	    
		// Clean up the configuration of any unsed values.
		FileConfiguration cleanConfig = new YamlConfiguration();
		Map<String, Object> configValues = config.getDefaults().getValues(false);
		for (Map.Entry<String, Object> configEntry : configValues.entrySet()) {
			cleanConfig.set(configEntry.getKey(), config.get(configEntry.getKey()));
		}
		config = cleanConfig;

    	try {
    		config.save(configFile);
		} catch(IOException ex) {
			log.severe("Cannot save config.yml");
		}
    	defConfig = null;
	    configFile = null;

	    
	    // Another typo fix from 3.0.0
	    if (textConfig.contains("plogin-reload-fail-permissions")) {
	    	textConfig.set("plugin-reload-fail-permissions", textConfig.get("plogin-reload-fail-permissions"));
	    }
	    
		FileConfiguration cleanTextConfig = new YamlConfiguration();
		Map<String, Object> textConfigValues = textConfig.getDefaults().getValues(false);
		for (Map.Entry<String, Object> textConfigEntry : textConfigValues.entrySet()) {
			cleanTextConfig.set(textConfigEntry.getKey(), textConfig.get(textConfigEntry.getKey()));
		}
		textConfig = cleanTextConfig;
		
		// Here's an oppsie fix for a typo in 3.0.0.
		if (textConfig.getString("bid-fail-under-starting-bid") != null && textConfig.getString("bid-fail-under-starting-bid").equals("&6The bidding must start at %A8.")) {
			textConfig.set("bid-fail-under-starting-bid", "&6The bidding must start at %A4.");
		}

		try {
    		textConfig.save(textConfigFile);
		} catch(IOException ex) {
			log.severe("Cannot save language.yml");
		}
        defTextConfig = null;
	    textConfigFile = null;
	    
	    // Build auction scopes.
	    AuctionScope.setupScopeList(config.getConfigurationSection("auction-scopes"), dataFolder);
	    
    }
    
    /**
     * Called by Bukkit when disabling.  Cancels all auctions and clears data.
     */
	public void onDisable() {
		AuctionScope.cancelAllAuctions();
		getServer().getScheduler().cancelTask(queueTimer);
		plugin = null;
		logToBukkit("plugin-disabled", Level.INFO);
		auctionLog = null;
	}
	
    // Overrides onCommand from Plugin
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

    	// Make sure the decimalPlaces loaded correctly.
    	// Sometimes the econ loads after floAuction.
	    if (!loadedDecimalFromVault && econ.isEnabled()) {
	    	loadedDecimalFromVault = true;
			decimalPlaces = Math.max(econ.fractionalDigits(), 0);
			config.set("decimal-places", decimalPlaces);
			if (decimalPlaces < 1) {
				decimalRegex = "^[0-9]{1,13}$";
			} else if (decimalPlaces == 1) {
				decimalRegex = "^[0-9]{0,13}(\\.[0-9])?$";
			} else {
				decimalRegex = "^[0-9]{0,13}(\\.[0-9]{1," + decimalPlaces + "})?$";
			}
	    }
    	
    	Player player = null;
    	Auction auction = null;
		AuctionScope userScope = null;
		String playerName = null;

    	if (sender instanceof Player) {
    		player = (Player) sender;
			playerName = player.getName();
			userScope = AuctionScope.getPlayerScope(player);
			if (userScope != null) {
				auction = userScope.getActiveAuction();
			}
//    	} else {
//			playerName = "*console*";
    	}

		if (
				(cmd.getName().equalsIgnoreCase("auction") || cmd.getName().equalsIgnoreCase("auc")) &&
				args.length > 0 &&
				args[0].equalsIgnoreCase("on")
		) {
			int index = getVoluntarilyDisabledUsers().indexOf(playerName);
			if (index != -1) {
				getVoluntarilyDisabledUsers().remove(index);
			}
			messageManager.sendPlayerMessage(Lists.newArrayList("auction-enabled"), playerName, (AuctionScope) null);
			saveObject(getVoluntarilyDisabledUsers(), "voluntarilyDisabledUsers.ser");
			return true;
		}
     
    	if (getVoluntarilyDisabledUsers().contains(playerName)) {
    		getVoluntarilyDisabledUsers().remove(getVoluntarilyDisabledUsers().indexOf(playerName));
    		messageManager.sendPlayerMessage(Lists.newArrayList("auction-fail-disabled"), playerName, (AuctionScope) null);
			getVoluntarilyDisabledUsers().add(playerName);
			saveObject(getVoluntarilyDisabledUsers(), "voluntarilyDisabledUsers.ser");
			return true;
		}
    	
    	if (
    		cmd.getName().equalsIgnoreCase("auc") ||
    		cmd.getName().equalsIgnoreCase("auction") ||
    		cmd.getName().equalsIgnoreCase("sauc") ||
    		cmd.getName().equalsIgnoreCase("sealedauction")
    	) {
    		if (args.length > 0) {
				if (args[0].equalsIgnoreCase("reload")) {
    				if (player != null && !perms.has(player, "auction.admin")) {
    					messageManager.sendPlayerMessage(Lists.newArrayList("plugin-reload-fail-permissions"), playerName, (AuctionScope) null);
    	    			return true;
    				}
    		    	// Don't reload if any auctions are running.
    				if (AuctionScope.areAuctionsRunning()) {
    					messageManager.sendPlayerMessage(Lists.newArrayList("plugin-reload-fail-auctions-running"), playerName, (AuctionScope) null);
						return true;
    				}

    				loadConfig();
    				messageManager.sendPlayerMessage(Lists.newArrayList("plugin-reloaded"), playerName, (AuctionScope) null);
    				return true;
    			} else if (args[0].equalsIgnoreCase("resume")) {
			    	if (args.length == 1) {
						if (player != null && !perms.has(player, "auction.admin")) {
							messageManager.sendPlayerMessage(Lists.newArrayList("unsuspension-fail-permissions"), playerName, (AuctionScope) null);
			    			return true;
						}
						// Resume globally:
						suspendAllAuctions = false;
						messageManager.broadcastAuctionScopeMessage(Lists.newArrayList("unsuspension-global"), (AuctionScope) null);
						return true;
    		    	}
    		    	
    				if (player != null && !perms.has(player, "auction.admin")) {
    					messageManager.sendPlayerMessage(Lists.newArrayList("unsuspension-fail-permissions"), playerName, (AuctionScope) null);
    	    			return true;
    				}

					if (!suspendedUsers.contains(args[1].toLowerCase())) {
						messageManager.sendPlayerMessage(Lists.newArrayList("unsuspension-user-fail-not-suspended"), playerName, (AuctionScope) null);
		    			return true;
					}

					suspendedUsers.remove(args[1].toLowerCase());
					saveObject(suspendedUsers, "suspendedUsers.ser");
					messageManager.sendPlayerMessage(Lists.newArrayList("unsuspension-user"), args[1], (AuctionScope) null);
			    	messageManager.sendPlayerMessage(Lists.newArrayList("unsuspension-user-success"), playerName, (AuctionScope) null);
    				
    				return true;
    			} else if (args[0].equalsIgnoreCase("suspend")) {
    				if (player != null && !perms.has(player, "auction.admin")) {
    					messageManager.sendPlayerMessage(Lists.newArrayList("suspension-fail-permissions"), playerName, (AuctionScope) null);
    	    			return true;
    				}
    				if (args.length > 1) {
    					// Suspend a player:
    					if (suspendedUsers.contains(args[1].toLowerCase())) {
    						messageManager.sendPlayerMessage(Lists.newArrayList("suspension-user-fail-already-suspended"), playerName, (AuctionScope) null);
    		    			return true;
    					}
    					
    					Player playerToSuspend = getServer().getPlayer(args[1]);
    					
    					if (playerToSuspend == null || !playerToSuspend.isOnline()) {
    						messageManager.sendPlayerMessage(Lists.newArrayList("suspension-user-fail-is-offline"), playerName, (AuctionScope) null);
    		    			return true;
    					}
    					
    					if (perms.has(playerToSuspend, "auction.admin")) {
    						messageManager.sendPlayerMessage(Lists.newArrayList("suspension-user-fail-is-admin"), playerName, (AuctionScope) null);
    		    			return true;
    					}
    					
    					suspendedUsers.add(args[1].toLowerCase());
    					saveObject(suspendedUsers, "suspendedUsers.ser");
    					messageManager.sendPlayerMessage(Lists.newArrayList("suspension-user"), playerToSuspend.getName(), (AuctionScope) null);
    			    	messageManager.sendPlayerMessage(Lists.newArrayList("suspension-user-success"), playerName, (AuctionScope) null);
    					
    					return true;
    				}
    				// Suspend globally:
    				suspendAllAuctions = true;
    				
    				AuctionScope.cancelAllAuctions();

    		    	messageManager.broadcastAuctionScopeMessage(Lists.newArrayList("suspension-global"), null);

	    			return true;
    			} else if (
        				args[0].equalsIgnoreCase("start") || 
        				args[0].equalsIgnoreCase("s") ||
        				args[0].equalsIgnoreCase("this") ||
        				args[0].equalsIgnoreCase("hand") ||
        				args[0].equalsIgnoreCase("all") ||
        				args[0].matches("[0-9]+")
    			) {
    		    	if (suspendAllAuctions) {
    			    	messageManager.sendPlayerMessage(Lists.newArrayList("suspension-global"), playerName, (AuctionScope) null);
    		    		return true;
    		    	}
    		    	if (player != null && suspendedUsers.contains(playerName.toLowerCase())) {
    		    		messageManager.sendPlayerMessage(Lists.newArrayList("suspension-user"), playerName, (AuctionScope) null);
    					return true;
    		    	}

    				// Start new auction!
    	    		if (player == null) {
    	    			messageManager.sendPlayerMessage(Lists.newArrayList("auction-fail-console"), playerName, (AuctionScope) null);
    	    			return true;
    	    		}
    	    		if (!AuctionConfig.getBoolean("allow-gamemode-creative", userScope) && player.getGameMode() == GameMode.CREATIVE) {
    	    			messageManager.sendPlayerMessage(Lists.newArrayList("auction-fail-gamemode-creative"), playerName, (AuctionScope) null);
    	    			return true;
    	    		}
    	    		if (userScope == null) {
    	    			messageManager.sendPlayerMessage(Lists.newArrayList("auction-fail-no-scope"), playerName, (AuctionScope) null);
    	    			return true;
    	    		}
    	    			
    				if (!perms.has(player, "auction.start")) {
    					messageManager.sendPlayerMessage(Lists.newArrayList("auction-fail-permissions"), playerName, (AuctionScope) null);
    	    			return true;
    				}
    				
    				if (!AuctionConfig.getBoolean("allow-sealed-auctions", userScope) && !AuctionConfig.getBoolean("allow-unsealed-auctions", userScope)) {
    					messageManager.sendPlayerMessage(Lists.newArrayList("auction-fail-no-auctions-allowed"), playerName, (AuctionScope) null);
    					return true;
    				}
    				
    				if (cmd.getName().equalsIgnoreCase("sealedauction") || cmd.getName().equalsIgnoreCase("sauc")) {
    					if (AuctionConfig.getBoolean("allow-sealed-auctions", userScope)) {
    						userScope.queueAuction(new Auction(this, player, args, userScope, true, messageManager));
    					} else {
    						messageManager.sendPlayerMessage(Lists.newArrayList("auction-fail-no-sealed-auctions"), playerName, (AuctionScope) null);
    					}
    				} else {
    					if (AuctionConfig.getBoolean("allow-unsealed-auctions", userScope)) {
    						userScope.queueAuction(new Auction(this, player, args, userScope, false, messageManager));
    					} else {
    						userScope.queueAuction(new Auction(this, player, args, userScope, true, messageManager));
    					}
    				}

					return true;
    			} else if (
        				args[0].equalsIgnoreCase("prep") || 
        				args[0].equalsIgnoreCase("p")
    			) {
    				// Save a users individual starting default values.
    	    		if (player == null) {
    	    			messageManager.sendPlayerMessage(Lists.newArrayList("auction-fail-console"), playerName, (AuctionScope) null);
    	    			return true;
    	    		}
    				if (!perms.has(player, "auction.start")) {
    					messageManager.sendPlayerMessage(Lists.newArrayList("auction-fail-permissions"), playerName, (AuctionScope) null);
    	    			return true;
    				}
    				
    				// The function returns null and sends error on failure.
    				String[] mergedArgs = functions.mergeInputArgs(playerName, args, true);
    				
    				if (mergedArgs != null) {
						floAuction.userSavedInputArgs.put(playerName, mergedArgs);
						floAuction.saveObject(floAuction.userSavedInputArgs, "userSavedInputArgs.ser");
						messageManager.sendPlayerMessage(Lists.newArrayList("prep-save-success"), playerName, (AuctionScope) null);
    				}

					return true;
    			} else if (args[0].equalsIgnoreCase("cancel") || args[0].equalsIgnoreCase("c")) {
    	    		if (userScope == null) {
    	    			messageManager.sendPlayerMessage(Lists.newArrayList("auction-fail-no-scope"), playerName, (AuctionScope) null);
    	    			return true;
    	    		}
    				if (userScope.getActiveAuction() == null && userScope.getAuctionQueueLength() == 0) {
    					messageManager.sendPlayerMessage(Lists.newArrayList("auction-fail-no-auction-exists"), playerName, (AuctionScope) null);
    					return true;
    				}
    				
    				ArrayList<Auction> auctionQueue = userScope.getAuctionQueue();
    				for(int i = 0; i < auctionQueue.size(); i++){
    					if (auctionQueue.get(i).getOwner().equalsIgnoreCase(playerName)) {
    						auctionQueue.remove(i);
    						messageManager.sendPlayerMessage(Lists.newArrayList("auction-cancel-queued"), playerName, (AuctionScope) null);
    						return true;
    					}
    				}
    				
    				if (auction == null) {
    					messageManager.sendPlayerMessage(Lists.newArrayList("auction-fail-no-auction-exists"), playerName, (AuctionScope) null);
    					return true;
    				}
    				
					if (player == null || player.getName().equalsIgnoreCase(auction.getOwner()) || perms.has(player, "auction.admin")) {
						if (AuctionConfig.getInt("cancel-prevention-seconds", userScope) > auction.getRemainingTime() || AuctionConfig.getDouble("cancel-prevention-percent", userScope) > (double)auction.getRemainingTime() / (double)auction.getTotalTime() * 100D) {
							messageManager.sendPlayerMessage(Lists.newArrayList("auction-fail-cancel-prevention"), playerName, (AuctionScope) null);
						} else {
	    					auction.cancel();
						}
					} else {
						messageManager.sendPlayerMessage(Lists.newArrayList("auction-fail-not-owner-cancel"), playerName, (AuctionScope) null);
					}
    				return true;
    			} else if (args[0].equalsIgnoreCase("confiscate") || args[0].equalsIgnoreCase("impound")) {
    				if (auction == null) {
    					messageManager.sendPlayerMessage(Lists.newArrayList("auction-fail-no-auction-exists"), playerName, (AuctionScope) null);
    					return true;
    				}
    				
    				if (player == null) {
    					messageManager.sendPlayerMessage(Lists.newArrayList("confiscate-fail-console"), playerName, (AuctionScope) null);
    					return true;
    				}
					if (!perms.has(player, "auction.admin")) {
						messageManager.sendPlayerMessage(Lists.newArrayList("confiscate-fail-permissions"), playerName, (AuctionScope) null);
    					return true;
					}
					if (playerName.equalsIgnoreCase(auction.getOwner())) {
						messageManager.sendPlayerMessage(Lists.newArrayList("confiscate-fail-self"), playerName, (AuctionScope) null);
    					return true;
					}
					auction.confiscate(player);
    				return true;
    			} else if (args[0].equalsIgnoreCase("end") || args[0].equalsIgnoreCase("e")) {
    				if (auction == null) {
    					messageManager.sendPlayerMessage(Lists.newArrayList("auction-fail-no-auction-exists"), playerName, (AuctionScope) null);
        				return true;
    				}
    				if (!AuctionConfig.getBoolean("allow-early-end", userScope)) {
    					messageManager.sendPlayerMessage(Lists.newArrayList("auction-fail-no-early-end"), playerName, (AuctionScope) null);
        				return true;
    				}
					if (player.getName().equalsIgnoreCase(auction.getOwner())) {
    					auction.end();
					} else {
						messageManager.sendPlayerMessage(Lists.newArrayList("auction-fail-not-owner-end"), playerName, (AuctionScope) null);
					}
    				return true;
    			} else if (
    					args[0].equalsIgnoreCase("stfu") ||
    					args[0].equalsIgnoreCase("ignore") ||
        				args[0].equalsIgnoreCase("quiet") ||
        				args[0].equalsIgnoreCase("off") ||
        				args[0].equalsIgnoreCase("silent") ||
        				args[0].equalsIgnoreCase("silence")
    			) {
    				if (getVoluntarilyDisabledUsers().indexOf(playerName) == -1) {
    					messageManager.sendPlayerMessage(Lists.newArrayList("auction-disabled"), playerName, (AuctionScope) null);
    					getVoluntarilyDisabledUsers().add(playerName);
    					saveObject(getVoluntarilyDisabledUsers(), "voluntarilyDisabledUsers.ser");
    				}
    				return true;
    			} else if (args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("i")) {
    				if (auction == null) {
    					messageManager.sendPlayerMessage(Lists.newArrayList("auction-info-no-auction"), playerName, (AuctionScope) null);
    					return true;
    				}
					auction.info(sender, false);
    				return true;
    			} else if (args[0].equalsIgnoreCase("queue") || args[0].equalsIgnoreCase("q")) {
    				ArrayList<Auction> auctionQueue = userScope.getAuctionQueue();
    				if (auctionQueue.isEmpty()) {
    					messageManager.sendPlayerMessage(Lists.newArrayList("auction-queue-status-not-in-queue"), playerName, (AuctionScope) null);
    					return true;
    				}
    				for(int i = 0; i < auctionQueue.size(); i++){
    					if (auctionQueue.get(i).getOwner().equalsIgnoreCase(playerName)) {
    						messageManager.sendPlayerMessage(Lists.newArrayList("auction-queue-status-in-queue"), playerName, (AuctionScope) null);
    						return true;
    					}
    				}

    				messageManager.sendPlayerMessage(Lists.newArrayList("auction-queue-status-not-in-queue"), playerName, (AuctionScope) null);
    				return true;
    			}
    		}
    		messageManager.sendPlayerMessage(Lists.newArrayList("auction-help"), playerName, (AuctionScope) null);
    		return true;
    	} else if (cmd.getName().equalsIgnoreCase("bid")) {
        	if (suspendAllAuctions) {
        		messageManager.sendPlayerMessage(Lists.newArrayList("suspension-global"), playerName, (AuctionScope) null);
        		return true;
        	}
        	if (player != null && suspendedUsers.contains(playerName.toLowerCase())) {
        		messageManager.sendPlayerMessage(Lists.newArrayList("suspension-user"), playerName, (AuctionScope) null);
    			return true;
        	}

    		if (player == null) {
    			messageManager.sendPlayerMessage(Lists.newArrayList("bid-fail-console"), playerName, (AuctionScope) null);
    			return true;
    		} 
    		if (!AuctionConfig.getBoolean("allow-gamemode-creative", userScope) && player.getGameMode().equals(GameMode.CREATIVE)) {
    			messageManager.sendPlayerMessage(Lists.newArrayList("bid-fail-gamemode-creative"), playerName, (AuctionScope) null);
    			return true;
    		}
			if (!perms.has(player, "auction.bid")) {
				messageManager.sendPlayerMessage(Lists.newArrayList("bid-fail-permissions"), playerName, (AuctionScope) null);
    			return true;
			}
    		if (auction == null) {
    			messageManager.sendPlayerMessage(Lists.newArrayList("bid-fail-no-auction"), playerName, (AuctionScope) null);
    			return true;
    		}
    		auction.Bid(player, args);
    		return true;
    	}
    	return false;
    }
    
    /**
     * Log data to the floAuction log file if logging is enabled.
     * 
     * @param sender who is initiating the logged event
     * @param message message to save
     */
    static void log(String playerName, String message, AuctionScope auctionScope) {
    	if (AuctionConfig.getBoolean("log-auctions", auctionScope)) {
    		String scopeId = null;
    		
			BufferedWriter out = null;
			try {
		    	if (auctionLog == null || !auctionLog.exists()) {
					auctionLog.createNewFile();
					auctionLog.setWritable(true);
		    	}
		    	
				out = new BufferedWriter(new FileWriter(auctionLog.getAbsolutePath(), true));

				if (auctionScope == null) {
					scopeId = "NOSCOPE";
				} else {
					scopeId = auctionScope.getScopeId();
				}
				
				out.append((new Date()).toString() + " (" + playerName + ", " + scopeId + "): " + ChatColor.stripColor(message) + "\n");
				out.close();

			} catch (IOException e) {
				
			}
    	}
	}

    /**
     * Setup Vault economy.
     * 
     * @return success level
     */
    private boolean setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    /**
     * Setup Vault chat.
     * 
     * @return success level
     */
    private boolean setupChat() {
        RegisteredServiceProvider<Chat> rsp = Bukkit.getServicesManager().getRegistration(Chat.class);
        if (rsp == null) {
            return false;
        }
        chat = rsp.getProvider();
        return chat != null;
    }

    /**
     * Setup Vault permission.
     * 
     * @return success level
     */
    private boolean setupPermissions() {
        RegisteredServiceProvider<Permission> rsp = Bukkit.getServicesManager().getRegistration(Permission.class);
        perms = rsp.getProvider();
        return perms != null;
    }

	/**
	 * Gets the active auction instance from the scope where the player is.
	 * 
	 * @param playerName player in reference
	 * @return auction instance
	 */
	public static Auction getPlayerAuction(String playerName) {
		if (playerName == null) return null;
		return getPlayerAuction(Bukkit.getPlayer(playerName));
	}

	/**
	 * Gets the active auction instance from the scope where the player is.
	 * 
	 * @param player player in reference
	 * @return auction instance
	 */
	public static Auction getPlayerAuction(Player player) {
		if (player == null) return null;
		AuctionScope auctionScope = AuctionScope.getPlayerScope(player);
		if (auctionScope == null) return null;
		return auctionScope.getActiveAuction();
	}

	public static ArrayList<String> getVoluntarilyDisabledUsers() {
		return voluntarilyDisabledUsers;
	}
    
    /**
     * Prepares chat, prepending prefix and removing colors.
     * 
	 * @param message message to prepare
	 * @param auctionScope the scope of the destination
	 * @return prepared message
     */
    private static String chatPrepClean(String message, AuctionScope auctionScope) {
    	message = AuctionConfig.getLanguageString("chat-prefix", auctionScope) + message;
    	message = ChatColor.translateAlternateColorCodes('&', message);
    	message = ChatColor.stripColor(message);
    	return message;
    }
    
    public static MessageManager getMessageManager() {
    	return messageManager;
    }

    private void logToBukkit(String key, Level level) {
    	List<String> messageList = AuctionConfig.getLanguageStringList(key, null);
    	
    	String originalMessage = null;
    	if (messageList == null || messageList.size() == 0) {
    		originalMessage = AuctionConfig.getLanguageString(key, null);
    		
    		if (originalMessage != null && originalMessage.length() != 0) {
        		messageList = Arrays.asList(originalMessage.split("(\r?\n|\r)"));
    		}
    	}
    	for (Iterator<String> i = messageList.iterator(); i.hasNext(); ) {
    		String messageListItem = i.next();
    		log.log(level, chatPrepClean(messageListItem, null));
    	}
    }
    
}

