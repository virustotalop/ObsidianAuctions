package com.flobi.floauction;

import io.puharesource.mc.titlemanager.api.ActionbarTitleObject;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import me.clip.placeholderapi.PlaceholderAPI;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Material;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.flobi.floauction.utilities.Functions;
import com.flobi.floauction.utilities.Items;

public class AuctionMessageManager extends MessageManager {
	private static Map<String, Map<String, String>> replacementDefaults = new HashMap<String, Map<String, String>>();
	
	public AuctionMessageManager() 
	{
		Map<String, String> aReplacments = new HashMap<String, String>();
		aReplacments.put("%auction-owner-name%", "-"); //%A1
		aReplacments.put("%auction-owner-display-name%", "-"); //%A2
		aReplacments.put("%auction-quantity%", "-"); //%A3
		aReplacments.put("%auction-bid-starting%", "-"); //%A4
		aReplacments.put("%auction-bid-increment%", "-"); //%A5
		aReplacments.put("%auction-buy-now%", "-"); //%A6
		aReplacments.put("%auction-remaining-time%", "-"); //%A7
		aReplacments.put("%auction-pre-tax%", "-"); //%A8
		aReplacments.put("%auction-post-tax%", "-"); //%A9
		replacementDefaults.put("a", aReplacments);
		
		Map<String, String> bReplacments = new HashMap<String, String>();
		bReplacments.put("%current-bid-name%", "-"); //%B1
		bReplacments.put("%current-bid-display-name%", "-"); //%B2
		bReplacments.put("%current-bid-amount%", "-"); //%B3
		bReplacments.put("%auction-bid-starting%", "-"); //%B4
		replacementDefaults.put("b", bReplacments);
		
		Map<String, String> lReplacments = new HashMap<String, String>();
		lReplacments.put("%item-material-name%", "-"); //%L1
		lReplacments.put("%item-display-name%", "-"); //%L2
		lReplacments.put("%item-firework-power%", "-"); //%L3
		lReplacments.put("%item-book-author%", "-"); //%L4
		lReplacments.put("%item-book-title%", "-"); //%l5
		lReplacments.put("%item-durability-left%", "-"); //%L6
		lReplacments.put("%item-enchantments%", "-"); //%L7
		replacementDefaults.put("l", lReplacments);
		
		Map<String, String> pReplacments = new HashMap<String, String>();
		pReplacments.put("%auction-prep-amount-other%", "-"); //%P1
		pReplacments.put("%auction-prep-amount-other%", "-"); //%P2
		pReplacments.put("%auction-prep-price-formatted%", "-"); //%P3
		pReplacments.put("%auction-prep-price%", "-"); //%P4
		pReplacments.put("%auction-prep-increment-formatted%", "-"); //%P5
		pReplacments.put("%auction-prep-increment%", "-"); //%P6
		pReplacments.put("%auction-prep-time-formatted%", "-"); //%P7
		pReplacments.put("%auction-prep-time%", "-"); //%P8
		pReplacments.put("%auction-prep-buynow-formatted%", "-"); //%P9
		pReplacments.put("%auction-prep-buynow%", "-"); //%P0
		replacementDefaults.put("p", pReplacments);
		
		Map<String, String> sReplacments = new HashMap<String, String>();
		sReplacments.put("%player-auction-queue-position%", "-"); //%S1
		sReplacments.put("%auction-queue-length%", "-"); //%S2
		sReplacments.put("%auction-scope-name%", "-"); //%S3
		sReplacments.put("%auction-scope-id%", "-"); //%S4
		replacementDefaults.put("s", sReplacments);
	}
	
	public void sendPlayerMessage(List<String> messageKeys, String playerName, Auction auction) 
	{
		CommandSender recipient = null;
		if (playerName == null) 
		{
			recipient = Bukkit.getConsoleSender();
		} 
		else 
		{
			recipient = Bukkit.getPlayer(playerName);
		}
		AuctionScope auctionScope = null;
		if (auction != null) 
		{
			auctionScope = auction.getScope();
		}
		if (auctionScope == null && recipient instanceof Player)
		{
			auctionScope = AuctionScope.getPlayerScope((Player) recipient);
		}
		this.sendMessage(messageKeys, recipient, auctionScope, false);
	}
	
