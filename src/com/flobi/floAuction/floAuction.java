package com.flobi.floAuction;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import com.flobi.WhatIsIt.WhatIsIt;

public class floAuction extends JavaPlugin {
	private static final Logger log = Logger.getLogger("Minecraft");
	public Auction publicAuction;

	// Got to figure out a better way to store these:
	public int defaultStartingBid = 100;
	public int defaultBidIncrement = 100;
	public int defaultAuctionTime = 60;
	public int maxStartingBid = 10000;
	public int minIncrement = 1;
	public int maxIncrement = 100;
	public int maxTime = 60;
	public int minTime = 15;
	
	// Config files info.
	private static File configFile = null;
	private static InputStream defConfigStream;
	private static FileConfiguration config = null;
	private static File textConfigFile = null;
	private static InputStream defTextConfigStream;
	private static FileConfiguration textConfig = null;
	private static YamlConfiguration defConfig = null;
	private static YamlConfiguration defTextConfig = null;
	private static File dataFolder;
	private static ConsoleCommandSender console;
	
	// In case items can't be given for some reason, save them
	public static ArrayList<AuctionLot> OrphanLots =  new ArrayList<AuctionLot>();
	
	
	public static void killOrphan(Player player) {
		for(int i = 0; i < OrphanLots.size(); i++) {
			if (OrphanLots.get(i).getOwner().equals(player)) {
				OrphanLots.get(i).cancelLot();
			}
		}
	}
	

	// Vault objects
	public static Economy econ = null;
    public static Permission perms = null;
    public static Chat chat = null;

