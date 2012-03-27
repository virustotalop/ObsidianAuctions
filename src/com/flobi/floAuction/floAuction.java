package com.flobi.floAuction;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

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
        if (!setupEconomy() ) {
			log.log(Level.SEVERE, String.format("[%s] - Disabled due to no Vault dependency found!", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        setupPermissions();
        setupChat();
		
		log.info(this.getConfig().getName() + " has been enabled.");
		
		//TODO: Load orphan lots from save file.
	}
	public void onDisable() { 
		log.info(this.getConfig().getName() + " has been disabled.");
		
		//TODO: Save orphan lots from save file.
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
    			if (
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
//    	String publicMessage = null;
//    	String privateMessage = null;
    	
    	// TODO:  Make this language supportive.
    	
    	getServer().broadcastMessage(messageKey.toString());
    	
/*    	switch (messageKey) {
    		case Q1:
    			
    	}*/
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

}

