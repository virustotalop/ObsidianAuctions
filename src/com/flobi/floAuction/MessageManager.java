package com.flobi.floAuction;

import org.bukkit.entity.Player;

public abstract class MessageManager {
	public abstract void sendPlayerMessage(Player player, Auction auction, String messageKey);
	
	public abstract void broadcastAuctionMessage(String messageKey, Auction auction);
}
