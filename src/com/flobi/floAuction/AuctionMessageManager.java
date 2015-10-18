package com.flobi.floAuction;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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

import com.flobi.floAuction.utilities.Functions;
import com.flobi.floAuction.utilities.Items;

public class AuctionMessageManager extends MessageManager {
	private static Map<String, Map<String, String>> replacementDefaults = new HashMap<String, Map<String, String>>();
	public AuctionMessageManager() {
		Map<String, String> aReplacments = new HashMap<String, String>();
		aReplacments.put("%A1", "-");
		aReplacments.put("%A2", "-");
		aReplacments.put("%A3", "-");
		aReplacments.put("%A4", "-");
		aReplacments.put("%A5", "-");
		aReplacments.put("%A6", "-");
		aReplacments.put("%A7", "-");
		aReplacments.put("%A8", "-");
		aReplacments.put("%A9", "-");
		replacementDefaults.put("a", aReplacments);
		
		Map<String, String> bReplacments = new HashMap<String, String>();
		bReplacments.put("%B1", "-");
		bReplacments.put("%B2", "-");
		bReplacments.put("%B3", "-");
		bReplacments.put("%B4", "-");
		replacementDefaults.put("b", bReplacments);
		
		Map<String, String> lReplacments = new HashMap<String, String>();
		lReplacments.put("%L1", "-");
		lReplacments.put("%L2", "-");
		lReplacments.put("%L3", "-");
		lReplacments.put("%L4", "-");
		lReplacments.put("%L5", "-");
		lReplacments.put("%L6", "-");
		lReplacments.put("%L7", "-");
		replacementDefaults.put("l", lReplacments);
		
		Map<String, String> pReplacments = new HashMap<String, String>();
		pReplacments.put("%P1", "-");
		pReplacments.put("%P2", "-");
		pReplacments.put("%P3", "-");
		pReplacments.put("%P4", "-");
		pReplacments.put("%P5", "-");
		pReplacments.put("%P6", "-");
		pReplacments.put("%P7", "-");
		pReplacments.put("%P8", "-");
		pReplacments.put("%P9", "-");
		pReplacments.put("%P0", "-");
		replacementDefaults.put("p", pReplacments);
		
		Map<String, String> sReplacments = new HashMap<String, String>();
		sReplacments.put("%S1", "-");
		sReplacments.put("%S2", "-");
		sReplacments.put("%S3", "-");
		sReplacments.put("%S4", "-");
		replacementDefaults.put("s", sReplacments);
	}
	
	public void sendPlayerMessage(List<String> messageKeys, String playerName, Auction auction) {
		CommandSender recipient = null;
		if (playerName == null) {
			recipient = Bukkit.getConsoleSender();
		} else {
			recipient = Bukkit.getPlayer(playerName);
		}
		AuctionScope auctionScope = null;
		if (auction != null) auctionScope = auction.getScope();
		if (auctionScope == null && recipient instanceof Player) auctionScope = AuctionScope.getPlayerScope((Player) recipient);
		sendMessage(messageKeys, recipient, auctionScope, false);
	}
	
	public void sendPlayerMessage(List<String> messageKeys, String playerName, AuctionScope auctionScope) {
		CommandSender recipient = null;
		if (playerName == null) {
			recipient = Bukkit.getConsoleSender();
		} else {
			recipient = Bukkit.getPlayer(playerName);
		}
		if (auctionScope == null && recipient instanceof Player) auctionScope = AuctionScope.getPlayerScope((Player) recipient);
		sendMessage(messageKeys, recipient, auctionScope, false);
	}
	
	public void broadcastAuctionMessage(List<String> messageKeys, Auction auction) {
		if (auction == null) return;
		AuctionScope auctionScope = auction.getScope();
		sendMessage(messageKeys, null, auctionScope, true);
	}
	
