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

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Material;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.ConfigurationSection;
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
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import com.flobi.utility.functions;
import com.flobi.utility.items;

public class floAuction extends JavaPlugin {
	private static final Logger log = Logger.getLogger("Minecraft");
	public static Auction publicAuction;

	// Got to figure out a better way to store these:
	public static long defaultStartingBid = 0;
	public static long defaultBidIncrement = 100;
	public static int defaultAuctionTime = 60;
	public static long maxStartingBid = 10000;
	public static long maxBuyNow = 10000;
	public static long minIncrement = 1;
	public static long maxIncrement = 100;
	public static int maxTime = 60;
	public static int minTime = 15;
	public static int maxAuctionQueueLength = 2;
	public static int minAuctionIntervalSecs = 10;
	public static boolean allowBidOnOwn = false;
	public static boolean useOldBidLogic = false;
	public static boolean logAuctions = false;
	public static boolean allowEarlyEnd = false;
	public static int decimalPlaces = 2;
	public static String decimalRegex = "^[0-9]{0,13}(\\.[0-9]{1," + decimalPlaces + "})?$";
	public static boolean loadedDecimalFromVault = false;
	public static boolean allowCreativeMode = false;
	public static boolean allowDamagedItems = false;
	private static File auctionLog = null;
	private static long lastAuctionDestroyTime = 0;
	private static boolean suspendAllAuctions = false;
	public static boolean allowMaxBids = true;
	public static boolean allowGamemodeChange = false;
	public static boolean allowWorldChange = false;
	public static List<String> bannedItems = new ArrayList<String>();
	public static Map<String, String> taxedItems = new HashMap<String, String>();
	public static double taxPerAuction = 0;
	public static double taxPercentage = 0;
	public static String taxDestinationUser = "";
	public static Location currentBidPlayerLocation;
	public static GameMode currentBidPlayerGamemode;
	public static Location currentAuctionOwnerLocation;
	public static GameMode currentAuctionOwnerGamemode;
	public static int cancelPreventionSeconds = 15;
	public static double cancelPreventionPercent = 50;
    public static boolean antiSnipe = false;
	public static int antiSnipePreventionSeconds = 15;
	public static int antiSnipeExtensionSeconds = 15;
	public static boolean useWhatIsIt = true;
	
	public static boolean allowSealedAuctions = true;
	public static boolean allowUnsealedAuctions = true;
	public static boolean broadCastBidUpdates = true;
	public static boolean allowAutoBid = true;
	public static boolean suppressCountdown = true;
	public static boolean suppressAuctionStartInfo = true;
	public static boolean allowRenamedItems = true;
	public static boolean allowBuyNow = true;
	public static boolean expireBuyNowOnFirstBid = false;
	public static List<String> disabledCommands = new ArrayList<String>();
	
	public static List<Participant> auctionParticipants = new ArrayList<Participant>();
	public static List<String> bannedLore = new ArrayList<String>();

	public static Map<String, String[]> userSavedInputArgs = new HashMap<String, String[]>();

	// Config files info.
	private static File configFile = null;
	private static InputStream defConfigStream;
	public static FileConfiguration config = null;
	private static File textConfigFile = null;
	private static InputStream defTextConfigStream;
	public static FileConfiguration textConfig = null;
	private static YamlConfiguration defConfig = null;
	private static YamlConfiguration defTextConfig = null;
	private static File dataFolder;
	private static ConsoleCommandSender console;
	public static Server server;
	public static int queueTimer;
	public static ArrayList<Auction> auctionQueue = new ArrayList<Auction>();
	
	public static ArrayList<AuctionLot> orphanLots = new ArrayList<AuctionLot>();
	public static ArrayList<String> voluntarilyDisabledUsers = new ArrayList<String>();
	public static ArrayList<String> suspendedUsers = new ArrayList<String>();
	
	
	public static void saveObject(Object arraylist, String filename) {
    	File saveFile = new File(dataFolder, filename);
    	
    	try {
			//use buffering
    		if (saveFile.exists()) saveFile.delete();
    		FileOutputStream file = new FileOutputStream(saveFile.getAbsolutePath());
			OutputStream buffer = new BufferedOutputStream(file);
			ObjectOutput output = new ObjectOutputStream(buffer);
			try {
				output.writeObject(arraylist);
			}
			finally {
				output.close();
			}
  	    }  
  	    catch(IOException ex){
    		return;
  	    }
	}
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