	public void sendPlayerMessage(List<String> messageKeys, String playerName, AuctionScope auctionScope) 
	{
		CommandSender recipient = null;
		if (playerName == null) 
		{
			recipient = Bukkit.getConsoleSender();
		} 
		else 
		{
			recipient = Bukkit.getPlayer(playerName);
		}
		if (auctionScope == null && recipient instanceof Player) 
		{
			auctionScope = AuctionScope.getPlayerScope((Player) recipient);
		}
		this.sendMessage(messageKeys, recipient, auctionScope, false);
	}
	
	public void broadcastAuctionMessage(List<String> messageKeys, Auction auction) 
	{
		if (auction == null) 
		{
			return;
		}
		AuctionScope auctionScope = auction.getScope();
		this.sendMessage(messageKeys, null, auctionScope, true);
	}
	
	public void broadcastAuctionScopeMessage(List<String> messageKeys, AuctionScope auctionScope) 
	{
		this.sendMessage(messageKeys, null, auctionScope, true);
	}
	
	/**
     * Sends a message to a player or scope.
     * 
     * @param messageKeys keys to message in language.yml
     * @param player focused player
     * @param auctionScope focused scope
     * @param fullBroadcast whether to broadcast or send to player
     */
    private void sendMessage(List<String> messageKeys, CommandSender sender, AuctionScope auctionScope, boolean fullBroadcast) {

    	Auction auction = null;
    	Player player = null;
    	
    	if (auctionScope != null) 
    	{
    		auction = auctionScope.getActiveAuction();
    	}
    	
    	if (sender != null) 
    	{
	    	if (sender instanceof Player) 
	    	{
	    		player = (Player) sender;
		    	if (!fullBroadcast && FloAuction.getVoluntarilyDisabledUsers().indexOf(player.getName()) != -1) 
		    	{
		    		// Don't send this user any messages.
		    		return;
				}
	    	}
	    	else 
	    	{
		    	if (!fullBroadcast && FloAuction.getVoluntarilyDisabledUsers().indexOf("*console*") != -1) 
		    	{
		    		// Don't send console any messages.
		    		return;
				}
	    	}
    	}
    	
    	List<String> messages = parseMessages(messageKeys, auctionScope, auction, player, fullBroadcast);

		if (fullBroadcast) 
		{
    		broadcastMessage(messages, auctionScope);
    	} 
		else if (player != null) 
    	{
	    	for (String message : messages) 
	    	{
	    		if(FloAuction.enableChatMessages)
	    			player.sendMessage(message);
	    		if(FloAuction.titleManagerEnabled && FloAuction.enableActionbarMessages)
	    			new ActionbarTitleObject(message).send(player);
	    		
	    		FloAuction.log(player.getName(), message, auctionScope);
	    	}
    	} 
		else if (sender != null) 
		{
    		ConsoleCommandSender console = Bukkit.getConsoleSender();
	    	for (String message : messages) 
	    	{
	    		console.sendMessage(ChatColor.stripColor(message));
	    		FloAuction.log("CONSOLE", message, auctionScope);
	    	}
    	} 
		else 
		{
	    	for (String message : messages) 
	    	{
	    		FloAuction.log("NO TARGET!", message, auctionScope);
	    	}
    	}
    }
    
