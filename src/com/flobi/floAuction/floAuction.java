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
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Material;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import com.flobi.utility.functions;
import com.flobi.utility.items;

/**
 * A Bukkit based Minecraft plugin to facilitate auctions.
 * 
 * @author Joshua "flobi" Hatfield
 */
public class floAuction extends JavaPlugin {
	private static final Logger log = Logger.getLogger("Minecraft");

	// Got to figure out a better way to store these:
	public static int decimalPlaces = 2;
	public static String decimalRegex = "^[0-9]{0,13}(\\.[0-9]{1," + decimalPlaces + "})?$";
	public static boolean loadedDecimalFromVault = false;
	private static File auctionLog = null;
	private static boolean suspendAllAuctions = false;
	public static boolean useWhatIsIt = true;
	public static List<AuctionParticipant> auctionParticipants = new ArrayList<AuctionParticipant>();
	public static Map<String, String[]> userSavedInputArgs = new HashMap<String, String[]>();
	private static AuctionWebserver auctionWebserver = null; 

	// Config files info.
	public static FileConfiguration config = null;
	public static FileConfiguration textConfig = null;
	private static File dataFolder;
	private static ConsoleCommandSender console;
	private static int queueTimer;
	static floAuction plugin;
	
	private static ArrayList<AuctionLot> orphanLots = new ArrayList<AuctionLot>();
	private static ArrayList<String> voluntarilyDisabledUsers = new ArrayList<String>();
	private static ArrayList<String> suspendedUsers = new ArrayList<String>();
	
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
		// List of orphans to potentially kill.
		ArrayList<AuctionLot> orphanDeathRow = orphanLots;
		
		// New orphanage.
		orphanLots = new ArrayList<AuctionLot>();
		
