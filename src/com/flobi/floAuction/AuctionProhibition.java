package com.flobi.floAuction;

import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class AuctionProhibition {
	private Plugin prohibiterPlugin = null;
	private String playerName = null;
	private String enableMessage = null;
	private String reminderMessage = null;
	private String disableMessage = null;

	private static ArrayList<AuctionProhibition> involuntarilyDisabledUsers = new ArrayList<AuctionProhibition>();

	private static AuctionProhibition getProhibition(Plugin prohibiterPlugin, String playerName) {
		for (int i = 0; i < involuntarilyDisabledUsers.size(); i++) {
			AuctionProhibition auctionProhibition = involuntarilyDisabledUsers.get(i);
			if (auctionProhibition.playerName.equalsIgnoreCase(playerName) && auctionProhibition.prohibiterPlugin.equals(prohibiterPlugin)) {
				return auctionProhibition;
			}
		}
		return null;
	}
	
	private static AuctionProhibition getProhibition(String playerName) {
		for (int i = 0; i < involuntarilyDisabledUsers.size(); i++) {
			AuctionProhibition auctionProhibition = involuntarilyDisabledUsers.get(i);
			if (auctionProhibition.playerName.equalsIgnoreCase(playerName)) {
				return auctionProhibition;
			}
		}
		return null;
	}

	public static boolean isOnProhibition(String playerName, boolean sendReminderMessage) {
		AuctionProhibition auctionProhibition = AuctionProhibition.getProhibition(playerName);
		if (auctionProhibition != null) {
			if (sendReminderMessage) {
				Player player = Bukkit.getPlayer(playerName);
				if (player == null) return true;
				if (auctionProhibition.reminderMessage == null) {
					// Send stock message.
					floAuction.sendMessage("remote-plugin-prohibition-reminder", playerName, AuctionScope.getPlayerScope(player));
				} else {
					player.sendMessage(auctionProhibition.reminderMessage);
				}
			}
			return true;
		} else {
			return false;
		}
	}

	public static boolean isOnProhibition(Plugin prohibiterPlugin, String playerName, boolean sendReminderMessage) {
		AuctionProhibition auctionProhibition = AuctionProhibition.getProhibition(prohibiterPlugin, playerName);
		if (auctionProhibition != null) {
			if (sendReminderMessage) {
				Player player = Bukkit.getPlayer(playerName);
				if (player == null) return true;
				if (auctionProhibition.reminderMessage == null) {
					// Send stock message.
					floAuction.sendMessage("remote-plugin-prohibition-reminder", playerName, AuctionScope.getPlayerScope(player));
				} else {
					player.sendMessage(auctionProhibition.reminderMessage);
				}
			}
			return true;
		} else {
			return false;
		}
	}

	public static boolean prohibitPlayer(Plugin prohibiterPlugin, String playerName) {
		return prohibitPlayer(prohibiterPlugin, playerName, null, null, null);
	}
	
	public static boolean prohibitPlayer(Plugin prohibiterPlugin, String playerName, String enableMessage, String reminderMessage, String disableMessage) {
		if (AuctionParticipant.isParticipating(playerName)) return false;
		
		if (AuctionProhibition.isOnProhibition(prohibiterPlugin, playerName, false)) return true;
		
		if (AuctionProhibition.getProhibition(playerName) != null) {
			AuctionProhibition.prohibitPlayer(prohibiterPlugin, playerName, disableMessage, reminderMessage, enableMessage);
			return true;
		}
		
		AuctionProhibition.prohibitPlayer(prohibiterPlugin, playerName, disableMessage, reminderMessage, enableMessage);
		
		Player player = Bukkit.getPlayer(playerName);
		if (player == null) return true;
		if (enableMessage == null) {
			// Send stock message.
			floAuction.sendMessage("remote-plugin-prohibition-enabled", playerName, AuctionScope.getPlayerScope(player));
		} else {
			player.sendMessage(enableMessage);
		}
		return true;
	}

	public static boolean removeProhibition(Plugin prohibiterPlugin, String playerName) {
		Player player = Bukkit.getPlayer(playerName);
		AuctionScope auctionScope = AuctionScope.getPlayerScope(player);
		for (int i = 0; i < involuntarilyDisabledUsers.size(); i++) {
			AuctionProhibition auctionProhibition = involuntarilyDisabledUsers.get(i);
			if (auctionProhibition.playerName.equalsIgnoreCase(playerName) && auctionProhibition.prohibiterPlugin.equals(prohibiterPlugin)) {
				if (player != null) {
					if (auctionProhibition.disableMessage == null) {
						// Send stock message.
						floAuction.sendMessage("remote-plugin-prohibition-disabled", playerName, auctionScope);
					} else {
						player.sendMessage(auctionProhibition.disableMessage);
					}
				}
				involuntarilyDisabledUsers.remove(i);
				i--;
			}
		}
		
		AuctionProhibition auctionProhibition = getProhibition(playerName);
		if (auctionProhibition != null) {
			if (player != null) {
				if (auctionProhibition.enableMessage == null) {
					// Send stock message.
					floAuction.sendMessage("remote-plugin-prohibition-enabled", playerName, auctionScope);
				} else {
					player.sendMessage(auctionProhibition.enableMessage);
				}
			}
		}
		
		return true;
	}
	
	private AuctionProhibition(Plugin prohibiterPlugin, String playerName, String enableMessage, String reminderMessage, String disableMessage) {
		this.prohibiterPlugin = prohibiterPlugin;
		this.playerName = playerName;
		this.enableMessage = enableMessage;
		this.reminderMessage = reminderMessage;
		this.disableMessage = disableMessage;
	}
}
