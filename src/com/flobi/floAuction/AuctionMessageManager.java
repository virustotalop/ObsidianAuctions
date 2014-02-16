package com.flobi.floAuction;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
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
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.flobi.utility.functions;
import com.flobi.utility.items;

public class AuctionMessageManager extends MessageManager {
	public AuctionMessageManager() {
		// The only reason this isn't just static methods is because it needs to be passed into the auction.
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
    private static void sendMessage(List<String> messageKeys, CommandSender player, AuctionScope auctionScope, boolean fullBroadcast) {
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
		    	if (!fullBroadcast && floAuction.getVoluntarilyDisabledUsers().indexOf(player.getName()) != -1) {
		    		// Don't send this user any messages.
		    		return;
				}
		    	playerName = player.getName();
	    	} else {
		    	if (!fullBroadcast && floAuction.getVoluntarilyDisabledUsers().indexOf("*console*") != -1) {
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
        		displayName = ChatColor.translateAlternateColorCodes('&', AuctionConfig.getLanguageString("wrap-display-name", auctionScope)).replace("%i", displayName) + ChatColor.translateAlternateColorCodes('&', "&r");
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
				            	floAuction.log(player, message);
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
			            	floAuction.log(player, message);
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
			            	floAuction.log(player, message);
		        		}
	    			}
	    			continue;
				}
	
				if (fullBroadcast) {
		    		broadcastMessage(message, auctionScope);
		    	} else if (player != null) {
			    	player.sendMessage(message);
		    	}
		    	floAuction.log(player, message);
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
        	if (floAuction.getVoluntarilyDisabledUsers().contains(player.getName())) continue;
    		if (auctionScope != null && !auctionScope.equals(AuctionScope.getPlayerScope(player))) continue;
    		player.sendMessage(message);
    	}
    	
    	if (auctionScope == null && floAuction.getVoluntarilyDisabledUsers().indexOf("*console*") == -1) {
			Bukkit.getConsoleSender().sendMessage(message);
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
    	message = AuctionConfig.getLanguageString("chat-prefix", auctionScope) + message;
    	message = ChatColor.translateAlternateColorCodes('&', message);
    	return message;
    }
}