    /**
     * Broadcast a message to everyone in an auctionscope.
     * 
     * @param message message to send
     * @param auctionScope scope to send it to
     */
    private static void broadcastMessage(List<String> messages, AuctionScope auctionScope) 
    {
    	Collection<? extends Player> onlinePlayers = Bukkit.getServer().getOnlinePlayers();
    	
    	
    	for (Player player : onlinePlayers) 
    	{
        	if (FloAuction.getVoluntarilyDisabledUsers().contains(player.getName())) 
        	{
        		continue;
        	}
    		if (auctionScope != null && !auctionScope.equals(AuctionScope.getPlayerScope(player))) 
    		{
    			continue;
    		}
	    	for (String message : messages) 
	    	{
	    		if(FloAuction.enableChatMessages)
	    			player.sendMessage(message);
	    		if(FloAuction.titleManagerEnabled && FloAuction.enableActionbarMessages)
	    			new ActionbarTitleObject(message).send(player);
	    	}
    	}
    	
    	if (auctionScope == null && FloAuction.getVoluntarilyDisabledUsers().indexOf("*console*") == -1) 
    	{
	    	for (String message : messages) 
	    	{
	    		message = ChatColor.stripColor(message);
				Bukkit.getConsoleSender().sendMessage(message);
	    	}
		}
    	for (String message : messages) 
    	{
    		message = ChatColor.stripColor(message);
    		FloAuction.log("BROADCAST", message, auctionScope);
    	}
    }
    
	/**
	 * Prepares chat, prepending prefix and processing colors.
	 * 
	 * @param message message to prepare
	 * @param auctionScope the scope of the destination
	 * @return prepared message
	 */
    private static String chatPrep(String message, AuctionScope auctionScope) 
    {
    	message = ChatColor.translateAlternateColorCodes('&', AuctionConfig.getLanguageString("chat-prefix", auctionScope)) + message;
    	return message;
    }
    
    /**
     * Sends via raw message wrapped in json.  This is currently a placeholder sending normally instead.
     * 
     * @param playerName
     * @param message
     */
/*    private static void sendTellRaw(String playerName, String message) {
    	Bukkit.getPlayer(playerName).sendMessage(message);
    	Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), "tellraw " + playerName + " {text:\"" + escapeJSONString(message) + "\"}");
    }
    
    private static String escapeJSONString(String message) {
    	return message.replace("'", "\\'").replace("\"", "\\\"").replace("\\", "\\\\");
    }*/
    
	/**
	 * Gets the messages from the language.yml file based on the keys passed in.
	 * 
	 * @param messageKeys Keys specified in the language.yml file
	 * @param auctionScope A scope to check for local messages
	 * @return List of actual messages to send
	 */
	private List<String> parseMessages(List<String> messageKeys, AuctionScope auctionScope, Auction auction, Player player, boolean isBroadcast) 
	{
		List<String> messageList = new ArrayList<String>();
		
    	for (int l = 0; l < messageKeys.size(); l++) 
    	{
    		String messageKey = messageKeys.get(l);
        	if (messageKey == null) 
        	{
        		continue;
        	}
    		
	    	List<String> partialMessageList = AuctionConfig.getLanguageStringList(messageKey, auctionScope);
	    	
	    	if (partialMessageList == null || partialMessageList.size() == 0) 
	    	{
		    	String originalMessage = null;
	    		originalMessage = AuctionConfig.getLanguageString(messageKey, auctionScope);
	    		
	    		if (originalMessage == null || originalMessage.length() == 0) 
	    		{
	        		continue;
	    		} 
	    		else 
	    		{
	    			partialMessageList = Arrays.asList(originalMessage.split("(\r?\n|\r)"));
	    		}
	    	}	
    		messageList.addAll(partialMessageList);
    	}
		return parseMessageTokens(messageList, auctionScope, auction, player, isBroadcast);
	}
	
