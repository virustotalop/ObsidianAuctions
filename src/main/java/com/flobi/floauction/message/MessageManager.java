package com.flobi.floauction.message;

import com.flobi.floauction.Auction;
import com.flobi.floauction.AuctionScope;

import java.util.List;

public abstract class MessageManager {

	/**
	 * Sends a message to a player.
	 *
	 * @param playerName username of player or null for console
	 * @param auction the auction being referenced if any
	 * @param messageKey message key to be used by floAuction
	 */
	public abstract void sendPlayerMessage(String messageKey, String playerName, Auction auction);

	/**
	 * Sends a message to a player.
	 * 
	 * @param playerName username of player or null for console
	 * @param auction the auction being referenced if any
	 * @param messageKeys list of message keys used by floAuction 
	 */
	public abstract void sendPlayerMessage(List<String> messageKeys, String playerName, Auction auction);

	/**
	 * Sends a message to a player.
	 *
	 * @param messageKey message key used by floAuction
	 * @param playerName username of player or null for console
	 * @param auctionScope the auction scope being referenced if any
	 */
	public abstract void sendPlayerMessage(String messageKey, String playerName, AuctionScope auctionScope);

	/**
	 * Sends a message to a player.
	 *
	 * @param messageKeys list of message keys used by floAuction
	 * @param playerName username of player or null for console
	 * @param auctionScope the auction scope being referenced if any
	 */
	public abstract void sendPlayerMessage(List<String> messageKeys, String playerName, AuctionScope auctionScope);

	/**
	 * Sends a message to anyone in the scope of a given auction.
	 *
	 * @param messageKey message key to be used by floAuction
	 * @param auction list of message keys used by floAuction
	 */
	public abstract void broadcastAuctionMessage(String messageKey, Auction auction);


	/**
	 * Sends a message to anyone in the scope of a given auction.
	 * 
	 * @param messageKeys message keys to be used by floAuction
	 * @param auction list of message keys used by floAuction
	 */
	public abstract void broadcastAuctionMessage(List<String> messageKeys, Auction auction);

	/**
	 * Send a message to a specific AuctionScope or whole server if scope is null.
	 *
	 * @param messageKey message key to be used by floAuction
	 * @param auctionScope scope to send message or null to broadcast to all
	 */
	public abstract void broadcastAuctionScopeMessage(String messageKey, AuctionScope auctionScope);

	/**
	 * Send a message to a specific AuctionScope or whole server if scope is null.
	 * 
	 * @param messageKeys list of message keys used by floAuction
	 * @param auctionScope scope to send message or null to broadcast to all
	 */
	public abstract void broadcastAuctionScopeMessage(List<String> messageKeys, AuctionScope auctionScope);
}
