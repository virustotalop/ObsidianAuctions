package com.flobi.floAuction;

import java.util.Arrays;
import java.util.List;

import org.bukkit.entity.Player;

public class AuctionMessageManager extends MessageManager {
	public AuctionMessageManager() {
		
	}
	
	public void sendPlayerMessage(Player player, Auction auction, String messageKey) {
		
	}
	
	public void broadcastAuctionMessage(String messageKey, Auction auction) {
		
	}
	
	private String getMessageTemplate(String messageKey, AuctionScope auctionScope) {
		return "";
	}
	
	protected static class tokenProcessor {
		private static List<String> generalTokens = Arrays.asList("%c", "%C");
		private static List<String> auctionScopeTokens = Arrays.asList("%k", "%x");
		private static List<String> auctionTokens = Arrays.asList("%q", "%i", "%d", "%s", "%n", "%b", "%B", "%f", "%h", "%t", "%D", "%E", "%F", "%r", "%X", "%o", "%O");
		private static List<String> playerTokens = Arrays.asList("%Q", "%u", "%U", "%v", "%V", "%w", "%W", "%z", "%Z", "%g", "%G");
		private static List<String> specialTokens = Arrays.asList("%A", "%a");
		

		public String processTokens(String message, AuctionScope auctionScope, Auction auction, Player player) {
			if (auctionScope == null) {
				for (String auctionScopeToken : auctionScopeTokens) {
					message.replace(auctionScopeToken, "-");
				}
			} else {
				for (String auctionScopeToken : auctionScopeTokens) {
					if (message.contains(auctionScopeToken)) {
						message = processAuctionScopeTokens(message, auctionScope);
						break;
					}
				}
			}
			
			if (auction == null) {
				for (String auctionToken : auctionTokens) {
					message.replace(auctionToken, "-");
				}
			} else {
				for (String auctionToken : auctionTokens) {
					if (message.contains(auctionToken)) {
						message = processAuctionTokens(message, auction);
						break;
					}
				}
			}
			
			if (player == null) {
				for (String playerToken : playerTokens) {
					message.replace(playerToken, "-");
				}
			} else {
				for (String playerToken : playerTokens) {
					if (message.contains(playerToken)) {
						message = processPlayerTokens(message, player);
						break;
					}
				}
			}
			
			return message.replace("%%", "%");
		}
		
		private String processAuctionScopeTokens(String message, AuctionScope auctionScope) {
			return message;
		}
		
		private String processAuctionTokens(String message, Auction auction) {
			return message;
		}
		
		private String processPlayerTokens(String message, Player player) {
			return message;
		}
	}
}