	public void onEnable() {
		server = getServer();
		console = server.getConsoleSender();
		dataFolder = getDataFolder();
		defConfigStream = getResource("config.yml");
		defTextConfigStream = getResource("language.yml");
		final Plugin plugin = this;

		setupEconomy();
        setupPermissions();
        setupChat();

        loadConfig();
		if (server.getPluginManager().getPlugin("WhatIsIt") == null) {
			if (config.getBoolean("allow-inferior-item-name-logic")) {
				log.log(Level.SEVERE, chatPrepClean(textConfig.getString("whatisit-recommended")));
				useWhatIsIt = false;
			} else {
				log.log(Level.SEVERE, chatPrepClean(textConfig.getString("no-whatisit")));
				server.getPluginManager().disablePlugin(this);
	            return;
			}
		} else {
			useWhatIsIt = true;
		}
		if (econ == null) {
			log.log(Level.SEVERE, chatPrepClean(textConfig.getString("no-economy")));
			server.getPluginManager().disablePlugin(this);
            return;
		}
        
        server.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void playerJoin(PlayerJoinEvent event) {
        	    floAuction.killOrphan(event.getPlayer());
            }
            @EventHandler
            public void onPlayerChangedWorld(PlayerChangedWorldEvent event){
            	if (allowWorldChange || publicAuction == null) return;
            	
                // Get player objects
                final Player player = event.getPlayer();
                if (publicAuction.getOwner().equalsIgnoreCase(player.getName()) && !player.getLocation().getWorld().equals(currentAuctionOwnerLocation.getWorld())) {
                	// This is running as a timer because MultiInv is using HIGHEST priority and 
                	// there's no way to send a cancel to it, so we have to go after the fact and
                	// remove the user.
                	getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                		public void run() {
    	                	player.teleport(currentAuctionOwnerLocation, TeleportCause.PLUGIN);
                		}
                	}, 1L);
                	sendMessage("worldchange-fail-auction-owner", player, publicAuction, false);
                } else if (publicAuction.getCurrentBid() != null && publicAuction.getCurrentBid().getBidder().equalsIgnoreCase(player.getName())
                		 && !player.getLocation().getWorld().equals(currentBidPlayerLocation.getWorld())
                		) {
                	// This is running as a timer because MultiInv is using HIGHEST priority and 
                	// there's no way to send a cancel to it, so we have to go after the fact and
                	// remove the user.
                	getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                		public void run() {
                        	player.teleport(currentBidPlayerLocation, TeleportCause.PLUGIN);
                		}
                	}, 1L);
                	sendMessage("worldchange-fail-auction-bidder", player, publicAuction, false);
                }
            }
            @EventHandler
            public void onPlayerChangedGameMode(PlayerGameModeChangeEvent event){
            	if (event.isCancelled()) return;
            	if (allowGamemodeChange || publicAuction == null) return;
            	
                // Get player objects
                Player player = event.getPlayer();
                
                if (publicAuction.getOwner().equalsIgnoreCase(player.getName())) {
                	event.setCancelled(true);
                	sendMessage("gamemodechange-fail-auction-owner", player, publicAuction, false);
                } else if (publicAuction.getCurrentBid() != null && publicAuction.getCurrentBid().getBidder().equalsIgnoreCase(player.getName())) {
                	event.setCancelled(true);
                	sendMessage("gamemodechange-fail-auction-bidder", player, publicAuction, false);
                }
            }
            @EventHandler(priority = EventPriority.LOWEST)
            public void onPlayerPreprocessCommand(PlayerCommandPreprocessEvent event){
            	if (event.isCancelled()) return;
            	if (publicAuction == null && auctionQueue.size() == 0) return;
            	if (event.getPlayer() == null) return;
            	if (event.getMessage().isEmpty()) return;
            	
            	boolean isDisabledCommand = false;
        		for (int i = 0; i < floAuction.disabledCommands.size(); i++) {
        			String disabledCommand = floAuction.disabledCommands.get(i);
        			if (disabledCommand.isEmpty()) continue;
        			if (event.getMessage().toLowerCase().startsWith(disabledCommand.toLowerCase())) {
        				isDisabledCommand = true;
        				break;
        			}
        		}
        		if (!isDisabledCommand) return;

    			if (Participant.isParticipating(event.getPlayer().getName())) {
	            	event.setCancelled(true);
	            	sendMessage("disabled-command", event.getPlayer(), publicAuction, false);
    				return;
    			}
            }
        	@EventHandler()
        	public void onPlayerMove(PlayerMoveEvent event) {
        		if (event.isCancelled()) return;
        		Participant.forceLocation(event.getPlayer().getName());
        	}
        }, this);
		
		queueTimer = getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
		    public void run() {
		    	checkAuctionQueue();
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
		if (configFile == null) {
	    	configFile = new File(dataFolder, "config.yml");
	    }
		if (auctionLog == null) {
	    	auctionLog = new File(dataFolder, "auctions.log");
		}
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
	    
	    if (textConfigFile == null) {
	    	textConfigFile = new File(dataFolder, "language.yml");
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
	    
	    logAuctions = config.getBoolean("log-auctions");
	    
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

		defaultStartingBid = functions.getSafeMoney(config.getDouble("default-starting-bid"));
		defaultBidIncrement = functions.getSafeMoney(config.getDouble("default-bid-increment"));
		defaultAuctionTime = config.getInt("default-auction-time");
		maxStartingBid = functions.getSafeMoney(config.getDouble("max-starting-bid"));
		maxBuyNow = functions.getSafeMoney(config.getDouble("max-buynow"));
		minIncrement = functions.getSafeMoney(config.getDouble("min-bid-increment"));
		maxIncrement = functions.getSafeMoney(config.getDouble("max-bid-increment"));
		maxTime = config.getInt("max-auction-time");
		minTime = config.getInt("min-auction-time");
		maxAuctionQueueLength = config.getInt("max-auction-queue-length");
		minAuctionIntervalSecs = config.getInt("min-auction-interval-secs");
		allowBidOnOwn = config.getBoolean("allow-bid-on-own-auction");
		useOldBidLogic = config.getBoolean("use-old-bid-logic");
		allowEarlyEnd = config.getBoolean("allow-early-end");
		allowCreativeMode = config.getBoolean("allow-gamemode-creative");
		allowDamagedItems = config.getBoolean("allow-damaged-items");
		bannedItems = config.getStringList("banned-items");
		taxPerAuction = config.getDouble("auction-start-tax");
		taxPercentage = config.getDouble("auction-end-tax-percent");
		allowMaxBids = config.getBoolean("allow-max-bids");
		allowGamemodeChange = config.getBoolean("allow-gamemode-change");
		allowWorldChange = config.getBoolean("allow-world-change");
		taxDestinationUser = config.getString("deposit-tax-to-user");
		cancelPreventionSeconds = config.getInt("cancel-prevention-seconds");
		cancelPreventionPercent = config.getDouble("cancel-prevention-percent");
        antiSnipe = config.getBoolean("anti-snipe");
        antiSnipePreventionSeconds = config.getInt("anti-snipe-prevention-seconds");
        antiSnipeExtensionSeconds = config.getInt("anti-snipe-extension-seconds");
		disabledCommands = config.getStringList("disabled-commands");
		
        allowSealedAuctions = config.getBoolean("allow-sealed-auctions");
        if (allowSealedAuctions) {
        	allowUnsealedAuctions = config.getBoolean("allow-unsealed-auctions");
        } else {
        	allowUnsealedAuctions = true;
        }
        
        allowRenamedItems = config.getBoolean("allow-renamed-items");
        allowBuyNow = config.getBoolean("allow-buynow");
        expireBuyNowOnFirstBid = config.getBoolean("expire-buynow-at-first-bid");

        broadCastBidUpdates = config.getBoolean("broadcast-bid-updates");
        allowAutoBid = config.getBoolean("allow-auto-bid");
        suppressCountdown = config.getBoolean("suppress-countdown");
        suppressAuctionStartInfo = config.getBoolean("suppress-auction-start-info");
        

		ConfigurationSection taxedItemsSection = config.getConfigurationSection("taxed-items");
		taxedItems = new HashMap<String, String>();
		if (taxedItemsSection != null) {
			for (String itemCode : taxedItemsSection.getKeys(false)) {
				taxedItems.put(itemCode, taxedItemsSection.getString(itemCode));
			}
		}
		
		Participant.setAuctionHouseBox(
					config.getString("auctionhouse-world"), 
					config.getDouble("auctionhouse-min-x"), 
					config.getDouble("auctionhouse-min-y"), 
					config.getDouble("auctionhouse-min-z"), 
					config.getDouble("auctionhouse-max-x"), 
					config.getDouble("auctionhouse-max-y"), 
					config.getDouble("auctionhouse-max-z"));
        
		bannedLore = config.getStringList("banned-lore");
		
        // Update all values to include defaults which may be new.
		
		FileConfiguration cleanConfig = new YamlConfiguration();
		Map<String, Object> configValues = config.getDefaults().getValues(true);
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
	    
	    if (maxStartingBid == 0) {
	    	maxStartingBid = 100000000000000000L;
	    }
	    
	    if (maxBuyNow == 0) {
	    	maxBuyNow = 100000000000000000L;
	    }
    }
	public void onDisable() {
		if (publicAuction != null) publicAuction.cancel();
		getServer().getScheduler().cancelTask(queueTimer);
		sendMessage("plugin-disabled", console, null, false);
	}
	public void detachAuction(Auction auction) {
		publicAuction = null;
		lastAuctionDestroyTime = System.currentTimeMillis();
		checkAuctionQueue();
	}
	/**
	 * Prepares chat, prepending prefix and processing colors.
	 * 
	 * @param String message to prepare
	 * @return String prepared message
	 */
    private static String chatPrep(String message) {
    	message = textConfig.getString("chat-prefix") + message;
    	message = ChatColor.translateAlternateColorCodes('&', message);
    	return message;
    }
    private static String chatPrepClean(String message) {
    	message = textConfig.getString("chat-prefix") + message;
    	message = ChatColor.translateAlternateColorCodes('&', message);
    	message = ChatColor.stripColor(message);
    	return message;
    }
    
    public void queueAuction(Auction auctionToQueue, Player player, Auction currentAuction) {
		String playerName = player.getName();

		if (currentAuction == null) {
			// Queuing because of interval not yet timed out.
			// Allow a queue of 1 to override if 0 for this condition.
	    	if (Math.max(maxAuctionQueueLength, 1) <= auctionQueue.size()) {
				sendMessage("auction-queue-fail-full", player, currentAuction, false);
				return;
			}
		} else {
	    	if (maxAuctionQueueLength <= 0) {
				sendMessage("auction-fail-auction-exists", player, currentAuction, false);
				return;
			}
			if (currentAuction.getOwner().equalsIgnoreCase(playerName)) {
				sendMessage("auction-queue-fail-current-auction", player, currentAuction, false);
				return;
			}
			if (maxAuctionQueueLength <= auctionQueue.size()) {
				sendMessage("auction-queue-fail-full", player, currentAuction, false);
				return;
			}
		}
		for(int i = 0; i < auctionQueue.size(); i++) {
			if (auctionQueue.get(i) != null) {
				Auction queuedAuction = auctionQueue.get(i);
				if (queuedAuction.getOwner().equalsIgnoreCase(playerName)) {
					sendMessage("auction-queue-fail-in-queue", player, currentAuction, false);
					return;
				}
			}
		}
		if ((auctionQueue.size() == 0 && System.currentTimeMillis() - lastAuctionDestroyTime >= minAuctionIntervalSecs * 1000) || auctionToQueue.isValid()) {
			auctionQueue.add(auctionToQueue);
			Participant.addParticipant(playerName);
			checkAuctionQueue();
			if (auctionQueue.contains(auctionToQueue)) {
				sendMessage("auction-queue-enter", player, currentAuction, false);
			}
		}
    }
	private void checkAuctionQueue() {
		if (publicAuction != null) {
			return;
		}
		if (System.currentTimeMillis() - lastAuctionDestroyTime < minAuctionIntervalSecs * 1000) {
			return;
		}
		if (auctionQueue.size() == 0) {
			return;
		}
		Auction auction = auctionQueue.remove(0);
		if (auction == null) {
			return;
		}
		
		Player player = getServer().getPlayer(auction.getOwner());
		if (player == null || !player.isOnline()) {
			return;
		}
		if (!allowCreativeMode && player.getGameMode() == GameMode.CREATIVE) {
			sendMessage("auction-fail-gamemode-creative", player, null, false);
			return;
		}
			
		if (!perms.has(player, "auction.start")) {
			sendMessage("no-permission", player, null, false);
			return;
		}
		if (!auction.isValid()) {
			return;
		}
		if (auction.start()) {
			publicAuction = auction;
		}
	}
    
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

		// In the mean time, use public auction.
		auction = publicAuction;
		String userScope = "public_auction";
		String playerName = "";

    	if (sender instanceof Player) {
    		player = (Player) sender;
			playerName = player.getName();
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
    	
    	if (suspendAllAuctions) {
	    	if (args.length == 1 && args[0].equalsIgnoreCase("resume")) {
				if (player != null && !perms.has(player, "auction.admin")) {
	    			sendMessage("no-permission", sender, null, false);
	    			return true;
				}
				// Suspend globally:
				suspendAllAuctions = false;
    			sendMessage("unsuspension-global", (Player) null, null, true);
				return true;
			}
			sendMessage("suspension-global", sender, null, false);
    		return true;
    	}
    	
    	if (player != null && suspendedUsers.contains(playerName.toLowerCase())) {
			sendMessage("suspension-user", player, null, false);
			return true;
    	}

    	if (
    		cmd.getName().equalsIgnoreCase("auc") ||
    		cmd.getName().equalsIgnoreCase("auction") ||
    		cmd.getName().equalsIgnoreCase("sauc") ||
    		cmd.getName().equalsIgnoreCase("sealedauction")
    	) {
    		if (args.length > 0) {
/*    			if (args[0].equalsIgnoreCase("addlore")) {
    				if (player == null || !perms.has(player, "auction.admin")) {
    	    			sendMessage("no-permission", sender, null, false);
    	    			return true;
    				}
    				ItemStack heldItem = player.getItemInHand();
    				if (heldItem == null) return true;
    				String[] lore = new String[4];
    				lore[0] = "Flobi's Lore";
    				lore[1] = "Crazy shit goes here!";
    				lore[2] = "More lore!";
    				lore[3] = "Last line of lore.";
    				items.setLore(heldItem, lore);
    				return true;
    			}*/
    			if (args[0].equalsIgnoreCase("reload")) {
    				if (player != null && !perms.has(player, "auction.admin")) {
    	    			sendMessage("no-permission", sender, null, false);
    	    			return true;
    				}
    				defConfigStream = getResource("config.yml");
    				defTextConfigStream = getResource("language.yml");
    				loadConfig();
	    			sendMessage("plugin-reloaded", sender, null, false);
    				return true;
/*    			} else if (args[0].equalsIgnoreCase("orphans")) {
    				if (player != null && !perms.has(player, "auction.admin")) {
    	    			sendMessage("no-permission", sender, null, false);
    	    			return true;
    				}
    				
    				for(int i = 0; i < orphanLots.size(); i++) {
    					if (orphanLots.get(i) != null) {
    						AuctionLot lot = orphanLots.get(i);
    		    			sendMessage(lot.getOwner() + ": " + lot.getQuantity() + " " + items.getItemName(lot.getTypeStack()), sender, null, false);
    					}
    				}

    				return true;*/
    			} else if (args[0].equalsIgnoreCase("resume")) {
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
    				
    				// Clear queued auctions first.
    				auctionQueue.clear();
    				
    				// Loop through all scopes when they come around.
    				if (publicAuction != null) {
    					publicAuction.cancel();
    				}

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
    				// Start new auction!
    	    		if (player == null) {
    	    			sendMessage("auction-fail-console", sender, null, false);
    	    			return true;
    	    		}
    	    		if (!allowCreativeMode && player.getGameMode() == GameMode.CREATIVE) {
    	    			sendMessage("auction-fail-gamemode-creative", sender, null, false);
    	    			return true;
    	    		}
    	    			
    				if (!perms.has(player, "auction.start")) {
    	    			sendMessage("no-permission", sender, null, false);
    	    			return true;
    				}
    				
    				if (cmd.getName().equalsIgnoreCase("sealedauction") || cmd.getName().equalsIgnoreCase("sauc")) {
    					if (allowSealedAuctions) {
    						queueAuction(new Auction(this, player, args, userScope, true), player, auction);
    					} else {
    						sendMessage("auction-fail-no-sealed-auctions", sender, auction, false);
    					}
    				} else {
    					if (allowUnsealedAuctions) {
    						queueAuction(new Auction(this, player, args, userScope, false), player, auction);
    					} else {
    						queueAuction(new Auction(this, player, args, userScope, true), player, auction);
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
						if (allowBuyNow) {
							sendMessage("prep-save-success-with-buynow", sender, null, false);
						} else {
							sendMessage("prep-save-success", sender, null, false);
						}
    				}

					return true;
    			} else if (args[0].equalsIgnoreCase("cancel") || args[0].equalsIgnoreCase("c")) {
    				if (auction == null && auctionQueue.size() == 0) {
    					sendMessage("auction-fail-no-auction-exists", sender, auction, false);
    					return true;
    				}
    				
    				for(int i = 0; i < auctionQueue.size(); i++){
    					if (auctionQueue.get(i).getOwner().equalsIgnoreCase(playerName)) {
    						auctionQueue.remove(i);
    						sendMessage("auction-cancel-queued", player, auction, false);
    						return true;
    					}
    				}
    				
    				if (auction == null) {
    					sendMessage("auction-fail-no-auction-exists", sender, auction, false);
    					return true;
    				}
    				
					if (player == null || player.getName().equalsIgnoreCase(auction.getOwner()) || perms.has(player, "auction.admin")) {
						if (cancelPreventionSeconds > auction.getRemainingTime() || cancelPreventionPercent > (double)auction.getRemainingTime() / (double)auction.getTotalTime() * 100D) {
	    					sendMessage("auction-fail-cancel-prevention", player, auction, false);
						} else {
	    					auction.cancel();
	    					publicAuction = null;
						}
					} else {
    					sendMessage("auction-fail-not-owner-cancel", player, auction, false);
					}
    				return true;
    			} else if (args[0].equalsIgnoreCase("confiscate") || args[0].equalsIgnoreCase("impound")) {
    				if (auction == null) {
    					sendMessage("auction-fail-no-auction-exists", sender, auction, false);
    					return true;
    				}
    				
    				if (player == null) {
    					sendMessage("confiscation-fail-console", player, auction, false);
    					return true;
    				}
					if (!perms.has(player, "auction.admin")) {
    					sendMessage("no-permission", player, auction, false);
    					return true;
					}
					if (playerName.equalsIgnoreCase(auction.getOwner())) {
    					sendMessage("confiscation-fail-self", player, auction, false);
    					return true;
					}
					auction.confiscate(player);
					publicAuction = null;
    				return true;
    			} else if (args[0].equalsIgnoreCase("end") || args[0].equalsIgnoreCase("e")) {
    				if (auction == null) {
    					sendMessage("auction-fail-no-auction-exists", player, auction, false);
        				return true;
    				}
    				if (!allowEarlyEnd) {
    					sendMessage("auction-fail-no-early-end", player, auction, false);
        				return true;
    				}
					if (player.getName().equalsIgnoreCase(auction.getOwner())) {
    					auction.end();
    					publicAuction = null;
					} else {
    					sendMessage("auction-fail-not-owner-end", player, auction, false);
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
    					sendMessage("auction-info-no-auction", player, auction, false);
    					return true;
    				}
					auction.info(sender, false);
    				return true;
    			} else if (args[0].equalsIgnoreCase("queue") || args[0].equalsIgnoreCase("q")) {
    				if (auctionQueue.isEmpty()) {
    					sendMessage("auction-queue-status-not-in-queue", player, auction, false);
    					return true;
    				}
    				for(int i = 0; i < auctionQueue.size(); i++){
    					if (auctionQueue.get(i).getOwner().equalsIgnoreCase(playerName)) {
        					sendMessage("auction-queue-status-in-queue", player, auction, false);
    						return true;
    					}
    				}

					sendMessage("auction-queue-status-not-in-queue", player, auction, false);
    				return true;
    			}
    		}
			sendMessage("auction-help", sender, auction, false);
    		return true;
    	} else if (cmd.getName().equalsIgnoreCase("bid")) {
    		if (player == null) {
    			sendMessage("bid-fail-console", console, null, false);
    			return true;
    		} 
    		if (!allowCreativeMode && player.getGameMode().equals(GameMode.CREATIVE)) {
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
    
    public static void sendMessage(String messageKey, CommandSender player, Auction auction, boolean fullBroadcast) {
    	if (messageKey == null) {
    		return;
    	}
    	List<String> messageKeys = new ArrayList<String>();
    	messageKeys.add(messageKey);
    	sendMessage(messageKeys, player, auction, fullBroadcast, "-");
    }

    public static void sendMessage(List<String> messageKeys, CommandSender player, Auction auction, boolean fullBroadcast) {
    	sendMessage(messageKeys, player, auction, fullBroadcast, "-");
    }

    public static void sendMessage(String messageKey, CommandSender player, Auction auction, boolean fullBroadcast, String fireworkAspect) {
    	if (messageKey == null) {
    		return;
    	}
    	List<String> messageKeys = new ArrayList<String>();
    	messageKeys.add(messageKey);
    	sendMessage(messageKeys, player, auction, fullBroadcast, fireworkAspect);
    }

    public static void sendMessage(List<String> messageKeys, CommandSender player, Auction auction, boolean fullBroadcast, String fireworkAspect) {

    	String playerName = null;
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

		for(int i = 0; i < auctionQueue.size(); i++){
			if (auctionQueue.get(i).getOwner().equalsIgnoreCase(playerName)) {
				queuePostition = Integer.toString(i + 1);
			}
		}


    	if (auction != null) {
    		typeLot = auction.getLotType();
    		
    		if (auction.getOwner() != null) owner = auction.getOwner();
    		
    		Player ownerPlayer = server.getPlayer(owner);
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
			
			timeRemaining = functions.formatTime(auction.getRemainingTime());
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
        		displayName = ChatColor.translateAlternateColorCodes('&', textConfig.getString("display-name-prefix")) + displayName + ChatColor.translateAlternateColorCodes('&', "&r");
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
    		
	    	List<String> messageList = textConfig.getStringList(messageKey);
	    	
	    	String originalMessage = null;
	    	if (messageList == null || messageList.size() == 0) {
	    		originalMessage = textConfig.getString(messageKey);
	    		
	    		if (originalMessage == null || originalMessage.length() == 0) {
	        		continue;
	    		} else {
	        		messageList = Arrays.asList(originalMessage.split("(\r?\n|\r)"));
	    		}
	    	}
	    	
	    	for (Iterator<String> i = messageList.iterator(); i.hasNext(); ) {
	    		String messageListItem = i.next();
	    		String message = chatPrep(messageListItem);
		
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
				message = message.replace("%k", Integer.toString(auctionQueue.size()));
				message = message.replace("%Q", queuePostition);
				message = message.replace("%c", floAuction.econ.currencyNameSingular());
				message = message.replace("%C", floAuction.econ.currencyNamePlural());
				
				String[] defaultStartArgs = functions.mergeInputArgs(playerName, new String[] {}, false);
				if (defaultStartArgs[0].equalsIgnoreCase("this") || defaultStartArgs[0].equalsIgnoreCase("hand")) {
					message = message.replace("%U", textConfig.getString("prep-amount-in-hand"));
				} else if (defaultStartArgs[0].equalsIgnoreCase("all")) {
					message = message.replace("%U", textConfig.getString("prep-all-of-this-kind"));
				} else {
					message = message.replace("%U", textConfig.getString("prep-qty-of-this-kind"));
				}
				message = message.replace("%u", defaultStartArgs[0]);
				message = message.replace("%v", defaultStartArgs[1]);
				message = message.replace("%V", functions.formatAmount(Double.parseDouble(defaultStartArgs[1])));
				message = message.replace("%w", defaultStartArgs[2]);
				message = message.replace("%W", functions.formatAmount(Double.parseDouble(defaultStartArgs[2])));
				message = message.replace("%z", defaultStartArgs[3]);
				message = message.replace("%Z", functions.formatTime(Integer.parseInt(defaultStartArgs[3])));
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
								String payloadSeparator = ChatColor.translateAlternateColorCodes('&', textConfig.getString("auction-info-payload-separator"));
								
								Type type = payload.getType();
								if (type != null) {
									if (!payloadAspects.isEmpty()) payloadAspects += payloadSeparator;
									if (textConfig.getString("firework-shapes." + type.toString()) == null) {
										payloadAspects += type.toString();
									} else {
										payloadAspects += textConfig.getString("firework-shapes." + type.toString());
									}
								}
								List<Color> colors = payload.getColors();
								for (int k = 0; k < colors.size(); k++) {
									if (!payloadAspects.isEmpty()) payloadAspects += payloadSeparator;
									Color color = colors.get(k);
									String colorRGB = color.toString().replace("Color:[rgb0x", "").replace("]", "");
									if (textConfig.getString("firework-colors." + colorRGB) == null) {
										payloadAspects += "#" + colorRGB;
									} else {
										payloadAspects += textConfig.getString("firework-colors." + colorRGB);
									}
								}
								if (payload.hasFlicker()) {
									if (!payloadAspects.isEmpty()) payloadAspects += payloadSeparator;
									payloadAspects += textConfig.getString("firework-twinkle");
								}
								if (payload.hasTrail()) {
									if (!payloadAspects.isEmpty()) payloadAspects += payloadSeparator;
									payloadAspects += textConfig.getString("firework-trail");
								}
								message = originalMessage.replace("%A", payloadAspects);
				            	if (fullBroadcast) {
				            		broadcastMessage(message);
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
						String enchantmentSeparator = ChatColor.translateAlternateColorCodes('&', textConfig.getString("auction-info-enchantment-separator"));
		        		for (Entry<Enchantment, Integer> enchantment : enchantments.entrySet()) {
		        			if (!enchantmentList.isEmpty()) enchantmentList += enchantmentSeparator;
		        			enchantmentList += items.getEnchantmentName(enchantment);
		        		}
		        		if (enchantmentList.isEmpty()) enchantmentList = ChatColor.translateAlternateColorCodes('&', textConfig.getString("auction-info-enchantment-none"));
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
			            		broadcastMessage(message);
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
			            		broadcastMessage(message);
			            	} else {
			        	    	player.sendMessage(message);
			            	}
			            	log(player, message);
		        		}
	    			}
	    			continue;
				}
	
				if (fullBroadcast) {
		    		broadcastMessage(message);
		    	} else if (player != null) {
			    	player.sendMessage(message);
		    	}
		    	log(player, message);
	    	}
    	}
    	
    }
    public static void broadcastMessage(String message) {
    	Player[] onlinePlayers = server.getOnlinePlayers();
    	
    	for (Player player : onlinePlayers) {
        	if (voluntarilyDisabledUsers.indexOf(player.getName()) == -1 && Participant.checkLocation(player.getName())) {
        		player.sendMessage(message);
    		}
    	}
    	
    	if (voluntarilyDisabledUsers.indexOf("*console*") == -1) {
			console.sendMessage(message);
		}
    }
    private static void log(CommandSender player, String message) {
    	if (logAuctions) {
    		String playerName = null;
    		
			BufferedWriter out = null;
			try {
		    	if (!auctionLog.exists()) {
					auctionLog.createNewFile();
					auctionLog.setWritable(true);
		    	}
		    	
				out = new BufferedWriter(new FileWriter(auctionLog.getAbsolutePath(), true));

				if (player == null) {
					playerName = "BROADCAST";
				} else {
					playerName = player.getName();
				}
				
				out.append((new Date()).toString() + " (" + playerName + "): " + ChatColor.stripColor(message) + "\n");
				out.close();

			} catch (IOException e) {
				
			}
	    	
			
    	}
		
	}
	private boolean setupEconomy() {
        if (server.getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = server.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    private boolean setupChat() {
        RegisteredServiceProvider<Chat> rsp = server.getServicesManager().getRegistration(Chat.class);
        if (rsp == null) {
            return false;
        }
        chat = rsp.getProvider();
        return chat != null;
    }

    private boolean setupPermissions() {
        RegisteredServiceProvider<Permission> rsp = server.getServicesManager().getRegistration(Permission.class);
        perms = rsp.getProvider();
        return perms != null;
    }

	public static void sendMessage(String messageKey, String playerName, Auction auction) {
		if (playerName == null) {
			sendMessage(messageKey, (CommandSender) null, auction, true);
		} else {
			sendMessage(messageKey, server.getPlayer(playerName), auction, false);
		}
		
	}
}