	public void onEnable() {
		console = getServer().getConsoleSender();
		dataFolder = getDataFolder();
		defConfigStream = getResource("config.yml");
		defTextConfigStream = getResource("language.yml");
        loadConfig();
		if (getServer().getPluginManager().getPlugin("WhatIsIt") == null) {
			log.log(Level.SEVERE, chatPrepClean(textConfig.getString("NO_WHATISIT")));
            getServer().getPluginManager().disablePlugin(this);
            return;
		}
		if (!setupEconomy() ) {
			log.log(Level.SEVERE, chatPrepClean(textConfig.getString("NO_VAULT")));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        setupPermissions();
        setupChat();
		
		console.sendMessage(chatPrep(textConfig.getString("PLUGIN_ENABLED")));
		
		//TODO: Load orphan lots from save file.
	}
	public void onDisable() { 
		console.sendMessage(chatPrep(textConfig.getString("PLUGIN_DISABLED")));
		
		//TODO: Save orphan lots from save file.
	}
	/**
	 * Prepares chat, prepending prefix and processing colors.
	 * 
	 * @param String message to prepare
	 * @return String prepared message
	 */
    private static String chatPrep(String message) {
    	message = textConfig.getString("CHAT_PREFIX") + message;
    	message = ChatColor.translateAlternateColorCodes('&', message);
    	return message;
    }
    private static String chatPrepClean(String message) {
    	message = textConfig.getString("CHAT_PREFIX") + message;
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
    	if (sender instanceof Player) {
    		player = (Player) sender;
    	} else {
    	}
     
    	if (cmd.getName().equalsIgnoreCase("auction")) {
    		if (args.length > 0) {
    			if (args[0].equalsIgnoreCase("reload")) {
    				//TODO: check permissions
    				loadConfig();
    				if (args.length > 1) {
    					if (textConfig.getString(args[1]) != null) {
		    				sender.sendMessage(chatPrep(textConfig.getString(args[1])));
    					}
    				}
    			} else if (
        				args[0].equalsIgnoreCase("start") ||
        				args[0].equalsIgnoreCase("this") ||
        				args[0].equalsIgnoreCase("all") ||
        				args[0].matches("[0-9]+")
    			) {
    				// Start new auction!
    	    		if (player == null) {
    	    			sendMessage(AuctionMessage.AUCTION_FAIL_CONSOLE, null, null);
    	    		} else {
        				if (auction == null) {
        					auction = new Auction(this, player, args);
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
        					sendMessage(AuctionMessage.AUCTION_FAIL_AUCTION_EXISTS, player, auction);
        				}
    	    		}
					return true;
    			} else if (args[0].equalsIgnoreCase("cancel")) {
    				if (auction == null) {
    					sendMessage(AuctionMessage.AUCTION_FAIL_NO_AUCTION_EXISTS, player, auction);
    				} else {
    					if (player.equals(auction.getOwner())) {
	    					auction.cancel(player);
	    					// TODO: Make scope specific
	    					publicAuction = null;
    					} else {
        					sendMessage(AuctionMessage.AUCTION_FAIL_NOT_OWNER_CANCEL, player, auction);
    					}
    				}
    				return true;
    			} else if (args[0].equalsIgnoreCase("end")) {
    				if (auction == null) {
    					sendMessage(AuctionMessage.AUCTION_FAIL_NO_AUCTION_EXISTS, player, auction);
    				} else {
    					if (player.equals(auction.getOwner())) {
	    					auction.end(player);
	    					// TODO: Make scope specific
	    					publicAuction = null;
    					} else {
        					sendMessage(AuctionMessage.AUCTION_FAIL_NOT_OWNER_END, player, auction);
    					}
    				}
    				return true;
    			}
    		}
			sendMessage(AuctionMessage.AUCTION_HELP, player, auction);
    		return true;
    	} else if (cmd.getName().equalsIgnoreCase("bid")) {
    		if (player == null) {
    			sendMessage(AuctionMessage.BID_FAIL_CONSOLE, null, null);
    		} else if (auction == null) {
    			sendMessage(AuctionMessage.BID_FAIL_NO_AUCTION, null, null);
    		} else {
    			auction.Bid(player, args);
    		}
    		return true;
    	}
    	return false;
    }
    
    public void sendMessage(AuctionMessage messageKey, Player player, Auction scope) {
    	String message = textConfig.getString(messageKey.toString());
    	if (message == null) {
    		message = messageKey.toString();
    	}
    		
    	message = chatPrep(message);
    	
/*    	switch (messageKey) {
    		case Q1:
    			
    	}*/

    	getServer().broadcastMessage(message);
    }
    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    private boolean setupChat() {
        RegisteredServiceProvider<Chat> rsp = getServer().getServicesManager().getRegistration(Chat.class);
        chat = rsp.getProvider();
        return chat != null;
    }

    private boolean setupPermissions() {
        RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager().getRegistration(Permission.class);
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
	    config = YamlConfiguration.loadConfiguration(configFile);
	 
	    // Look for defaults in the jar
	    if (defConfig != null) {
	    	config.setDefaults(defConfig);
	        defConfigStream = null;
	    }
	    if (defConfigStream != null) {
	        defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
	        config.setDefaults(defConfig);
	        defConfigStream = null;
	    }
	    if (!configFile.exists() && defConfig != null) {
	    	try {
	    		defConfig.save(configFile);
			} catch(IOException ex) {
				log.severe("Cannot save config.yml");
			}
	    }
	    if (textConfigFile == null) {
	    	textConfigFile = new File(dataFolder, "language.yml");
	    }
	    textConfig = YamlConfiguration.loadConfiguration(textConfigFile);
	 
	    // Look for defaults in the jar
	    if (defTextConfig != null) {
	        textConfig.setDefaults(defTextConfig);
	        defTextConfigStream = null;
	    }
	    if (defTextConfigStream != null) {
	        defTextConfig = YamlConfiguration.loadConfiguration(defTextConfigStream);
	        textConfig.setDefaults(defTextConfig);
	        defTextConfigStream = null;
	    }
	    if (!textConfigFile.exists() && defTextConfig != null) {
	    	try {
	    		defTextConfig.save(textConfigFile);
			} catch(IOException ex) {
				log.severe("Cannot save language.yml");
			}
	    }
    }
}

