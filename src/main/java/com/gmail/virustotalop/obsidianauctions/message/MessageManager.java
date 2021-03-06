package com.gmail.virustotalop.obsidianauctions.message;

import com.gmail.virustotalop.obsidianauctions.auction.Auction;
import com.gmail.virustotalop.obsidianauctions.auction.AuctionScope;

import java.util.List;
import java.util.UUID;

public abstract class MessageManager {

    /**
     * Sends a message to a player.
     *
     * @param playerUUID uuid of player or null for console
     * @param auction    the auction being referenced if any
     * @param messageKey message key to be used by floAuction
     */
    public abstract void sendPlayerMessage(String messageKey, UUID playerUUID, Auction auction);

    /**
     * Sends a message to a player.
     *
     * @param playerUUID  uuid of player or null for console
     * @param auction     the auction being referenced if any
     * @param messageKeys list of message keys used by floAuction
     */
    public abstract void sendPlayerMessage(List<String> messageKeys, UUID playerUUID, Auction auction);

    /**
     * Sends a message to a player.
     *
     * @param messageKey   message key used by floAuction
     * @param playerUUID   uuid of player or null for console
     * @param auctionScope the auction scope being referenced if any
     */
    public abstract void sendPlayerMessage(String messageKey, UUID playerUUID, AuctionScope auctionScope);

    /**
     * Sends a message to a player.
     *
     * @param messageKeys  list of message keys used by floAuction
     * @param playerUUID   uuid of player or null for console
     * @param auctionScope the auction scope being referenced if any
     */
    public abstract void sendPlayerMessage(List<String> messageKeys, UUID playerUUID, AuctionScope auctionScope);

    /**
     * Sends a message to anyone in the scope of a given auction.
     *
     * @param messageKey message key to be used by floAuction
     * @param auction    list of message keys used by floAuction
     */
    public abstract void broadcastAuctionMessage(String messageKey, Auction auction);


    /**
     * Sends a message to anyone in the scope of a given auction.
     *
     * @param messageKeys message keys to be used by floAuction
     * @param auction     list of message keys used by floAuction
     */
    public abstract void broadcastAuctionMessage(List<String> messageKeys, Auction auction);

    /**
     * Send a message to a specific AuctionScope or whole server if scope is null.
     *
     * @param messageKey   message key to be used by floAuction
     * @param auctionScope scope to send message or null to broadcast to all
     */
    public abstract void broadcastAuctionScopeMessage(String messageKey, AuctionScope auctionScope);

    /**
     * Send a message to a specific AuctionScope or whole server if scope is null.
     *
     * @param messageKeys  list of message keys used by floAuction
     * @param auctionScope scope to send message or null to broadcast to all
     */
    public abstract void broadcastAuctionScopeMessage(List<String> messageKeys, AuctionScope auctionScope);
}
