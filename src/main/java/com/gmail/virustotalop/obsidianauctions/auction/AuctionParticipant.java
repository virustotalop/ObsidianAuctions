package com.gmail.virustotalop.obsidianauctions.auction;

import com.gmail.virustotalop.obsidianauctions.ObsidianAuctions;
import org.bukkit.Location;

import java.util.UUID;

/**
 * Class to track and manipulate the participants of auctions, which are auction starters (a.k.a. owners), the current highest bidder for unsealed auctions and all bidders on sealed auctions.
 *
 * @author Joshua "flobi" Hatfield
 */
public class AuctionParticipant {

    private final UUID playerUUID;
    private final AuctionScope auctionScope;
    private Location lastKnownGoodLocation = null;
    private boolean sentEscapeWarning = false;
    private boolean sentArenaWarning = false;

    /**
     * Constructor to build instance by player name and scope.
     *
     * @param playerUUID   player name
     * @param auctionScope auction scope
     */
    AuctionParticipant(UUID playerUUID, AuctionScope auctionScope) {
        this.playerUUID = playerUUID;
        this.auctionScope = auctionScope;
    }

    public UUID getPlayerUUID() {
        return this.playerUUID;
    }

    public AuctionScope getAuctionScope() {
        return this.auctionScope;
    }

    /**
     * Determines whether or not the player of participant instance is actually participating in an auction and will purge the participant instance if it is not participating.
     *
     * @return whether participant is actually participating
     */
    public boolean isParticipating() {
        boolean participating = false;
        Auction scopeAuction = this.auctionScope.getActiveAuction();
        if (scopeAuction != null) {
            if (scopeAuction.getOwnerUUID().equals(this.playerUUID)) {
                participating = true;
            } else if (scopeAuction.getCurrentBid() != null && scopeAuction.getCurrentBid().getBidderUUID().equals(this.playerUUID)) {
                participating = true;
            }
            for (int i = 0; i < scopeAuction.getSealedBids().size(); i++) {
                if (scopeAuction.getSealedBids().get(i).getBidderName().equals(this.playerUUID)) {
                    participating = true;
                }
            }
        }
        for (int i = 0; i < this.auctionScope.getAuctionQueueLength(); i++) {
            Auction queuedAuction = this.auctionScope.getAuctionQueue().get(i);
            if (queuedAuction != null) {
                if (queuedAuction.getOwnerUUID().equals(this.playerUUID)) {
                    participating = true;
                }
                if (queuedAuction.getCurrentBid() != null && queuedAuction.getCurrentBid().getBidderUUID().equals(this.playerUUID)) {
                    participating = true;
                }
            }
        }
        if (!participating) {
            ObsidianAuctions.get().getAuctionManager().removeParticipant(this);
        }
        return participating;
    }

    /**
     * Send a one time warning about attempting to enter an arena.
     */
    void sendArenaWarning() {
        if (this.sentArenaWarning) {
            return;
        }
        ObsidianAuctions.get().getMessageManager().sendPlayerMessage("arena-warning", this.playerUUID, (AuctionScope) null);
        this.sentArenaWarning = true;
    }

    /**
     * Send a one time warning about attempting to escape the auction scope.
     */
    void sendEscapeWarning() {
        if (this.sentEscapeWarning) {
            return;
        }
        ObsidianAuctions.get().getMessageManager().sendPlayerMessage("auctionscope-escape-warning", this.playerUUID, (AuctionScope) null);
        this.sentEscapeWarning = true;
    }

    Location getLastKnownGoodLocation() {
        return this.lastKnownGoodLocation;
    }

    void setLastKnownGoodLocation(Location location) {
        this.lastKnownGoodLocation = location;
    }
}