		// KILL THEM ALL!
		for(int i = 0; i < orphanDeathRow.size(); i++) {
			// Hmm, only if they're actually orphans though.
			if (orphanDeathRow.get(i) != null) {
				// And only if they belong to player.
				AuctionLot lot = orphanDeathRow.get(i);
				
				if (lot.getOwner().equalsIgnoreCase(player.getName())) {
					lot.cancelLot();
					orphanDeathRow.set(i, null);
				} else {
					// This one's still alive, put it back in the orphanage.
					orphanLots.add(lot);
				}
			}
		}
		saveObject(orphanLots, "orphanLots.ser");
	}
	

	// Vault objects
	public static Economy econ = null;
    public static Permission perms = null;
    public static Chat chat = null;

    /**
     * Called by Bukkit when initializing.  Sets up basic plugin settings.
     */
	public void onEnable() {
		console = Bukkit.getConsoleSender();
		dataFolder = getDataFolder();
		plugin = this;
    	auctionLog = new File(dataFolder, "auctions.log");
		
		setupEconomy();
        setupPermissions();
        setupChat();

        loadConfig();
		if (Bukkit.getPluginManager().getPlugin("WhatIsIt") == null) {
			if (config.getBoolean("allow-inferior-item-name-logic")) {
				log.log(Level.SEVERE, chatPrepClean(AuctionConfig.getLanguageString("whatisit-recommended", null), null));
				useWhatIsIt = false;
			} else {
				log.log(Level.SEVERE, chatPrepClean(AuctionConfig.getLanguageString("no-whatisit", null), null));
				Bukkit.getPluginManager().disablePlugin(this);
	            return;
			}
		} else {
			useWhatIsIt = true;
		}
		if (econ == null) {
			log.log(Level.SEVERE, chatPrepClean(AuctionConfig.getLanguageString("no-economy", null), null));
			Bukkit.getPluginManager().disablePlugin(this);
            return;
		}
        
		Bukkit.getPluginManager().registerEvents(new ArenaManager(), this);
		
		Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void playerJoin(PlayerJoinEvent event) {
        	    floAuction.killOrphan(event.getPlayer());
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
            	AuctionScope playerScope = AuctionScope.getPlayerScope(player);
            	Auction playerAuction = getPlayerAuction(player);
            	if (AuctionConfig.getBoolean("allow-gamemode-change", playerScope) || playerAuction == null) return;
            	
            	if (AuctionParticipant.isParticipating(player.getName())) {
                	event.setCancelled(true);
                	sendMessage("gamemodechange-fail-participating", player, playerScope, false);
            	}
            }
            @EventHandler(priority = EventPriority.LOWEST)
            public void onPlayerPreprocessCommand(PlayerCommandPreprocessEvent event){

            	if (event.isCancelled()) return;
            	Player player = event.getPlayer();
            	if (player == null) return;
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
    	            	sendMessage("disabled-command-inscope", event.getPlayer(), playerScope, false);
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
    	            	sendMessage("disabled-command-participating", event.getPlayer(), playerScope, false);
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
		
		queueTimer = getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
		    public void run() {
		    	AuctionScope.checkAuctionQueue();
		    }
		}, 20L, 20L);
		
		orphanLots = loadArrayListAuctionLot("orphanLots.ser");
		voluntarilyDisabledUsers = loadArrayListString("voluntarilyDisabledUsers.ser");
		suspendedUsers = loadArrayListString("suspendedUsers.ser");
		userSavedInputArgs = loadMapStringStringArray("userSavedInputArgs.ser");

        // Load up the Plugin metrics
        try {
            MetricsLite metrics = new MetricsLite(this);
            metrics.start();
        } catch (IOException e) {
            // Failed to submit the stats :-(
        }
        
		sendMessage("plugin-enabled", console, null, false);
		
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
	    textConfig = YamlConfiguration.loadConfiguration(textConfigFile);
	 
	    // Look for defaults in the jar
	    if (defTextConfigStream != null) {
	        defTextConfig = YamlConfiguration.loadConfiguration(defTextConfigStream);
	        defTextConfigStream = null;
	    }
	    if (defTextConfig != null) {
	        textConfig.setDefaults(defTextConfig);
	    }
	    
	    decimalPlaces = config.getInt("decimal-places");
	    if (econ != null && econ.isEnabled()) {
	    	if (econ.fractionalDigits() >= 0) {
				decimalPlaces = econ.fractionalDigits();
	    	}
	    	loadedDecimalFromVault = true;
	    }
		config.set("decimal-places", decimalPlaces);
		if (decimalPlaces < 1) {
			decimalRegex = "^[0-9]{1,13}$";
		} else if (decimalPlaces == 1) {
			decimalRegex = "^[0-9]{0,13}(\\.[0-9])?$";
		} else {
			decimalRegex = "^[0-9]{0,13}(\\.[0-9]{1," + decimalPlaces + "})?$";
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

	    
		FileConfiguration cleanTextConfig = new YamlConfiguration();
		Map<String, Object> textConfigValues = textConfig.getDefaults().getValues(false);
		for (Map.Entry<String, Object> textConfigEntry : textConfigValues.entrySet()) {
			cleanTextConfig.set(textConfigEntry.getKey(), textConfig.get(textConfigEntry.getKey()));
		}
		textConfig = cleanTextConfig;

		try {
    		textConfig.save(textConfigFile);
		} catch(IOException ex) {
			log.severe("Cannot save language.yml");
		}
        defTextConfig = null;
	    textConfigFile = null;
	    
	    // Build auction scopes.
	    AuctionScope.setupScopeList(config.getConfigurationSection("auction-scopes"), dataFolder);
	    
	    // Enable webserver
        int port = config.getInt("web-port");
	    if (auctionWebserver == null) {
			if (port > 0) {
		        auctionWebserver = new AuctionWebserver(port);
		        try {
					auctionWebserver.start();
				} catch (IOException e) {
					auctionWebserver = null;
				}
	        }
	    } else {
			if (port > 0) {
				if (auctionWebserver.getListeningPort() != port) {
					auctionWebserver.stop();
					auctionWebserver = null;
					auctionWebserver = new AuctionWebserver(port);
			        try {
						auctionWebserver.start();
					} catch (IOException e) {
						auctionWebserver = null;
					}
				}
	        }
	    }
    }
    
    /**
     * Called by Bukkit when disabling.  Cancels all auctions and clears data.
     */
	public void onDisable() {
		if (auctionWebserver != null && auctionWebserver.isAlive()) {
			auctionWebserver.stop();
		}
		auctionWebserver = null;
		AuctionScope.cancelAllAuctions();
		getServer().getScheduler().cancelTask(queueTimer);
		plugin = null;
		auctionLog = null;
		sendMessage("plugin-disabled", console, null, false);
	}
	
	/**
	 * Prepares chat, prepending prefix and processing colors.
	 * 
	 * @param message message to prepare
	 * @param auctionScope the scope of the destination
	 * @return prepared message
	 */
    private static String chatPrep(String message, AuctionScope auctionScope) {
    	message = AuctionConfig.getLanguageString("chat-prefix", auctionScope) + message;
    	message = ChatColor.translateAlternateColorCodes('&', message);
    	return message;
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

    // Overrides onCommand from Plugin
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

    	// Make sure the decimalPlaces loaded correctly.
    	// Sometimes the econ loads after floAuction.
	    if (!loadedDecimalFromVault && econ.isEnabled()) {
	    	loadedDecimalFromVault = true;
			decimalPlaces = econ.fractionalDigits();
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
		String playerName = "";

    	if (sender instanceof Player) {
    		player = (Player) sender;
			playerName = player.getName();
			userScope = AuctionScope.getPlayerScope(player);
			if (userScope != null) {
				auction = userScope.getActiveAuction();
			}
    	} else {
			playerName = "*console*";
    	}

		if (
				cmd.getName().equalsIgnoreCase("auction") &&
				args.length > 0 &&
				args[0].equalsIgnoreCase("on")
		) {
			int index = voluntarilyDisabledUsers.indexOf(playerName);
			if (index != -1) {
				voluntarilyDisabledUsers.remove(index);
			}
			sendMessage("auction-enabled", sender, null, false);
			saveObject(voluntarilyDisabledUsers, "voluntarilyDisabledUsers.ser");
			return true;
		}
     
    	if (voluntarilyDisabledUsers.contains(playerName)) {
    		voluntarilyDisabledUsers.remove(voluntarilyDisabledUsers.indexOf(playerName));
			sendMessage("auction-fail-disabled", sender, null, false);
			voluntarilyDisabledUsers.add(playerName);
			saveObject(voluntarilyDisabledUsers, "voluntarilyDisabledUsers.ser");
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
    	    			sendMessage("no-permission", sender, null, false);
    	    			return true;
    				}
    		    	// Don't reload if any auctions are running.
    				if (AuctionScope.areAuctionsRunning()) {
						sendMessage("plugin-reloaded-fail-auctions-running", sender, null, false);
						return true;
    				}

    				loadConfig();
	    			sendMessage("plugin-reloaded", sender, null, false);
    				return true;
    			} else if (args[0].equalsIgnoreCase("resume")) {
			    	if (args.length == 1) {
						if (player != null && !perms.has(player, "auction.admin")) {
			    			sendMessage("no-permission", sender, null, false);
			    			return true;
						}
						// Resume globally:
						suspendAllAuctions = false;
		    			sendMessage("unsuspension-global", (Player) null, null, true);
						return true;
    		    	}
    		    	
    				if (player != null && !perms.has(player, "auction.admin")) {
    	    			sendMessage("no-permission", sender, null, false);
    	    			return true;
    				}

					if (!suspendedUsers.contains(args[1].toLowerCase())) {
		    			sendMessage("unsuspension-user-fail-not-suspended", sender, null, false);
		    			return true;
					}

					suspendedUsers.remove(args[1].toLowerCase());
					saveObject(suspendedUsers, "suspendedUsers.ser");
	    			sendMessage("unsuspension-user", getServer().getPlayer(args[1]), null, false);
	    			sendMessage("unsuspension-user-success", sender, null, false);
    				
    				return true;
    			} else if (args[0].equalsIgnoreCase("suspend")) {
    				if (player != null && !perms.has(player, "auction.admin")) {
    	    			sendMessage("no-permission", sender, null, false);
    	    			return true;
    				}
    				if (args.length > 1) {
    					// Suspend a player:
    					if (suspendedUsers.contains(args[1].toLowerCase())) {
    		    			sendMessage("suspension-user-fail-already-suspended", sender, null, false);
    		    			return true;
    					}
    					
    					Player playerToSuspend = getServer().getPlayer(args[1]);
    					
    					if (playerToSuspend == null || !playerToSuspend.isOnline()) {
    		    			sendMessage("suspension-user-fail-is-offline", sender, null, false);
    		    			return true;
    					}
    					
    					if (perms.has(playerToSuspend, "auction.admin")) {
    		    			sendMessage("suspension-user-fail-is-admin", sender, null, false);
    		    			return true;
    					}
    					
    					suspendedUsers.add(args[1].toLowerCase());
    					saveObject(suspendedUsers, "suspendedUsers.ser");
		    			sendMessage("suspension-user", playerToSuspend, null, false);
		    			sendMessage("suspension-user-success", sender, null, false);
    					
    					return true;
    				}
    				// Suspend globally:
    				suspendAllAuctions = true;
    				
    				AuctionScope.cancelAllAuctions();

	    			sendMessage("suspension-global", (Player) null, null, true);

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
    					sendMessage("suspension-global", sender, null, false);
    		    		return true;
    		    	}
    		    	if (player != null && suspendedUsers.contains(playerName.toLowerCase())) {
    					sendMessage("suspension-user", player, null, false);
    					return true;
    		    	}

    				// Start new auction!
    	    		if (player == null) {
    	    			sendMessage("auction-fail-console", sender, null, false);
    	    			return true;
    	    		}
    	    		if (!AuctionConfig.getBoolean("allow-gamemode-creative", userScope) && player.getGameMode() == GameMode.CREATIVE) {
    	    			sendMessage("auction-fail-gamemode-creative", sender, null, false);
    	    			return true;
    	    		}
    	    		if (userScope == null) {
    	    			sendMessage("auction-fail-no-scope", sender, null, false);
    	    			return true;
    	    		}
    	    			
    				if (!perms.has(player, "auction.start")) {
    	    			sendMessage("no-permission", sender, null, false);
    	    			return true;
    				}
    				
    				if (!AuctionConfig.getBoolean("allow-sealed-auctions", userScope) && !AuctionConfig.getBoolean("allow-unsealed-auctions", userScope)) {
    					sendMessage("auction-fail-no-auctions-allowed", sender, userScope, false);
    					return true;
    				}
    				
    				if (cmd.getName().equalsIgnoreCase("sealedauction") || cmd.getName().equalsIgnoreCase("sauc")) {
    					if (AuctionConfig.getBoolean("allow-sealed-auctions", userScope)) {
    						userScope.queueAuction(new Auction(this, player, args, userScope, true));
    					} else {
    						sendMessage("auction-fail-no-sealed-auctions", sender, userScope, false);
    					}
    				} else {
    					if (AuctionConfig.getBoolean("allow-unsealed-auctions", userScope)) {
    						userScope.queueAuction(new Auction(this, player, args, userScope, false));
    					} else {
    						userScope.queueAuction(new Auction(this, player, args, userScope, true));
    					}
    				}

					return true;
    			} else if (
        				args[0].equalsIgnoreCase("prep") || 
        				args[0].equalsIgnoreCase("p")
    			) {
    				// Save a users individual starting default values.
    	    		if (player == null) {
    	    			sendMessage("auction-fail-console", sender, null, false);
    	    			return true;
    	    		}
    				if (!perms.has(player, "auction.start")) {
    	    			sendMessage("no-permission", sender, null, false);
    	    			return true;
    				}
    				
    				// The function returns null and sends error on failure.
    				String[] mergedArgs = functions.mergeInputArgs(playerName, args, true);
    				
    				if (mergedArgs != null) {
						floAuction.userSavedInputArgs.put(playerName, mergedArgs);
						floAuction.saveObject(floAuction.userSavedInputArgs, "userSavedInputArgs.ser");
						if (AuctionConfig.getBoolean("allow-buynow", userScope)) {
							sendMessage("prep-save-success-with-buynow", sender, null, false);
						} else {
							sendMessage("prep-save-success", sender, null, false);
						}
    				}

					return true;
    			} else if (args[0].equalsIgnoreCase("cancel") || args[0].equalsIgnoreCase("c")) {
    				if (userScope.getActiveAuction() == null && userScope.getAuctionQueueLength() == 0) {
    					sendMessage("auction-fail-no-auction-exists", sender, userScope, false);
    					return true;
    				}
    				
    				ArrayList<Auction> auctionQueue = userScope.getAuctionQueue();
    				for(int i = 0; i < auctionQueue.size(); i++){
    					if (auctionQueue.get(i).getOwner().equalsIgnoreCase(playerName)) {
    						auctionQueue.remove(i);
    						sendMessage("auction-cancel-queued", player, userScope, false);
    						return true;
    					}
    				}
    				
    				if (auction == null) {
    					sendMessage("auction-fail-no-auction-exists", sender, userScope, false);
    					return true;
    				}
    				
					if (player == null || player.getName().equalsIgnoreCase(auction.getOwner()) || perms.has(player, "auction.admin")) {
						if (AuctionConfig.getInt("cancel-prevention-seconds", userScope) > auction.getRemainingTime() || AuctionConfig.getDouble("cancel-prevention-percent", userScope) > (double)auction.getRemainingTime() / (double)auction.getTotalTime() * 100D) {
	    					sendMessage("auction-fail-cancel-prevention", player, userScope, false);
						} else {
	    					auction.cancel();
						}
					} else {
    					sendMessage("auction-fail-not-owner-cancel", player, userScope, false);
					}
    				return true;
    			} else if (args[0].equalsIgnoreCase("confiscate") || args[0].equalsIgnoreCase("impound")) {
    				if (auction == null) {
    					sendMessage("auction-fail-no-auction-exists", sender, userScope, false);
    					return true;
    				}
    				
    				if (player == null) {
    					sendMessage("confiscation-fail-console", player, userScope, false);
    					return true;
    				}
					if (!perms.has(player, "auction.admin")) {
    					sendMessage("no-permission", player, userScope, false);
    					return true;
					}
					if (playerName.equalsIgnoreCase(auction.getOwner())) {
    					sendMessage("confiscation-fail-self", player, userScope, false);
    					return true;
					}
					auction.confiscate(player);
    				return true;
    			} else if (args[0].equalsIgnoreCase("end") || args[0].equalsIgnoreCase("e")) {
    				if (auction == null) {
    					sendMessage("auction-fail-no-auction-exists", player, userScope, false);
        				return true;
    				}
    				if (!AuctionConfig.getBoolean("allow-early-end", userScope)) {
    					sendMessage("auction-fail-no-early-end", player, userScope, false);
        				return true;
    				}
					if (player.getName().equalsIgnoreCase(auction.getOwner())) {
    					auction.end();
					} else {
    					sendMessage("auction-fail-not-owner-end", player, userScope, false);
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
    				if (voluntarilyDisabledUsers.indexOf(playerName) == -1) {
    					sendMessage("auction-disabled", sender, null, false);
    					voluntarilyDisabledUsers.add(playerName);
    					saveObject(voluntarilyDisabledUsers, "voluntarilyDisabledUsers.ser");
    				}
    				return true;
    			} else if (args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("i")) {
    				if (auction == null) {
    					sendMessage("auction-info-no-auction", player, userScope, false);
    					return true;
    				}
					auction.info(sender, false);
    				return true;
    			} else if (args[0].equalsIgnoreCase("queue") || args[0].equalsIgnoreCase("q")) {
    				ArrayList<Auction> auctionQueue = userScope.getAuctionQueue();
    				if (auctionQueue.isEmpty()) {
    					sendMessage("auction-queue-status-not-in-queue", player, userScope, false);
    					return true;
    				}
    				for(int i = 0; i < auctionQueue.size(); i++){
    					if (auctionQueue.get(i).getOwner().equalsIgnoreCase(playerName)) {
        					sendMessage("auction-queue-status-in-queue", player, userScope, false);
    						return true;
    					}
    				}

					sendMessage("auction-queue-status-not-in-queue", player, userScope, false);
    				return true;
    			}
    		}
			sendMessage("auction-help", sender, userScope, false);
    		return true;
    	} else if (cmd.getName().equalsIgnoreCase("bid")) {
        	if (suspendAllAuctions) {
    			sendMessage("suspension-global", sender, null, false);
        		return true;
        	}
        	if (player != null && suspendedUsers.contains(playerName.toLowerCase())) {
    			sendMessage("suspension-user", player, null, false);
    			return true;
        	}

    		if (player == null) {
    			sendMessage("bid-fail-console", console, null, false);
    			return true;
    		} 
    		if (!AuctionConfig.getBoolean("allow-gamemode-creative", userScope) && player.getGameMode().equals(GameMode.CREATIVE)) {
    			sendMessage("bid-fail-gamemode-creative", sender, null, false);
    			return true;
    		}
			if (!perms.has(player, "auction.bid")) {
    			sendMessage("no-permission", sender, null, false);
    			return true;
			}
    		if (auction == null) {
    			sendMessage("bid-fail-no-auction", player, null, false);
    			return true;
    		}
    		auction.Bid(player, args);
    		return true;
    	}
    	return false;
    }
    
    /**
     * Sends a message to a player or scope.
     * 
     * @param messageKey key to message in language.yml
     * @param player focused player
     * @param auctionScope focused scope
     * @param fullBroadcast whether to broadcast or send to player
     */
    public static void sendMessage(String messageKey, CommandSender player, AuctionScope auctionScope, boolean fullBroadcast) {
    	if (messageKey == null) {
    		return;
    	}
    	List<String> messageKeys = new ArrayList<String>();
    	messageKeys.add(messageKey);
    	sendMessage(messageKeys, player, auctionScope, fullBroadcast, "-");
    }

    /**
     * Sends a message to a player or scope.
     * 
     * @param messageKeys keys to message in language.yml
     * @param player focused player
     * @param auctionScope focused scope
     * @param fullBroadcast whether to broadcast or send to player
     */
    public static void sendMessage(List<String> messageKeys, CommandSender player, AuctionScope auctionScope, boolean fullBroadcast) {
    	sendMessage(messageKeys, player, auctionScope, fullBroadcast, "-");
    }

    /*private static void sendMessage(String messageKey, CommandSender player, AuctionScope auctionScope, boolean fullBroadcast, String fireworkAspect) {
    	if (messageKey == null) {
    		return;
    	}
    	List<String> messageKeys = new ArrayList<String>();
    	messageKeys.add(messageKey);
    	sendMessage(messageKeys, player, auctionScope, fullBroadcast, fireworkAspect);
    }*/

    /**
     * Sends a message to a player or scope.
     * 
     * @param messageKeys keys to message in language.yml
     * @param player focused player
     * @param auctionScope focused scope
     * @param fullBroadcast whether to broadcast or send to player
     * @param fireworkAspect aspects of firework to send
     */
    private static void sendMessage(List<String> messageKeys, CommandSender player, AuctionScope auctionScope, boolean fullBroadcast, String fireworkAspect) {

    	String playerName = null;
    	Auction auction = null;
    	ArrayList<Auction> auctionQueue = null;
    	
    	if (auctionScope != null) {
    		auction = auctionScope.getActiveAuction();
    	}
    	
    	if (player != null) {
	    	if (player instanceof Player) {
		    	if (!fullBroadcast && voluntarilyDisabledUsers.indexOf(player.getName()) != -1) {
		    		// Don't send this user any messages.
		    		return;
				}
		    	playerName = player.getName();
	    	} else {
		    	if (!fullBroadcast && voluntarilyDisabledUsers.indexOf("*console*") != -1) {
		    		// Don't send console any messages.
		    		return;
				}
		    	playerName = "*console*";
	    	}
    	}
    	

    	if (messageKeys == null || messageKeys.size() == 0) {
    		return;
    	}
    	
    	String owner = null;
    	String ownerDisplay = null;
    	String quantity = null;
    	String lotType = null;
    	String startingBid = null;
    	String minBidIncrement = null;
    	String buyNow = null;
    	String currentBidder = null;
    	String currentBid = null;
    	String currentMaxBid = null;
    	String timeRemaining = null;
    	String durabilityRemaining = null;
    	String endAuctionTax = null;
    	String startAuctionTax = null;
    	String bookAuthor = null;
    	String bookTitle = null;
    	String displayName = null;
    	String rocketPower = null;
    	ItemStack typeLot = null;
    	String queuePostition = "-";
    	String queueSize = "-";

    	if (auctionScope != null) {
	    	auctionQueue = auctionScope.getAuctionQueue();
			for(int i = 0; i < auctionQueue.size(); i++){
				if (auctionQueue.get(i).getOwner().equalsIgnoreCase(playerName)) {
					queuePostition = Integer.toString(i + 1);
				}
			}
			queueSize = Integer.toString(auctionQueue.size());
    	}

    	if (auction != null) {
    		typeLot = auction.getLotType();
    		auctionScope = auction.getScope();
    		
    		if (auction.getOwner() != null) owner = auction.getOwner();
    		
    		Player ownerPlayer = Bukkit.getPlayer(owner);
    		if (ownerPlayer != null) {
    			ownerDisplay = ownerPlayer.getDisplayName();
    		} else {
    			ownerDisplay = owner;
    		}
    		
    		quantity = Integer.toString(auction.getLotQuantity());
    		lotType = items.getItemName(typeLot);
    		if (auction.getStartingBid() == 0) {
	    		startingBid = functions.formatAmount(auction.getMinBidIncrement());
    		} else {
	    		startingBid = functions.formatAmount(auction.getStartingBid());
    		}
    		minBidIncrement = functions.formatAmount(auction.getMinBidIncrement());
    		buyNow = functions.formatAmount(auction.getBuyNow());
			
			timeRemaining = functions.formatTime(auction.getRemainingTime(), auctionScope);
    		startAuctionTax = functions.formatAmount(auction.extractedPreTax);
			endAuctionTax = functions.formatAmount(auction.extractedPostTax);
			if (auction.getCurrentBid() != null) {
				currentBidder = auction.getCurrentBid().getBidder();
				currentBid = functions.formatAmount(auction.getCurrentBid().getBidAmount());
				currentMaxBid = functions.formatAmount(auction.getCurrentBid().getMaxBidAmount());
			} else {
				currentBidder = "noone";
				currentBid = startingBid;
				currentMaxBid = startingBid;
			}
        	durabilityRemaining = "-";
			if (typeLot != null) {
				if (typeLot.getType().getMaxDurability() > 0) {
			        DecimalFormat decimalFormat = new DecimalFormat("#%");
			        durabilityRemaining = decimalFormat.format((1 - ((double) typeLot.getDurability() / (double) typeLot.getType().getMaxDurability())));
				}
			}
        	bookAuthor = items.getBookAuthor(typeLot);
        	if (bookAuthor == null) bookAuthor = "";
        	bookTitle = items.getBookTitle(typeLot);
        	if (bookTitle == null) bookTitle = "";
        	displayName = items.getDisplayName(typeLot);
        	if (displayName == null) displayName = "";
        	
        	if (displayName.isEmpty()) {
        		displayName = lotType;
        	} else {
        		displayName = ChatColor.translateAlternateColorCodes('&', AuctionConfig.getLanguageString("display-name-prefix", auctionScope)) + displayName + ChatColor.translateAlternateColorCodes('&', "&r");
        	}
        	
			if (typeLot != null && typeLot.getType() == Material.FIREWORK) {
				Integer power = items.getFireworkPower(typeLot);
				if (power != null) {
					rocketPower = power.toString();
				}
			}
			if (rocketPower == null) rocketPower = "-";
    	} else {
        	owner = "-";
        	ownerDisplay = "-";
        	quantity = "-";
        	lotType = "-";
        	startingBid = "-";
        	minBidIncrement = "-";
        	buyNow = "-";
        	currentBidder = "-";
        	currentBid = "-";
        	currentMaxBid = "-";
        	timeRemaining = "-";
        	durabilityRemaining = "-";
    		startAuctionTax = "-";
        	endAuctionTax = "-";
        	bookAuthor = "-";
        	bookTitle = "-";
        	displayName = "-";
        	rocketPower = "-";
    	}
    	
    	for (int l = 0; l < messageKeys.size(); l++) {
    		String messageKey = messageKeys.get(l);
        	if (messageKey == null) {
        		continue;
        	}
    		
	    	List<String> messageList = AuctionConfig.getLanguageStringList(messageKey, auctionScope);
	    	
	    	String originalMessage = null;
	    	if (messageList == null || messageList.size() == 0) {
	    		originalMessage = AuctionConfig.getLanguageString(messageKey, auctionScope);
	    		
	    		if (originalMessage == null || originalMessage.length() == 0) {
	        		continue;
	    		} else {
	        		messageList = Arrays.asList(originalMessage.split("(\r?\n|\r)"));
	    		}
	    	}
	    	
	    	for (Iterator<String> i = messageList.iterator(); i.hasNext(); ) {
	    		String messageListItem = i.next();
	    		String message = chatPrep(messageListItem, auctionScope);
		
				message = message.replace("%o", owner);
				message = message.replace("%O", ownerDisplay);

				message = message.replace("%q", quantity);
				message = message.replace("%i", displayName);
				message = message.replace("%s", startingBid);
				message = message.replace("%n", minBidIncrement);
				message = message.replace("%f", buyNow);
				message = message.replace("%b", currentBid);
				message = message.replace("%B", currentBidder);
				message = message.replace("%h", currentMaxBid);
				message = message.replace("%t", timeRemaining);
				message = message.replace("%D", durabilityRemaining);
				message = message.replace("%x", startAuctionTax);
				message = message.replace("%X", endAuctionTax);
				message = message.replace("%y", bookAuthor);
				message = message.replace("%Y", bookTitle);
				message = message.replace("%d", lotType);
				message = message.replace("%r", rocketPower);
				message = message.replace("%k", queueSize);
				message = message.replace("%Q", queuePostition);
				message = message.replace("%c", floAuction.econ.currencyNameSingular());
				message = message.replace("%C", floAuction.econ.currencyNamePlural());
				
				String[] defaultStartArgs = functions.mergeInputArgs(playerName, new String[] {}, false);
				if (defaultStartArgs[0].equalsIgnoreCase("this") || defaultStartArgs[0].equalsIgnoreCase("hand")) {
					message = message.replace("%U", AuctionConfig.getLanguageString("prep-amount-in-hand", auctionScope));
				} else if (defaultStartArgs[0].equalsIgnoreCase("all")) {
					message = message.replace("%U", AuctionConfig.getLanguageString("prep-all-of-this-kind", auctionScope));
				} else {
					message = message.replace("%U", AuctionConfig.getLanguageString("prep-qty-of-this-kind", auctionScope));
				}
				message = message.replace("%u", defaultStartArgs[0]);
				message = message.replace("%v", defaultStartArgs[1]);
				message = message.replace("%V", functions.formatAmount(Double.parseDouble(defaultStartArgs[1])));
				message = message.replace("%w", defaultStartArgs[2]);
				message = message.replace("%W", functions.formatAmount(Double.parseDouble(defaultStartArgs[2])));
				message = message.replace("%z", defaultStartArgs[3]);
				message = message.replace("%Z", functions.formatTime(Integer.parseInt(defaultStartArgs[3]), auctionScope));
				message = message.replace("%g", defaultStartArgs[4]);
				message = message.replace("%G", functions.formatAmount(Double.parseDouble(defaultStartArgs[4])));
				
				
				originalMessage = message;
				
				// Firework charges:
				if (originalMessage.contains("%A")) {
					if (auction != null) {
						FireworkEffect[] payloads = items.getFireworkEffects(typeLot);
						if (payloads != null && payloads.length > 0) {
							for (int j = 0; j < payloads.length; j++) {
								FireworkEffect payload = payloads[j];
								// %A lists all aspects of the payload
								
								String payloadAspects = "";
								String payloadSeparator = ChatColor.translateAlternateColorCodes('&', AuctionConfig.getLanguageString("auction-info-payload-separator", auctionScope));
								
								Type type = payload.getType();
								if (type != null) {
									if (!payloadAspects.isEmpty()) payloadAspects += payloadSeparator;
									if (AuctionConfig.getLanguageString("firework-shapes." + type.toString(), auctionScope) == null) {
										payloadAspects += type.toString();
									} else {
										payloadAspects += AuctionConfig.getLanguageString("firework-shapes." + type.toString(), auctionScope);
									}
								}
								List<Color> colors = payload.getColors();
								for (int k = 0; k < colors.size(); k++) {
									if (!payloadAspects.isEmpty()) payloadAspects += payloadSeparator;
									Color color = colors.get(k);
									String colorRGB = color.toString().replace("Color:[rgb0x", "").replace("]", "");
									if (AuctionConfig.getLanguageString("firework-colors." + colorRGB, auctionScope) == null) {
										payloadAspects += "#" + colorRGB;
									} else {
										payloadAspects += AuctionConfig.getLanguageString("firework-colors." + colorRGB, auctionScope);
									}
								}
								if (payload.hasFlicker()) {
									if (!payloadAspects.isEmpty()) payloadAspects += payloadSeparator;
									payloadAspects += AuctionConfig.getLanguageString("firework-twinkle", auctionScope);
								}
								if (payload.hasTrail()) {
									if (!payloadAspects.isEmpty()) payloadAspects += payloadSeparator;
									payloadAspects += AuctionConfig.getLanguageString("firework-trail", auctionScope);
								}
								message = originalMessage.replace("%A", payloadAspects);
				            	if (fullBroadcast) {
				            		broadcastMessage(message, auctionScope);
				            	} else {
				        	    	player.sendMessage(message);
				            	}
				            	log(player, message);
							}
							continue;
						} else {
							message = message.replace("%A", "-");
						}
					} else {
						message = message.replace("%A", "-");
					}
				}
				
				
				// Enchantments:
				if (originalMessage.contains("%F")) {
					if (auction != null) {
		        		Map<Enchantment, Integer> enchantments = typeLot.getEnchantments();
		        		if (enchantments == null || enchantments.size() == 0) {
		        			enchantments = items.getStoredEnchantments(typeLot);
		        		}
						String enchantmentList = "";
						String enchantmentSeparator = ChatColor.translateAlternateColorCodes('&', AuctionConfig.getLanguageString("auction-info-enchantment-separator", auctionScope));
		        		for (Entry<Enchantment, Integer> enchantment : enchantments.entrySet()) {
		        			if (!enchantmentList.isEmpty()) enchantmentList += enchantmentSeparator;
		        			enchantmentList += items.getEnchantmentName(enchantment);
		        		}
		        		if (enchantmentList.isEmpty()) enchantmentList = ChatColor.translateAlternateColorCodes('&', AuctionConfig.getLanguageString("auction-info-enchantment-none", auctionScope));
		        		message = message.replace("%F", enchantmentList);
					} else {
		        		message = message.replace("%F", "-");
					}
				}
				if (originalMessage.contains("%E")) {
	    			if (auction != null) {
		        		Map<Enchantment, Integer> enchantments = typeLot.getEnchantments();
		        		if (enchantments == null || enchantments.size() == 0) {
		        			enchantments = items.getStoredEnchantments(typeLot);
		        		}
		        		for (Entry<Enchantment, Integer> enchantment : enchantments.entrySet()) {
		        			message = originalMessage.replace("%E", items.getEnchantmentName(enchantment));
		        			
			            	if (fullBroadcast) {
			            		broadcastMessage(message, auctionScope);
			            	} else {
			        	    	player.sendMessage(message);
			            	}
			            	log(player, message);
		        		}
	    			}
	    			continue;
				}
				
				if (originalMessage.contains("%L")) {
	    			if (auction != null) {
	    				String[] lore = items.getLore(typeLot);
		        		for (int j = 0; j < lore.length; j++) {
		        			message = originalMessage.replace("%L", lore[j]);
		        			
			            	if (fullBroadcast) {
			            		broadcastMessage(message, auctionScope);
			            	} else {
			        	    	player.sendMessage(message);
			            	}
			            	log(player, message);
		        		}
	    			}
	    			continue;
				}
	
				if (fullBroadcast) {
		    		broadcastMessage(message, auctionScope);
		    	} else if (player != null) {
			    	player.sendMessage(message);
		    	}
		    	log(player, message);
	    	}
    	}
    	
    }
    
    /**
     * Broadcast a message to everyone in an auctionscope.
     * 
     * @param message message to send
     * @param auctionScope scope to send it to
     */
    private static void broadcastMessage(String message, AuctionScope auctionScope) {
    	Player[] onlinePlayers = Bukkit.getOnlinePlayers();
    	
    	for (Player player : onlinePlayers) {
        	if (voluntarilyDisabledUsers.contains(player.getName())) continue;
    		if (auctionScope != null && !auctionScope.equals(AuctionScope.getPlayerScope(player))) continue;
    		player.sendMessage(message);
    	}
    	
    	if (auctionScope == null && voluntarilyDisabledUsers.indexOf("*console*") == -1) {
			console.sendMessage(message);
		}
    }
    
    /**
     * Log data to the floAuction log file if logging is enabled.
     * 
     * @param sender who is initiating the logged event
     * @param message message to save
     */
    private static void log(CommandSender sender, String message) {
    	Player player = null;
    	AuctionScope playerScope = null;
    	if (sender instanceof Player) {
    		player = (Player) sender;
    		playerScope = AuctionScope.getPlayerScope(player);
    	}
    	if (AuctionConfig.getBoolean("log-auctions", playerScope)) {
    		String playerName = null;
    		String scopeId = null;
    		
			BufferedWriter out = null;
			try {
		    	if (!auctionLog.exists()) {
					auctionLog.createNewFile();
					auctionLog.setWritable(true);
		    	}
		    	
				out = new BufferedWriter(new FileWriter(auctionLog.getAbsolutePath(), true));

				if (player == null && sender != null) {
					playerName = "CONSOLE";
				} else if (player == null) {
					playerName = "BROADCAST";
				} else {
					playerName = player.getName();
				}
				
				if (playerScope == null) {
					scopeId = "NOSCOPE";
				} else {
					scopeId = playerScope.getScopeId();
				}
				
				// TODO: Add scope name, yay!
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
     * Sends a message to a player or scope.
     * 
     * @param messageKeys keys to message in language.yml
     * @param player focused player
     * @param auctionScope focused scope
     */
	public static void sendMessage(String messageKey, String playerName, AuctionScope auctionScope) {
		if (playerName == null) {
			sendMessage(messageKey, (CommandSender) null, auctionScope, true);
		} else {
			sendMessage(messageKey, Bukkit.getPlayer(playerName), auctionScope, false);
		}
		
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
}

