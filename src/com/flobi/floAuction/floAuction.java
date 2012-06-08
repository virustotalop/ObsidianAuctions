package com.flobi.floAuction;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
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
import org.bukkit.event.player.PlayerJoinEvent;
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
	public static boolean allowBidOnOwn = false;
	public static boolean useOldBidLogic = false;
	public static boolean logAuctions = false;
	public static boolean allowEarlyEnd = false;
	public static boolean useGoldStandard = false;
	public static int decimalPlaces = 2;
	public static String decimalRegex = "(\\.[0-9][0-9]?)?";
	private static File auctionLog = null;
	
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
	
	public static ArrayList<String> disabledUsers = new ArrayList<String>(); 
	
	// In case items can't be given for some reason, save them
	public static ArrayList<AuctionLot> OrphanLots = new ArrayList<AuctionLot>();
	
	
	// Eliminate orphan lots (i.e. try to give the items to a player again).
	public static void killOrphan(Player player) {
		// List of orphans to potentially kill.
		ArrayList<AuctionLot> orphanDeathRow = OrphanLots;
		
		// New orphanage.
		OrphanLots = new ArrayList<AuctionLot>();
		
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
					OrphanLots.add(lot);
				}
			}
		}
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
        loadConfig();
		if (server.getPluginManager().getPlugin("WhatIsIt") == null) {
			log.log(Level.SEVERE, chatPrepClean(textConfig.getString("no-whatisit")));
			server.getPluginManager().disablePlugin(this);
            return;
		}
		setupEconomy();
        setupPermissions();
        setupChat();
        
        server.getPluginManager().registerEvents(new Listener() {
            @SuppressWarnings("unused")
			@EventHandler
            public void playerJoin(PlayerJoinEvent event) {
        	    floAuction.killOrphan(event.getPlayer());
            }
        }, this);		
		sendMessage("plugin-enabled", console, null);
		
		//TODO: Load orphan lots from save file.
	}
	public void onDisable() { 
		sendMessage("plugin-disabled", console, null);
		
		//TODO: Save orphan lots from save file.
	}
	public void detachAuction(Auction auction) {
		// TODO: make look through auction scopes.  
		// In the mean time, just null the public as that's the only one right now.
		publicAuction = null;
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
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
    	Player player = null;
    	Auction auction = null;
		// TODO: Figure out auction for context.
		// In the mean time, use public auction.
		auction = publicAuction;
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
			int index = disabledUsers.indexOf(playerName);
			if (index != -1) {
				disabledUsers.remove(index);
			}
			sendMessage("auction-enabled", sender, null);
			return true;
		}
     
    	if (disabledUsers.indexOf(playerName) != -1) {
    		disabledUsers.remove(disabledUsers.indexOf(playerName));
			sendMessage("auction-fail-disabled", sender, null);
			disabledUsers.add(playerName);
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
    				if (!perms.has(player, "auction.start")) {
    	    			sendMessage("no-permission", sender, null);
    	    			return true;
    				}
    				if (auction == null) {
    					auction = new Auction(this, player, args, "public_auction");
    					if (auction.isValid()) {
        					if (auction.start()) {
        						// TODO: assign to respective context
        						// In the mean time, use public auction
        						publicAuction = auction;
        					} else {
        						auction = null;
        					}
    					} else {
    						auction = null;
    					}
    				} else {
    					// Already an auction.
    					sendMessage("auction-fail-auction-exists", sender, auction);
    				}
					return true;
    			} else if (args[0].equalsIgnoreCase("cancel") || args[0].equalsIgnoreCase("c")) {
    				if (auction == null) {
    					sendMessage("auction-fail-no-auction-exists", sender, auction);
    					return true;
    				}
					if (player == null || player.getName().equalsIgnoreCase(auction.getOwner()) || perms.has(player, "auction.admin")) {
    					auction.cancel(player);
    					// TODO: Make scope specific
    					publicAuction = null;
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
    					// TODO: Make scope specific
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
    				if (disabledUsers.indexOf(playerName) == -1) {
    					sendMessage("auction-disabled", sender, null);
    					disabledUsers.add(playerName);
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
			if (!perms.has(player, "auction.start")) {
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
		    	if (disabledUsers.indexOf(player.getName()) != -1) {
		    		// Don't send this user any messages.
		    		return;
				}
	    	} else {
		    	if (disabledUsers.indexOf("*console*") != -1) {
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

    	if (auction != null) {
    		
    		if (auction.getOwner() != null) owner = auction.getOwner();
    		quantity = Integer.toString(auction.getLotQuantity());
    		lotType = WhatIsIt.itemName(auction.getLotType());
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
				currentBidder = auction.getCurrentBid().getBidder().getName();
				currentBid = functions.formatAmount(auction.getCurrentBid().getBidAmount());
				currentMaxBid = functions.formatAmount(auction.getCurrentBid().getMaxBidAmount());
			} else {
				currentBidder = "noone";
				currentBid = startingBid;
				currentMaxBid = startingBid;
			}
			auctionScope = auction.getScope();
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
    private static void broadcastMessage(String message) {
    	Player[] onlinePlayers = server.getOnlinePlayers();
    	
    	for (Player player : onlinePlayers) {
        	if (disabledUsers.indexOf(player.getName()) == -1) {
        		player.sendMessage(message);
    		}
    	}
    	
    	if (disabledUsers.indexOf("*console*") == -1) {
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
        chat = rsp.getProvider();
        return chat != null;
    }

    private boolean setupPermissions() {
        RegisteredServiceProvider<Permission> rsp = server.getServicesManager().getRegistration(Permission.class);
        perms = rsp.getProvider();
        return perms != null;
    }

    /**
	 * Loads config.yml and text.yml configuration files.
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
	    
	    if (econ == null) {
	    	useGoldStandard = true;
	    	config.set("use-gold-standard", true);
	    } else {
			useGoldStandard = config.getBoolean("use-gold-standard");
	    }
		if (useGoldStandard) {
			decimalPlaces = 0;
			config.set("decimal-places", decimalPlaces);
		} else {
			decimalPlaces = config.getInt("decimal-places");
		}
		if (decimalPlaces < 1) {
			decimalRegex = "";
		} else if (decimalPlaces > 1) {
			decimalRegex = "(\\.[0-9])?";
		} else {
			decimalRegex = "(\\.[0-9][0-9]?)?";
		}
	    defaultStartingBid = functions.getSafeMoney(config.getDouble("default-starting-bid"));
		defaultBidIncrement = functions.getSafeMoney(config.getDouble("default-bid-increment"));
		defaultAuctionTime = config.getInt("default-auction-time");
		maxStartingBid = functions.getSafeMoney(config.getDouble("max-starting-bid"));
		minIncrement = functions.getSafeMoney(config.getDouble("min-bid-increment"));
		maxIncrement = functions.getSafeMoney(config.getDouble("max-bid-increment"));
		maxTime = config.getInt("max-auction-time");
		minTime = config.getInt("min-auction-time");
		allowBidOnOwn = config.getBoolean("allow-bid-on-own-auction");
		useOldBidLogic = config.getBoolean("use-old-bid-logic");
		allowEarlyEnd = config.getBoolean("allow-early-end");


    	try {
    		config.save(configFile);
		} catch(IOException ex) {
			log.severe("Cannot save config.yml");
		}
    	defConfig = null;
	    configFile = null;

	    
		try {
    		textConfig.save(textConfigFile);
		} catch(IOException ex) {
			log.severe("Cannot save language.yml");
		}
        defTextConfig = null;
	    textConfigFile = null;
    }
	public static void sendMessage(String messageKey, String playerName, Auction auction) {
		if (playerName == null) {
			sendMessage(messageKey, (CommandSender) null, auction);
		} else {
			sendMessage(messageKey, server.getPlayer(playerName), auction);
		}
		
	}
}

