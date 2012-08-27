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
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import com.flobi.WhatIsIt.WhatIsIt;
import com.flobi.utility.functions;

public class floAuction extends JavaPlugin {
	private static final Logger log = Logger.getLogger("Minecraft");
	public Auction publicAuction;

	// Got to figure out a better way to store these:
	public static long defaultStartingBid = 0;
	public static long defaultBidIncrement = 100;
	public static int defaultAuctionTime = 60;
	public static long maxStartingBid = 10000;
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
	public static boolean allowCreativeMode = false;
	public static boolean allowDamagedItems = false;
	private static File auctionLog = null;
	private static long lastAuctionDestroyTime = 0;
	private static boolean suspendAllAuctions = false;
	public static boolean allowMaxBids = true;
	public static boolean allowGamemodeChange = false;
	public static boolean allowWorldChange = false;
	public static List<String> bannedItems = new ArrayList<String>();
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
	
	// TODO: Save these items when updated so we don't loose info when restarting.
	public static ArrayList<AuctionLot> orphanLots = new ArrayList<AuctionLot>();;
	public static ArrayList<String> voluntarilyDisabledUsers = new ArrayList<String>();;
	public static ArrayList<String> suspendedUsers = new ArrayList<String>();
	
	
	public static void saveObject(Object arraylist, String filename) {
    	File saveFile = new File(dataFolder, filename);
    	
    	try {
			//use buffering
    		if (saveFile.exists()) saveFile.delete();
			OutputStream file = new FileOutputStream(saveFile.getAbsolutePath());
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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

		if (server.getPluginManager().getPlugin("WhatIsIt") == null) {
			log.log(Level.SEVERE, chatPrepClean(textConfig.getString("no-whatisit")));
			server.getPluginManager().disablePlugin(this);
            return;
		}
        loadConfig();
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
                	getServer().getScheduler().scheduleAsyncDelayedTask(plugin, new Runnable() {
                		public void run() {
    	                	player.teleport(currentAuctionOwnerLocation, TeleportCause.PLUGIN);
                		}
                	}, 1L);
                	sendMessage("worldchange-fail-auction-owner", player, publicAuction);
                } else if (publicAuction.getCurrentBid() != null && publicAuction.getCurrentBid().getBidder().equalsIgnoreCase(player.getName())
                		 && !player.getLocation().getWorld().equals(currentBidPlayerLocation.getWorld())
                		) {
                	// This is running as a timer because MultiInv is using HIGHEST priority and 
                	// there's no way to send a cancel to it, so we have to go after the fact and
                	// remove the user.
                	getServer().getScheduler().scheduleAsyncDelayedTask(plugin, new Runnable() {
                		public void run() {
                        	player.teleport(currentBidPlayerLocation, TeleportCause.PLUGIN);
                		}
                	}, 1L);
                	sendMessage("worldchange-fail-auction-bidder", player, publicAuction);
                }
            }
            @EventHandler
            public void onPlayerChangedWorld(PlayerGameModeChangeEvent event){
            	if (allowGamemodeChange || publicAuction == null) return;
            	
                // Get player objects
                Player player = event.getPlayer();
                
                if (publicAuction.getOwner().equalsIgnoreCase(player.getName())) {
                	event.setCancelled(true);
                	sendMessage("gamemodechange-fail-auction-owner", player, publicAuction);
                } else if (publicAuction.getCurrentBid() != null && publicAuction.getCurrentBid().getBidder().equalsIgnoreCase(player.getName())) {
                	event.setCancelled(true);
                	sendMessage("gamemodechange-fail-auction-bidder", player, publicAuction);
                }
            }
        }, this);
		
		queueTimer = getServer().getScheduler().scheduleAsyncRepeatingTask(this, new Runnable() {
		    public void run() {
		    	checkAuctionQueue();
		    }
		}, 20L, 20L);
		
		orphanLots = loadArrayListAuctionLot("orphanLots.ser");
		voluntarilyDisabledUsers = loadArrayListString("voluntarilyDisabledUsers.ser");
		suspendedUsers = loadArrayListString("suspendedUsers.ser");

        // Load up the Plugin metrics
        try {
            MetricsLite metrics = new MetricsLite(this);
            metrics.start();
        } catch (IOException e) {
            // Failed to submit the stats :-(
        }
		sendMessage("plugin-enabled", console, null);
		
		//TODO: Load orphan lots from save file.
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
	    
		decimalPlaces = Math.min(Math.max(config.getInt("decimal-places"), 0), 5);
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
		Map<String, Object> textConfigValues = textConfig.getDefaults().getValues(true);
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
    }
	public void onDisable() { 
		getServer().getScheduler().cancelTask(queueTimer);
		sendMessage("plugin-disabled", console, null);
		
		//TODO: Save orphan lots from save file.
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
				sendMessage("auction-queue-fail-full", player, currentAuction);
				return;
			}
		} else {
	    	if (maxAuctionQueueLength <= 0) {
				sendMessage("auction-fail-auction-exists", player, currentAuction);
				return;
			}
			if (currentAuction.getOwner().equalsIgnoreCase(playerName)) {
				sendMessage("auction-queue-fail-current-auction", player, currentAuction);
				return;
			}
			if (maxAuctionQueueLength <= auctionQueue.size()) {
				sendMessage("auction-queue-fail-full", player, currentAuction);
				return;
			}
		}
		for(int i = 0; i < auctionQueue.size(); i++) {
			if (auctionQueue.get(i) != null) {
				Auction queuedAuction = auctionQueue.get(i);
				if (queuedAuction.getOwner().equalsIgnoreCase(playerName)) {
					sendMessage("auction-queue-fail-in-queue", player, currentAuction);
					return;
				}
			}
		}
		if (auctionToQueue.isValid()) {
			auctionQueue.add(auctionToQueue);
			checkAuctionQueue();
			if (auctionQueue.contains(auctionToQueue)) {
				sendMessage("auction-queue-enter", player, currentAuction);
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
		if (!auction.isValid()) {
			return;
		}
		if (auction.start()) {
			publicAuction = auction;
		}
	}
    
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
    	Player player = null;
    	Auction auction = null;
		// TODO: Figure out auction for context.
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
			sendMessage("auction-enabled", sender, null);
			saveObject(voluntarilyDisabledUsers, "voluntarilyDisabledUsers.ser");
			return true;
		}
     
    	if (voluntarilyDisabledUsers.contains(playerName)) {
    		voluntarilyDisabledUsers.remove(voluntarilyDisabledUsers.indexOf(playerName));
			sendMessage("auction-fail-disabled", sender, null);
			voluntarilyDisabledUsers.add(playerName);
			saveObject(voluntarilyDisabledUsers, "voluntarilyDisabledUsers.ser");
			return true;
		}
    	
    	if (suspendAllAuctions) {
	    	if (args.length == 1 && args[0].equalsIgnoreCase("resume")) {
				if (player != null && !perms.has(player, "auction.admin")) {
	    			sendMessage("no-permission", sender, null);
	    			return true;
				}
				// Suspend globally:
				suspendAllAuctions = false;
    			sendMessage("unsuspension-global", (Player) null, null);
				return true;
			}
			sendMessage("suspension-global", sender, null);
    		return true;
    	}
    	
    	if (player != null && suspendedUsers.contains(playerName.toLowerCase())) {
			sendMessage("suspension-user", player, null);
			return true;
    	}

    	if (cmd.getName().equalsIgnoreCase("auction")) {
    		if (args.length > 0) {
    			if (args[0].equalsIgnoreCase("reload")) {
    				if (player != null && !perms.has(player, "auction.admin")) {
    	    			sendMessage("no-permission", sender, null);
    	    			return true;
    				}
    				defConfigStream = getResource("config.yml");
    				defTextConfigStream = getResource("language.yml");
    				loadConfig();
	    			sendMessage("plugin-reloaded", sender, null);
    				return true;
    			} else if (args[0].equalsIgnoreCase("orphans")) {
    				if (player != null && !perms.has(player, "auction.admin")) {
    	    			sendMessage("no-permission", sender, null);
    	    			return true;
    				}
    				
    				for(int i = 0; i < orphanLots.size(); i++) {
    					if (orphanLots.get(i) != null) {
    						AuctionLot lot = orphanLots.get(i);
    		    			sendMessage(lot.getOwner() + ": " + lot.getQuantity() + " " + WhatIsIt.itemName(lot.getTypeStack()), sender, null);
    					}
    				}

    				return true;
    			} else if (args[0].equalsIgnoreCase("resume")) {
    				if (player != null && !perms.has(player, "auction.admin")) {
    	    			sendMessage("no-permission", sender, null);
    	    			return true;
    				}

					if (!suspendedUsers.contains(args[1].toLowerCase())) {
		    			sendMessage("unsuspension-user-fail-not-suspended", sender, null);
		    			return true;
					}

					suspendedUsers.remove(args[1].toLowerCase());
					saveObject(suspendedUsers, "suspendedUsers.ser");
	    			sendMessage("unsuspension-user", getServer().getPlayer(args[1]), null);
	    			sendMessage("unsuspension-user-success", sender, null);
    				
    				return true;
    			} else if (args[0].equalsIgnoreCase("suspend")) {
    				if (player != null && !perms.has(player, "auction.admin")) {
    	    			sendMessage("no-permission", sender, null);
    	    			return true;
    				}
    				if (args.length > 1) {
    					// Suspend a player:
    					if (suspendedUsers.contains(args[1].toLowerCase())) {
    		    			sendMessage("suspension-user-fail-already-suspended", sender, null);
    		    			return true;
    					}
    					
    					Player playerToSuspend = getServer().getPlayer(args[1]);
    					
    					if (playerToSuspend == null || !playerToSuspend.isOnline()) {
    		    			sendMessage("suspension-user-fail-is-offline", sender, null);
    		    			return true;
    					}
    					
    					if (perms.has(playerToSuspend, "auction.admin")) {
    		    			sendMessage("suspension-user-fail-is-admin", sender, null);
    		    			return true;
    					}
    					
    					suspendedUsers.add(args[1].toLowerCase());
    					saveObject(suspendedUsers, "suspendedUsers.ser");
		    			sendMessage("suspension-user", playerToSuspend, null);
		    			sendMessage("suspension-user-success", sender, null);
    					
    					return true;
    				}
    				// Suspend globally:
    				suspendAllAuctions = true;
    				
    				// Clear queued auctions first.
    				auctionQueue.clear();
    				
    				// Loop through all scopes when they come around.
    				if (publicAuction != null) {
    					publicAuction.cancel(player);
    				}

	    			sendMessage("suspension-global", (Player) null, null);

	    			return true;
    			} else if (
        				args[0].equalsIgnoreCase("start") || 
        				args[0].equalsIgnoreCase("s") ||
        				args[0].equalsIgnoreCase("this") ||
        				args[0].equalsIgnoreCase("all") ||
        				args[0].matches("[0-9]+")
    			) {
    				// Start new auction!
    	    		if (player == null) {
    	    			sendMessage("auction-fail-console", sender, null);
    	    			return true;
    	    		}
    	    		if (!allowCreativeMode && player.getGameMode() == GameMode.CREATIVE) {
    	    			sendMessage("auction-fail-gamemode-creative", sender, null);
    	    			return true;
    	    		}
    	    			
    				if (!perms.has(player, "auction.start")) {
    	    			sendMessage("no-permission", sender, null);
    	    			return true;
    				}
					queueAuction(new Auction(this, player, args, userScope), player, auction);
					
					return true;
    			} else if (args[0].equalsIgnoreCase("cancel") || args[0].equalsIgnoreCase("c")) {
    				if (auction == null) {
    					sendMessage("auction-fail-no-auction-exists", sender, auction);
    					return true;
    				}
					if (player == null || player.getName().equalsIgnoreCase(auction.getOwner()) || perms.has(player, "auction.admin")) {
						if (cancelPreventionSeconds > auction.getRemainingTime() || cancelPreventionPercent > (double)auction.getRemainingTime() / (double)auction.getTotalTime() * 100D) {
	    					sendMessage("auction-fail-cancel-prevention", player, auction);
						} else {
	    					auction.cancel(player);
	    					publicAuction = null;
						}
					} else {
    					sendMessage("auction-fail-not-owner-cancel", player, auction);
					}
    				return true;
    			} else if (args[0].equalsIgnoreCase("end") || args[0].equalsIgnoreCase("e")) {
    				if (auction == null) {
    					sendMessage("auction-fail-no-auction-exists", player, auction);
        				return true;
    				}
    				if (!allowEarlyEnd) {
    					sendMessage("auction-fail-no-early-end", player, auction);
        				return true;
    				}
					if (player.getName().equalsIgnoreCase(auction.getOwner())) {
    					auction.end(player);
    					publicAuction = null;
					} else {
    					sendMessage("auction-fail-not-owner-end", player, auction);
					}
    				return true;
    			} else if (
        				args[0].equalsIgnoreCase("stfu") ||
        				args[0].equalsIgnoreCase("quiet") ||
        				args[0].equalsIgnoreCase("off") ||
        				args[0].equalsIgnoreCase("silent") ||
        				args[0].equalsIgnoreCase("silence")
    			) {
    				if (voluntarilyDisabledUsers.indexOf(playerName) == -1) {
    					sendMessage("auction-disabled", sender, null);
    					voluntarilyDisabledUsers.add(playerName);
    					saveObject(voluntarilyDisabledUsers, "voluntarilyDisabledUsers.ser");
    				}
    				return true;
    			} else if (args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("i")) {
    				if (auction == null) {
    					sendMessage("auction-info-no-auction", player, auction);
    					return true;
    				}
					auction.info(sender);
    				return true;
    			}
    		}
			sendMessage("auction-help", sender, auction);
    		return true;
    	} else if (cmd.getName().equalsIgnoreCase("bid")) {
    		if (player == null) {
    			sendMessage("bid-fail-console", console, null);
    			return true;
    		} 
    		if (!allowCreativeMode && player.getGameMode().equals(GameMode.CREATIVE)) {
    			sendMessage("bid-fail-gamemode-creative", sender, null);
    			return true;
    		}
			if (!perms.has(player, "auction.bid")) {
    			sendMessage("no-permission", sender, null);
    			return true;
			}
    		if (auction == null) {
    			sendMessage("bid-fail-no-auction", player, null);
    			return true;
    		}
    		auction.Bid(player, args);
    		return true;
    	}
    	return false;
    }
    
    public static void sendMessage(String messageKey, CommandSender player, Auction auction) {

    	if (player != null) {
	    	if (player instanceof Player) {
		    	if (voluntarilyDisabledUsers.indexOf(player.getName()) != -1) {
		    		// Don't send this user any messages.
		    		return;
				}
	    	} else {
		    	if (voluntarilyDisabledUsers.indexOf("*console*") != -1) {
		    		// Don't send console any messages.
		    		return;
				}
	    	}
    	}
    	

    	if (messageKey == null) {
    		return;
    	}
    	
    	String owner = null;
    	String quantity = null;
    	String lotType = null;
    	String startingBid = null;
    	String minBidIncrement = null;
    	String currentBidder = null;
    	String currentBid = null;
    	String currentMaxBid = null;
    	String timeRemaining = null;
    	String auctionScope = null;
    	String durabilityRemaining = null;
    	String endAuctionTax = null;
    	String startAucitonTax = functions.formatAmount(taxPerAuction);

    	if (auction != null) {
    		ItemStack typeLot = auction.getLotType();
    		
    		if (auction.getOwner() != null) owner = auction.getOwner();
    		quantity = Integer.toString(auction.getLotQuantity());
    		lotType = WhatIsIt.itemName(typeLot);
    		if (auction.getStartingBid() == 0) {
	    		startingBid = functions.formatAmount(auction.getMinBidIncrement());
    		} else {
	    		startingBid = functions.formatAmount(auction.getStartingBid());
    		}
    		minBidIncrement = functions.formatAmount(auction.getMinBidIncrement());
			
    		if (auction.getRemainingTime() >= 60) {
    			timeRemaining = textConfig.getString("time-format-minsec");
    			timeRemaining = timeRemaining.replace("%s", Integer.toString(auction.getRemainingTime() % 60));
    			timeRemaining = timeRemaining.replace("%m", Integer.toString((auction.getRemainingTime() - (auction.getRemainingTime() % 60)) / 60));
    		} else {
    			timeRemaining = textConfig.getString("time-format-seconly");
    			timeRemaining = timeRemaining.replace("%s", Integer.toString(auction.getRemainingTime()));
    		}
	
			if (auction.getCurrentBid() != null) {
				currentBidder = auction.getCurrentBid().getBidder();
				currentBid = functions.formatAmount(auction.getCurrentBid().getBidAmount());
				currentMaxBid = functions.formatAmount(auction.getCurrentBid().getMaxBidAmount());
				endAuctionTax = functions.formatAmount((long) Math.floor(auction.getCurrentBid().getMaxBidAmount() * (floAuction.taxPercentage / 100D)));
			} else {
				currentBidder = "noone";
				currentBid = startingBid;
				currentMaxBid = startingBid;
				endAuctionTax = "-";
			}
			auctionScope = auction.getScope();
        	durabilityRemaining = "-";
			if (typeLot != null) {
				if (typeLot.getType().getMaxDurability() > 0) {
			        DecimalFormat decimalFormat = new DecimalFormat("#%");
			        durabilityRemaining = decimalFormat.format((1 - ((double) typeLot.getDurability() / (double) typeLot.getType().getMaxDurability())));
				}
			}
    	} else {
        	owner = "-";
        	quantity = "-";
        	lotType = "-";
        	startingBid = "-";
        	minBidIncrement = "-";
        	currentBidder = "-";
        	currentBid = "-";
        	currentMaxBid = "-";
        	timeRemaining = "-";
        	auctionScope = "no_auction";
        	durabilityRemaining = "-";
        	endAuctionTax = "-";
    	}
    	
    	List<String> messageList = textConfig.getStringList(messageKey);
    	
    	String originalMessage = null;
    	if (messageList == null || messageList.size() == 0) {
    		originalMessage = textConfig.getString(messageKey.toString());
    		
    		
    		if (originalMessage == null || originalMessage.length() == 0) {
        		messageList = new ArrayList<String>();
    			messageList.add(messageKey.toString());
    		} else {
        		messageList = Arrays.asList(originalMessage.split("(\r?\n|\r)"));
    		}
    	}
    	
    	for (Iterator<String> i = messageList.iterator(); i.hasNext(); ) {
    		String messageListItem = i.next();
	    	originalMessage = chatPrep(messageListItem);
	    	String message = originalMessage;
	
			message = message.replace("%O", owner);
			message = message.replace("%q", quantity);
			message = message.replace("%i", lotType);
			message = message.replace("%s", startingBid);
			message = message.replace("%n", minBidIncrement);
			message = message.replace("%b", currentBid);
			message = message.replace("%B", currentBidder);
			message = message.replace("%h", currentMaxBid);
			message = message.replace("%t", timeRemaining);
			message = message.replace("%D", durabilityRemaining);
			message = message.replace("%x", startAucitonTax);
			message = message.replace("%X", endAuctionTax);
	
			if (messageKey == "auction-info-enchantment") {
    			if (auction != null && auction.getLotType() != null) {
	        		Map<Enchantment, Integer> enchantments = auction.getLotType().getEnchantments();
	        		for (Entry<Enchantment, Integer> enchantment : enchantments.entrySet()) {
	        			message = originalMessage.replace("%E", WhatIsIt.enchantmentName(enchantment));
		            	if (player == null) {
		            		broadcastMessage(message);
		            	} else {
		        	    	player.sendMessage(message);
		            	}
		            	log(auctionScope, player, message);
	        		}
    			}
			} else {
		    	if (player == null) {
		    		broadcastMessage(message);
		    	} else {
			    	player.sendMessage(message);
		    	}
		    	log(auctionScope, player, message);
			}
    	}
    	
    }
    public static void broadcastMessage(String message) {
    	Player[] onlinePlayers = server.getOnlinePlayers();
    	
    	for (Player player : onlinePlayers) {
        	if (voluntarilyDisabledUsers.indexOf(player.getName()) == -1) {
        		player.sendMessage(message);
    		}
    	}
    	
    	if (voluntarilyDisabledUsers.indexOf("*console*") == -1) {
			console.sendMessage(message);
		}
    }
    private static void log(String scope, CommandSender player, String message) {
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
				
				out.append(scope + " (" + playerName + "): " + ChatColor.stripColor(message) + "\n");
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
			sendMessage(messageKey, (CommandSender) null, auction);
		} else {
			sendMessage(messageKey, server.getPlayer(playerName), auction);
		}
		
	}
}