	private List<String> parseMessageTokens(List<String> messageList, AuctionScope auctionScope, Auction auction, Player player, boolean isBroadcast) 
	{
		List<String> newMessageList = new ArrayList<String>();
		Map<String, String> replacements = new HashMap<String, String>();
		ItemStack lot = null;
		
		if (auction == null && auctionScope != null) 
		{
			auction = auctionScope.getActiveAuction();
		}
		
		// Search to see if auction info is required:
    	for (int l = 0; l < messageList.size(); l++) 
    	{
    		String message = messageList.get(l);
    		if (message.length() > 0 && message.contains("%auction-")) {
    			replacements.putAll(replacementDefaults.get("a"));
    			if (auction != null) 
    			{
    	    		replacements.put("%auction-owner-name%", auction.getOwner()); //%A1
    				replacements.put("%auction-owner-display-name%", auction.getOwnerDisplayName()); //%A2
    	    		replacements.put("%auction-quantity%", Integer.toString(auction.getLotQuantity()));
    	    		if (auction.getStartingBid() == 0) 
    	    		{
        	    		replacements.put("%auction-bid-starting%", Functions.formatAmount(auction.getMinBidIncrement())); //%A4
    	    		} 
    	    		else 
    	    		{
        	    		replacements.put("%auction-bid-starting%", Functions.formatAmount(auction.getStartingBid())); //%A4
    	    		}
    	    		replacements.put("%auction-bid-increment%", Functions.formatAmount(auction.getMinBidIncrement())); //%A5
    	    		replacements.put("%auction-buy-now%", Functions.formatAmount(auction.getBuyNow())); //%A6
    	    		replacements.put("%auction-remaining-time%", Functions.formatTime(auction.getRemainingTime(), auctionScope)); //%A7
    	    		replacements.put("%auction-pre-tax%", Functions.formatAmount(auction.extractedPreTax)); //%A8
    	    		replacements.put("%auction-post-tax%", Functions.formatAmount(auction.extractedPostTax)); //%A9
    			}
	    		break;
    		}
    	}
    	
    	// Search to see if auction bid info is required:
	    for (int l = 0; l < messageList.size(); l++) 
	    {
    		String message = messageList.get(l);
    		if (message.length() > 0 && (message.contains("%current-") || message.contains("%auction-bid")) )
    		{
    			replacements.putAll(replacementDefaults.get("b"));
    			if (auction != null) 
    			{
    				AuctionBid currentBid = auction.getCurrentBid();
	        		if (currentBid != null) 
	        		{
	    	    		replacements.put("%current-bid-name%", currentBid.getBidder()); //%B1
	    				replacements.put("%current-bid-display-name%", currentBid.getBidderDisplayName()); //%B2
	    	    		replacements.put("%current-bid-amount%", Functions.formatAmount(currentBid.getBidAmount())); //%B3
	    	    		replacements.put("%auction-bid-starting%", Functions.formatAmount(auction.getStartingBid())); //%B4
	    			}
	        		else 
	    			{
	    				String bidderName = ChatColor.translateAlternateColorCodes('&', AuctionConfig.getLanguageString("auction-info-bidder-noone", auctionScope));
	    				String startingBid = Functions.formatAmount(auction.getStartingBid());
	    	    		replacements.put("%current-bid-name%", bidderName); //%B1
	    				replacements.put("%current-bid-display-name%", bidderName); //%B2
	    	    		replacements.put("%current-bid-amount%", startingBid); //%B3
	    	    		replacements.put("%auction-bid-starting%", startingBid); //%B4
	    			}
    			}
    			break;
	    	}
		}

		// Search to see if auction lot info is required:
    	for (int l = 0; l < messageList.size(); l++) 
    	{
    		String message = messageList.get(l);
    		if (message.length() > 0 && message.contains("%item-")) 
    		{
    			replacements.putAll(replacementDefaults.get("l"));
    			if (auction != null) 
    			{
    		    	lot = auction.getLotType();
    				if (lot != null) 
    				{
    		    		replacements.put("%item-material-name%", Items.getItemName(lot)); //%L1
    					replacements.put("%item-display-name%", Items.getDisplayName(lot)); //%L2
    		    		if (replacements.get("%item-display-name%") == null || replacements.get("%item-display-name%").isEmpty()) 
    		    		{
    		    			replacements.put("%item-display-name%", replacements.get("%item-material-name%"));
    		    		}
    		    		if (Items.getFireworkPower(lot) != null) 
    		    		{
        		    		replacements.put("%item-firework-power%", Integer.toString(Items.getFireworkPower(lot))); //%L3
    		    		}
    		    		if (Items.getBookAuthor(lot) != null) 
    		    		{
        		    		replacements.put("%item-book-author%", Items.getBookAuthor(lot)); //%L4
    		    		}
    		    		if (Items.getBookTitle(lot) != null) 
    		    		{
        		    		replacements.put("%item-book-title%", Items.getBookTitle(lot)); //%L5
    		    		}
    		    		if (lot.getType().getMaxDurability() > 0) 
    		    		{
    				        DecimalFormat decimalFormat = new DecimalFormat("#%");
    				        replacements.put("%item-durability-left%", decimalFormat.format((1 - ((double) lot.getDurability() / (double) lot.getType().getMaxDurability())))); //%L6
    					}
    	        		Map<Enchantment, Integer> enchantments = lot.getEnchantments();
    	        		if (enchantments == null || enchantments.size() == 0) 
    	        		{
    	        			enchantments = Items.getStoredEnchantments(lot);
    	        		}
    	        		if (enchantments != null) 
    	        		{
        					String enchantmentList = "";
        					String enchantmentSeparator = ChatColor.translateAlternateColorCodes('&', AuctionConfig.getLanguageString("auction-info-enchantment-separator", auctionScope));
        	        		for (Entry<Enchantment, Integer> enchantment : enchantments.entrySet()) 
        	        		{
        	        			if (!enchantmentList.isEmpty()) enchantmentList += enchantmentSeparator;
        	        			enchantmentList += Items.getEnchantmentName(enchantment);
        	        		}
        	        		if (enchantmentList.isEmpty()) enchantmentList = ChatColor.translateAlternateColorCodes('&', AuctionConfig.getLanguageString("auction-info-enchantment-none", auctionScope));
        	        		replacements.put("%item-enchantments%", enchantmentList); //%L7
    	        		}
    				}
    			}
	    		break;
			}
    	}

    	
		// Search to see if player info is required:
    	for (int l = 0; l < messageList.size(); l++) 
    	{
    		String message = messageList.get(l);
    		if (message.length() > 0 && message.contains("%auction-prep")) 
    		{
    			replacements.putAll(replacementDefaults.get("p"));
    			if (player != null) 
    			{
	    			String playerName = player.getName();
	    			
	    			String[] defaultStartArgs = Functions.mergeInputArgs(playerName, new String[] {}, false);
					if (defaultStartArgs[0].equalsIgnoreCase("this") || defaultStartArgs[0].equalsIgnoreCase("hand")) 
					{
						replacements.put("%auction-prep-amount-other%", ChatColor.translateAlternateColorCodes('&', AuctionConfig.getLanguageString("prep-amount-in-hand", auctionScope))); //%P1
					} 
					else if (defaultStartArgs[0].equalsIgnoreCase("all")) 
					{
						replacements.put("%auction-prep-amount-other%", ChatColor.translateAlternateColorCodes('&', AuctionConfig.getLanguageString("prep-all-of-this-kind", auctionScope))); //%P1
					} 
					else 
					{
						replacements.put("%auction-prep-amount-other%", ChatColor.translateAlternateColorCodes('&', AuctionConfig.getLanguageString("prep-qty-of-this-kind", auctionScope))); //%P1
					}
					
					replacements.put("%auction-prep-amount-other%", defaultStartArgs[0]); //%P2
		    		replacements.put("%auction-prep-price-formatted%", Functions.formatAmount(Double.parseDouble(defaultStartArgs[1]))); //%P3
		    		replacements.put("%auction-prep-price%", defaultStartArgs[1]); //%P4
		    		replacements.put("%auction-prep-increment-formatted%", Functions.formatAmount(Double.parseDouble(defaultStartArgs[2]))); //%P5
		    		replacements.put("%auction-prep-increment%", defaultStartArgs[2]); //%P6
		    		replacements.put("%auction-prep-time-formatted%", Functions.formatTime(Integer.parseInt(defaultStartArgs[3]), auctionScope)); //%P7
		    		replacements.put("%auction-prep-time%", defaultStartArgs[3]); //%P8
		    		replacements.put("%auction-prep-buynow-formatted%", Functions.formatAmount(Double.parseDouble(defaultStartArgs[4]))); //%P9
		    		replacements.put("%auction-prep-buynow%", defaultStartArgs[4]); //%P0
    			}
    			break;
    		}
    	}
		
		// Search to see if scope info is required:
    	for (int l = 0; l < messageList.size(); l++) 
    	{
    		String message = messageList.get(l);
    		if (message.length() > 0 && message.contains("%player-auction-queue") || message.contains("%auction-")) 
    		{
    			replacements.putAll(replacementDefaults.get("s"));
    			if (auctionScope != null) 
    			{
        			if (player != null) 
        			{
        				replacements.put("%player-auction-queue-position%", Integer.toString(auctionScope.getQueuePosition(player.getName()))); //%S1
        			}
    				replacements.put("%auction-queue-length%", Integer.toString(auctionScope.getAuctionQueueLength())); //%S2
    	    		replacements.put("%auction-scope-name%", auctionScope.getName()); //%S3
    	    		replacements.put("%auction-scope-id%", auctionScope.getScopeId()); //%S4
    			}
    			break;
    		}
    	}
		
		// Search to see if conditionals are required:
    	Map<String, Boolean> conditionals = new HashMap<String, Boolean>();
    	for (int l = 0; l < messageList.size(); l++) 
    	{
    		String message = messageList.get(l);
    		if (message.length() > 0 && (message.contains("%conditional-true%") || message.contains("%conditional-false%"))) //%C    %N
    		{
    	    	conditionals.put("%is-admin%", player != null && FloAuction.perms.has(player, "auction.admin")); //1
    	    	conditionals.put("%can-start%", player != null && FloAuction.perms.has(player, "auction.start")); //2
    	    	conditionals.put("%can-bid%", player != null && FloAuction.perms.has(player, "auction.bid")); //3
    	    	conditionals.put("%has-enchantment%", lot != null && lot.getEnchantments() != null && lot.getEnchantments().size() > 0); //4
    	    	conditionals.put("%has-enchantment%", lot != null && lot.getEnchantments() != null && lot.getEnchantments().size() > 0); //5
    	    	conditionals.put("%is-sealed%", auction != null && auction.sealed); //6
    	    	conditionals.put("%not-sealed%", auction != null && !auction.sealed && auction.getCurrentBid() != null); //7
    	    	conditionals.put("%is-broadcast%", isBroadcast); //8
    	    	conditionals.put("%has-book-title%", lot != null && Items.getBookTitle(lot) != null && !Items.getBookTitle(lot).isEmpty()); //9
    	    	conditionals.put("%has-book-author%", lot != null && Items.getBookAuthor(lot) != null && !Items.getBookAuthor(lot).isEmpty()); //0
    	    	conditionals.put("%item-has-lore%", lot != null && Items.getLore(lot) != null && Items.getLore(lot).length > 0); //A
    	    	conditionals.put("%has-durability%", lot != null && lot.getType().getMaxDurability() > 0 && lot.getDurability() > 0); //B
    	    	conditionals.put("%is-firework%", lot != null && (lot.getType() == Material.FIREWORK || lot.getType() == Material.FIREWORK_CHARGE)); //C
    	    	conditionals.put("%is-buynow%", auction != null && auction.getBuyNow() != 0); //D
    	    	conditionals.put("%has-enchantments%", lot != null && ((lot.getEnchantments() != null && lot.getEnchantments().size() > 0) || (Items.getStoredEnchantments(lot) != null && Items.getStoredEnchantments(lot).size() > 0))); //E
    	    	conditionals.put("%allow-max-bids%", AuctionConfig.getBoolean("allow-max-bids", auctionScope)); //F
    	    	conditionals.put("%allow-buynow%", AuctionConfig.getBoolean("allow-buynow", auctionScope)); //G
    	    	conditionals.put("%allow-auto-bid%", AuctionConfig.getBoolean("allow-auto-bid", auctionScope)); //H
    	    	conditionals.put("%allow-early-bid%", AuctionConfig.getBoolean("allow-early-end", auctionScope)); //I
    	    	conditionals.put("%cancel-prevention-percent%", AuctionConfig.getInt("cancel-prevention-percent", auctionScope) < 100); //J
    	    	conditionals.put("%allow-unsealed-auctions%", AuctionConfig.getBoolean("allow-unsealed-auctions", auctionScope)); //K
    	    	conditionals.put("%allow-sealed-auctions%", AuctionConfig.getBoolean("allow-sealed-auctions", auctionScope)); //L
    	    	conditionals.put("%is-item-logic%", conditionals.get("%allow-unsealed-auctions%") || conditionals.get("%allow-sealed-auctions%")); //L or K
    	    	conditionals.put("%get-active-auction%", auctionScope != null && auctionScope.getActiveAuction() != null); //N
    	    	conditionals.put("%item-is-in-queue%", auctionScope != null && auctionScope.getAuctionQueueLength() > 0); //O
    			break;
    		}
    	}
    	
		// Apply replacements and duplicate/remove rows that need it.
    	for (int l = 0; l < messageList.size(); l++) 
    	{
    		String message = ChatColor.translateAlternateColorCodes('&', messageList.get(l));
    		
    		if (message.length() > 0) 
    		{
        		// Remove conditional sections.
        		if (message.contains("%conditional-true%") || message.contains("%conditional-false%")) 
        		{
        			for (Entry<String, Boolean> conditional : conditionals.entrySet()) 
        			{
        				if (message.length() > 0) 
        				{
        					message = parseConditionals(message, conditional.getKey(), conditional.getValue());
        				}
        			}
        		}
        		
        		// Make standard replacements.
        		for (Map.Entry<String, String> replacementEntry : replacements.entrySet()) 
        		{
        			message = message.replace(replacementEntry.getKey(), replacementEntry.getValue());
        		}
        		
        		// Only one repeatable can be processed per line.
        		if (message.contains("%repeatable")) 
        		{
        			// Mental note: I'm not caching these because there is no reason to use them more than once per message.
            		if (message.contains("%repeatable-enchantments%")) // Enchantments
            		{ 
            			if (lot != null) 
            			{
            				// Stored enchantments and regular ones are treated identically.
            				Map<Enchantment, Integer> enchantments = lot.getEnchantments();
            				if (enchantments == null) 
            				{
            					enchantments = Items.getStoredEnchantments(lot);
            				} 
            				else 
            				{
            					Map<Enchantment, Integer> storedEnchantments = Items.getStoredEnchantments(lot);
            					if (storedEnchantments != null) 
            					{
            						enchantments.putAll(storedEnchantments);
            					}
            				}
            				if (enchantments != null && enchantments.size() > 0) 
            				{
            					for (Map.Entry<Enchantment, Integer> enchantmentEntry : enchantments.entrySet()) 
            					{
            						if (message.length() > 0) 
            						{
            							newMessageList.add(chatPrep(message, auctionScope).replace("%repeatable-enchantment%", Items.getEnchantmentName(enchantmentEntry)));
            						}
            					}
            				}
            			}
            		} 
            		else if (message.contains("%repeatable-firework-payload%")) 
            		{ // Firework aspects
    					FireworkEffect[] payloads = Items.getFireworkEffects(lot);
    					if (payloads != null && payloads.length > 0) 
    					{
    						for (int j = 0; j < payloads.length; j++) 
    						{
    							FireworkEffect payload = payloads[j];
    							// %A lists all aspects of the payload
    							
    							String payloadAspects = "";
    							String payloadSeparator = ChatColor.translateAlternateColorCodes('&', AuctionConfig.getLanguageString("auction-info-payload-separator", auctionScope));
    							
    							Type type = payload.getType();
    							if (type != null) 
    							{
    								if (!payloadAspects.isEmpty()) payloadAspects += payloadSeparator;
    								String fireworkShape = AuctionConfig.getLanguageString("firework-shapes." + type.toString(), auctionScope);
    								if (fireworkShape == null) 
    								{
    									payloadAspects += type.toString();
    								} 
    								else 
    								{
    									payloadAspects += ChatColor.translateAlternateColorCodes('&', fireworkShape);
    								}
    							}
    							List<Color> colors = payload.getColors();
    							for (int k = 0; k < colors.size(); k++) 
    							{
    								if (!payloadAspects.isEmpty())
    								{
    									payloadAspects += payloadSeparator;
    								}
    								Color color = colors.get(k);
    								String colorRGB = color.toString().replace("Color:[rgb0x", "").replace("]", "");
    								String fireworkColor = AuctionConfig.getLanguageString("firework-colors." + colorRGB, auctionScope);
    								if (fireworkColor == null) 
    								{
    									payloadAspects += "#" + colorRGB;
    								} 
    								else 
    								{
    									payloadAspects += ChatColor.translateAlternateColorCodes('&', fireworkColor);
    								}
    							}
    							if (payload.hasFlicker()) 
    							{
    								if (!payloadAspects.isEmpty()) 
    								{
    									payloadAspects += payloadSeparator;
    								}
    								
    								payloadAspects += ChatColor.translateAlternateColorCodes('&', AuctionConfig.getLanguageString("firework-twinkle", auctionScope));
    							}
    							if (payload.hasTrail()) 
    							{
    								if (!payloadAspects.isEmpty())
    								{
    									payloadAspects += payloadSeparator;
    								}
    								payloadAspects += ChatColor.translateAlternateColorCodes('&', AuctionConfig.getLanguageString("firework-trail", auctionScope));
    							}
    							if (message.length() > 0)
    							{
    								newMessageList.add(chatPrep(message, auctionScope).replace("%repeatable-firework-payload%", payloadAspects));
    							}
    						}
    						continue;
    					}
            		} 
            		else if (message.contains("%repeatable-lore%")) 
            		{
        				if (auction != null) 
        				{
    	    				String[] lore = Items.getLore(lot);
    	    				for (int j = 0; j < lore.length; j++) 
    	    				{
    	    					if (message.length() > 0)
    	    					{
    	    						newMessageList.add(chatPrep(message, auctionScope).replace("%repeatable-lore%", lore[j]));
    	    					}
    	    				}
        				}
            		}
        		} else 
        		{
        			if (message.length() > 0) 
        			{
        				newMessageList.add(chatPrep(message, auctionScope));
        			}
        		}
    		}
    	}

    	if(newMessageList != null)
    	{
    		if(FloAuction.placeHolderApiEnabled)
    		{
    			for(int i = 0; i < newMessageList.size(); i++)
    			{
    				newMessageList.set(i, PlaceholderAPI.setPlaceholders(player, newMessageList.get(i)));
    			}
    		}
    	}

    	return newMessageList;
	}