	public void broadcastAuctionScopeMessage(List<String> messageKeys, AuctionScope auctionScope) {
		sendMessage(messageKeys, null, auctionScope, true);
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
    	
    	if (auctionScope != null) {
    		auction = auctionScope.getActiveAuction();
    	}
    	
    	if (sender != null) {
	    	if (sender instanceof Player) {
	    		player = (Player) sender;
		    	if (!fullBroadcast && floAuction.getVoluntarilyDisabledUsers().indexOf(player.getName()) != -1) {
		    		// Don't send this user any messages.
		    		return;
				}
	    	} else {
		    	if (!fullBroadcast && floAuction.getVoluntarilyDisabledUsers().indexOf("*console*") != -1) {
		    		// Don't send console any messages.
		    		return;
				}
	    	}
    	}
    	
    	List<String> messages = parseMessages(messageKeys, auctionScope, auction, player, fullBroadcast);

		if (fullBroadcast) {
    		broadcastMessage(messages, auctionScope);
    	} else if (player != null) {
	    	for (String message : messages) {
//				sendTellRaw(player.getName(), message);
	    		player.sendMessage(message);
	    		floAuction.log(player.getName(), message, auctionScope);
	    	}
    	} else if (sender != null) {
    		ConsoleCommandSender console = Bukkit.getConsoleSender();
	    	for (String message : messages) {
	    		console.sendMessage(ChatColor.stripColor(message));
	    		floAuction.log("CONSOLE", message, auctionScope);
	    	}
    	} else {
	    	for (String message : messages) {
	    		floAuction.log("NO TARGET!", message, auctionScope);
	    	}
    	}
    	
    }
    
    /**
     * Broadcast a message to everyone in an auctionscope.
     * 
     * @param message message to send
     * @param auctionScope scope to send it to
     */
    private static void broadcastMessage(List<String> messages, AuctionScope auctionScope) {
    	Player[] onlinePlayers = Bukkit.getOnlinePlayers();
    	
    	for (Player player : onlinePlayers) {
        	if (floAuction.getVoluntarilyDisabledUsers().contains(player.getName())) continue;
    		if (auctionScope != null && !auctionScope.equals(AuctionScope.getPlayerScope(player))) continue;
//    		sendTellRaw(player.getName(), message);
	    	for (String message : messages) {
	    		player.sendMessage(message);
	    	}
    	}
    	
    	if (auctionScope == null && floAuction.getVoluntarilyDisabledUsers().indexOf("*console*") == -1) {
	    	for (String message : messages) {
	    		message = ChatColor.stripColor(message);
				Bukkit.getConsoleSender().sendMessage(message);
	    	}
		}
    	for (String message : messages) {
    		message = ChatColor.stripColor(message);
    		floAuction.log("BROADCAST", message, auctionScope);
    	}
    }
    
	/**
	 * Prepares chat, prepending prefix and processing colors.
	 * 
	 * @param message message to prepare
	 * @param auctionScope the scope of the destination
	 * @return prepared message
	 */
    private static String chatPrep(String message, AuctionScope auctionScope) {
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
	private List<String> parseMessages(List<String> messageKeys, AuctionScope auctionScope, Auction auction, Player player, boolean isBroadcast) {
		List<String> messageList = new ArrayList<String>();
		
    	for (int l = 0; l < messageKeys.size(); l++) {
    		String messageKey = messageKeys.get(l);
        	if (messageKey == null) {
        		continue;
        	}
    		
	    	List<String> partialMessageList = AuctionConfig.getLanguageStringList(messageKey, auctionScope);
	    	
	    	if (partialMessageList == null || partialMessageList.size() == 0) {
		    	String originalMessage = null;
	    		originalMessage = AuctionConfig.getLanguageString(messageKey, auctionScope);
	    		
	    		if (originalMessage == null || originalMessage.length() == 0) {
	        		continue;
	    		} else {
	    			partialMessageList = Arrays.asList(originalMessage.split("(\r?\n|\r)"));
	    		}
	    	}
	    	
    		messageList.addAll(partialMessageList);
    	}
		
		return parseMessageTokens(messageList, auctionScope, auction, player, isBroadcast);
	}
	
	private List<String> parseMessageTokens(List<String> messageList, AuctionScope auctionScope, Auction auction, Player player, boolean isBroadcast) {
		List<String> newMessageList = new ArrayList<String>();
		Map<String, String> replacements = new HashMap<String, String>();
		ItemStack lot = null;
		
		if (auction == null && auctionScope != null) {
			auction = auctionScope.getActiveAuction();
		}
		
		// Search to see if auction info is required:
    	for (int l = 0; l < messageList.size(); l++) {
    		String message = messageList.get(l);
    		if (message.length() > 0 && message.contains("%A")) {
    			replacements.putAll(replacementDefaults.get("a"));
    			if (auction != null) {
    	    		replacements.put("%A1", auction.getOwner());
    				replacements.put("%A2", auction.getOwnerDisplayName());
    	    		replacements.put("%A3", Integer.toString(auction.getLotQuantity()));
    	    		if (auction.getStartingBid() == 0) {
        	    		replacements.put("%A4", Functions.formatAmount(auction.getMinBidIncrement()));
    	    		} else {
        	    		replacements.put("%A4", Functions.formatAmount(auction.getStartingBid()));
    	    		}
    	    		replacements.put("%A5", Functions.formatAmount(auction.getMinBidIncrement()));
    	    		replacements.put("%A6", Functions.formatAmount(auction.getBuyNow()));
    	    		replacements.put("%A7", Functions.formatTime(auction.getRemainingTime(), auctionScope));
    	    		replacements.put("%A8", Functions.formatAmount(auction.extractedPreTax));
    	    		replacements.put("%A9", Functions.formatAmount(auction.extractedPostTax));
    			}
	    		break;
    		}
    	}
    	
    	// Search to see if auction bid info is required:
	    for (int l = 0; l < messageList.size(); l++) {
    		String message = messageList.get(l);
    		if (message.length() > 0 && message.contains("%B")) {
    			replacements.putAll(replacementDefaults.get("b"));
    			if (auction != null) {
    				AuctionBid currentBid = auction.getCurrentBid();
	        		if (currentBid != null) {
	    	    		replacements.put("%B1", currentBid.getBidder());
	    				replacements.put("%B2", currentBid.getBidderDisplayName());
	    	    		replacements.put("%B3", Functions.formatAmount(currentBid.getBidAmount()));
	    	    		replacements.put("%B4", Functions.formatAmount(auction.getStartingBid()));
	    			} else {
	    				String bidderName = ChatColor.translateAlternateColorCodes('&', AuctionConfig.getLanguageString("auction-info-bidder-noone", auctionScope));
	    				String startingBid = Functions.formatAmount(auction.getStartingBid());
	    	    		replacements.put("%B1", bidderName);
	    				replacements.put("%B2", bidderName);
	    	    		replacements.put("%B3", startingBid);
	    	    		replacements.put("%B4", startingBid);
	    			}
    			}
    			break;
	    	}
		}

		// Search to see if auction lot info is required:
    	for (int l = 0; l < messageList.size(); l++) {
    		String message = messageList.get(l);
    		if (message.length() > 0 && message.contains("%L")) {
    			replacements.putAll(replacementDefaults.get("l"));
    			if (auction != null) {
    		    	lot = auction.getLotType();
    				if (lot != null) {
    		    		replacements.put("%L1", Items.getItemName(lot));
    					replacements.put("%L2", Items.getDisplayName(lot));
    		    		if (replacements.get("%L2") == null || replacements.get("%L2").isEmpty()) {
    		    			replacements.put("%L2", replacements.get("%L1"));
    		    		}
    		    		if (Items.getFireworkPower(lot) != null) {
        		    		replacements.put("%L3", Integer.toString(Items.getFireworkPower(lot)));
    		    		}
    		    		if (Items.getBookAuthor(lot) != null) {
        		    		replacements.put("%L4", Items.getBookAuthor(lot));
    		    		}
    		    		if (Items.getBookTitle(lot) != null) {
        		    		replacements.put("%L5", Items.getBookTitle(lot));
    		    		}
    		    		if (lot.getType().getMaxDurability() > 0) {
    				        DecimalFormat decimalFormat = new DecimalFormat("#%");
    				        replacements.put("%L6", decimalFormat.format((1 - ((double) lot.getDurability() / (double) lot.getType().getMaxDurability()))));
    					}
    	        		Map<Enchantment, Integer> enchantments = lot.getEnchantments();
    	        		if (enchantments == null || enchantments.size() == 0) {
    	        			enchantments = Items.getStoredEnchantments(lot);
    	        		}
    	        		if (enchantments != null) {
        					String enchantmentList = "";
        					String enchantmentSeparator = ChatColor.translateAlternateColorCodes('&', AuctionConfig.getLanguageString("auction-info-enchantment-separator", auctionScope));
        	        		for (Entry<Enchantment, Integer> enchantment : enchantments.entrySet()) {
        	        			if (!enchantmentList.isEmpty()) enchantmentList += enchantmentSeparator;
        	        			enchantmentList += Items.getEnchantmentName(enchantment);
        	        		}
        	        		if (enchantmentList.isEmpty()) enchantmentList = ChatColor.translateAlternateColorCodes('&', AuctionConfig.getLanguageString("auction-info-enchantment-none", auctionScope));
        	        		replacements.put("%L7", enchantmentList);
    	        		}
    				}
    			}
	    		break;
			}
    	}

    	
		// Search to see if player info is required:
    	for (int l = 0; l < messageList.size(); l++) {
    		String message = messageList.get(l);
    		if (message.length() > 0 && message.contains("%P")) {
    			replacements.putAll(replacementDefaults.get("p"));
    			if (player != null) {
	    			String playerName = player.getName();
	    			
	    			String[] defaultStartArgs = Functions.mergeInputArgs(playerName, new String[] {}, false);
					if (defaultStartArgs[0].equalsIgnoreCase("this") || defaultStartArgs[0].equalsIgnoreCase("hand")) {
						replacements.put("%P1", ChatColor.translateAlternateColorCodes('&', AuctionConfig.getLanguageString("prep-amount-in-hand", auctionScope)));
					} else if (defaultStartArgs[0].equalsIgnoreCase("all")) {
						replacements.put("%P1", ChatColor.translateAlternateColorCodes('&', AuctionConfig.getLanguageString("prep-all-of-this-kind", auctionScope)));
					} else {
						replacements.put("%P1", ChatColor.translateAlternateColorCodes('&', AuctionConfig.getLanguageString("prep-qty-of-this-kind", auctionScope)));
					}
					
					replacements.put("%P2", defaultStartArgs[0]);
		    		replacements.put("%P3", Functions.formatAmount(Double.parseDouble(defaultStartArgs[1])));
		    		replacements.put("%P4", defaultStartArgs[1]);
		    		replacements.put("%P5", Functions.formatAmount(Double.parseDouble(defaultStartArgs[2])));
		    		replacements.put("%P6", defaultStartArgs[2]);
		    		replacements.put("%P7", Functions.formatTime(Integer.parseInt(defaultStartArgs[3]), auctionScope));
		    		replacements.put("%P8", defaultStartArgs[3]);
		    		replacements.put("%P9", Functions.formatAmount(Double.parseDouble(defaultStartArgs[4])));
		    		replacements.put("%P0", defaultStartArgs[4]);
    			}
    			break;
    		}
    	}
		
		// Search to see if scope info is required:
    	for (int l = 0; l < messageList.size(); l++) {
    		String message = messageList.get(l);
    		if (message.length() > 0 && message.contains("%S")) {
    			replacements.putAll(replacementDefaults.get("s"));
    			if (auctionScope != null) {
        			if (player != null) replacements.put("%S1", Integer.toString(auctionScope.getQueuePosition(player.getName())));
    				replacements.put("%S2", Integer.toString(auctionScope.getAuctionQueueLength()));
    	    		replacements.put("%S3", auctionScope.getName());
    	    		replacements.put("%S4", auctionScope.getScopeId());
    			}
    			break;
    		}
    	}
		
		// Search to see if conditionals are required:
    	Map<String, Boolean> conditionals = new HashMap<String, Boolean>();
    	for (int l = 0; l < messageList.size(); l++) {
    		String message = messageList.get(l);
    		if (message.length() > 0 && (message.contains("%C") || message.contains("%N"))) {
    	    	conditionals.put("1", player != null && floAuction.perms.has(player, "auction.admin"));
    	    	conditionals.put("2", player != null && floAuction.perms.has(player, "auction.start"));
    	    	conditionals.put("3", player != null && floAuction.perms.has(player, "auction.bid"));
    	    	conditionals.put("4", lot != null && lot.getEnchantments() != null && lot.getEnchantments().size() > 0);
    	    	conditionals.put("5", lot != null && lot.getEnchantments() != null && lot.getEnchantments().size() > 0);
    	    	conditionals.put("6", auction != null && auction.sealed);
    	    	conditionals.put("7", auction != null && !auction.sealed && auction.getCurrentBid() != null);
    	    	conditionals.put("8", isBroadcast);
    	    	conditionals.put("9", lot != null && Items.getBookTitle(lot) != null && !Items.getBookTitle(lot).isEmpty());
    	    	conditionals.put("0", lot != null && Items.getBookAuthor(lot) != null && !Items.getBookAuthor(lot).isEmpty());
    	    	conditionals.put("A", lot != null && Items.getLore(lot) != null && Items.getLore(lot).length > 0);
    	    	conditionals.put("B", lot != null && lot.getType().getMaxDurability() > 0 && lot.getDurability() > 0);
    	    	conditionals.put("C", lot != null && (lot.getType() == Material.FIREWORK || lot.getType() == Material.FIREWORK_CHARGE));
    	    	conditionals.put("D", auction != null && auction.getBuyNow() != 0);
    	    	conditionals.put("E", lot != null && ((lot.getEnchantments() != null && lot.getEnchantments().size() > 0) || (Items.getStoredEnchantments(lot) != null && Items.getStoredEnchantments(lot).size() > 0)));
    	    	conditionals.put("F", AuctionConfig.getBoolean("allow-max-bids", auctionScope));
    	    	conditionals.put("G", AuctionConfig.getBoolean("allow-buynow", auctionScope));
    	    	conditionals.put("H", AuctionConfig.getBoolean("allow-auto-bid", auctionScope));
    	    	conditionals.put("I", AuctionConfig.getBoolean("allow-early-end", auctionScope));
    	    	conditionals.put("J", AuctionConfig.getInt("cancel-prevention-percent", auctionScope) < 100);
    	    	conditionals.put("K", AuctionConfig.getBoolean("allow-unsealed-auctions", auctionScope));
    	    	conditionals.put("L", AuctionConfig.getBoolean("allow-sealed-auctions", auctionScope));
    	    	conditionals.put("M", conditionals.get("K") || conditionals.get("L"));
    	    	conditionals.put("N", auctionScope != null && auctionScope.getActiveAuction() != null);
    	    	conditionals.put("O", auctionScope != null && auctionScope.getAuctionQueueLength() > 0);
    			break;
    		}
    	}
    	
		// Apply replacements and duplicate/remove rows that need it.
    	for (int l = 0; l < messageList.size(); l++) {
    		String message = ChatColor.translateAlternateColorCodes('&', messageList.get(l));
    		
    		if (message.length() > 0) {
        		// Remove conditional sections.
        		if (message.contains("%C") || message.contains("%N")) {
        			for (Entry<String, Boolean> conditional : conditionals.entrySet()) {
        				if (message.length() > 0) message = parseConditionals(message, conditional.getKey(), conditional.getValue());
        			}
        		}
        		
        		// Make standard replacements.
        		for (Map.Entry<String, String> replacementEntry : replacements.entrySet()) {
        			message = message.replace(replacementEntry.getKey(), replacementEntry.getValue());
        		}
        		
        		// Only one repeatable can be processed per line.
        		if (message.contains("%R")) {
        			// Mental note: I'm not caching these because there is no reason to use them more than once per message.
            		if (message.contains("%R1")) { // Enchantments
            			if (lot != null) {
            				// Stored enchantments and regular ones are treated identically.
            				Map<Enchantment, Integer> enchantments = lot.getEnchantments();
            				if (enchantments == null) {
            					enchantments = Items.getStoredEnchantments(lot);
            				} else {
            					Map<Enchantment, Integer> storedEnchantments = Items.getStoredEnchantments(lot);
            					if (storedEnchantments != null) enchantments.putAll(storedEnchantments);
            				}
            				if (enchantments != null && enchantments.size() > 0) {
            					for (Map.Entry<Enchantment, Integer> enchantmentEntry : enchantments.entrySet()) {
            						if (message.length() > 0) newMessageList.add(chatPrep(message, auctionScope).replace("%R1", Items.getEnchantmentName(enchantmentEntry)));
            					}
            				}
            			}
            		} else if (message.contains("%R2")) { // Firework aspects
    					FireworkEffect[] payloads = Items.getFireworkEffects(lot);
    					if (payloads != null && payloads.length > 0) {
    						for (int j = 0; j < payloads.length; j++) {
    							FireworkEffect payload = payloads[j];
    							// %A lists all aspects of the payload
    							
    							String payloadAspects = "";
    							String payloadSeparator = ChatColor.translateAlternateColorCodes('&', AuctionConfig.getLanguageString("auction-info-payload-separator", auctionScope));
    							
    							Type type = payload.getType();
    							if (type != null) {
    								if (!payloadAspects.isEmpty()) payloadAspects += payloadSeparator;
    								String fireworkShape = AuctionConfig.getLanguageString("firework-shapes." + type.toString(), auctionScope);
    								if (fireworkShape == null) {
    									payloadAspects += type.toString();
    								} else {
    									payloadAspects += ChatColor.translateAlternateColorCodes('&', fireworkShape);
    								}
    							}
    							List<Color> colors = payload.getColors();
    							for (int k = 0; k < colors.size(); k++) {
    								if (!payloadAspects.isEmpty()) payloadAspects += payloadSeparator;
    								Color color = colors.get(k);
    								String colorRGB = color.toString().replace("Color:[rgb0x", "").replace("]", "");
    								String fireworkColor = AuctionConfig.getLanguageString("firework-colors." + colorRGB, auctionScope);
    								if (fireworkColor == null) {
    									payloadAspects += "#" + colorRGB;
    								} else {
    									payloadAspects += ChatColor.translateAlternateColorCodes('&', fireworkColor);
    								}
    							}
    							if (payload.hasFlicker()) {
    								if (!payloadAspects.isEmpty()) payloadAspects += payloadSeparator;
    								payloadAspects += ChatColor.translateAlternateColorCodes('&', AuctionConfig.getLanguageString("firework-twinkle", auctionScope));
    							}
    							if (payload.hasTrail()) {
    								if (!payloadAspects.isEmpty()) payloadAspects += payloadSeparator;
    								payloadAspects += ChatColor.translateAlternateColorCodes('&', AuctionConfig.getLanguageString("firework-trail", auctionScope));
    							}
    							if (message.length() > 0) newMessageList.add(chatPrep(message, auctionScope).replace("%R2", payloadAspects));
    						}
    						continue;
    					}
            		} else if (message.contains("%R3")) {
        				if (auction != null) {
    	    				String[] lore = Items.getLore(lot);
    	    				for (int j = 0; j < lore.length; j++) {
    	    					if (message.length() > 0) newMessageList.add(chatPrep(message, auctionScope).replace("%R3", lore[j]));
    	    				}
        				}
            		}
        		} else {
        			if (message.length() > 0) newMessageList.add(chatPrep(message, auctionScope));
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
	private String parseConditionals(String message, String conditionalNumber, boolean condition) {
		message = parseConditional(message, "%C" + conditionalNumber, condition);
		message = parseConditional(message, "%N" + conditionalNumber, !condition);
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
	private String parseConditional(String message, String conditionalKey, boolean condition) {
		if (!message.contains(conditionalKey)) return message;
		if (condition) {
			message = message.replace(conditionalKey, "");
		} else {
			String[] parts = message.split(conditionalKey);
			message = "";
			for (int t = 0; t < parts.length; t++) {
				if (t % 2 == 0) message += parts[t];
			}
		}
		return message;
	}

}