	/**
	 * Removes sections wrapped by keys if condition is false or removes just the keys if the condition is true and removes the negative key if false.
	 * 
	 * @param message the original unparsed message
	 * @param conditionalNumber the number of the key to search for
	 * @param condition whether or not to remove sections wrapped by key
	 * @return message with condition parsed
	 */
	private String parseConditionals(String message, String conditionalNumber, boolean condition) 
	{
		message = parseConditional(message, "%conditional-true%" + conditionalNumber, condition); //%C
		message = parseConditional(message, "%conditional-false%" + conditionalNumber, !condition); //%N
		return message;
	}
	/**
	 * Removes sections wrapped by keys if condition is false or removes just the keys if the condition is true.
	 * 
	 * @param message the original unparsed message
	 * @param conditionalKey the key to search for
	 * @param condition whether or not to remove sections wrapped by key
	 * @return message with condition parsed
	 */
	private String parseConditional(String message, String conditionalKey, boolean condition) 
	{
		if (!message.contains(conditionalKey)) 
		{
			return message;
		}
		if (condition) 
		{
			message = message.replace(conditionalKey, "");
		} 
		else 
		{
			String[] parts = message.split(conditionalKey);
			message = "";
			for (int t = 0; t < parts.length; t++) 
			{
				if (t % 2 == 0) message += parts[t];
			}
		}
		return message;
	}
}