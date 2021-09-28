package com.gmail.virustotalop.obsidianauctions.auction;

import com.gmail.virustotalop.obsidianauctions.ObsidianAuctions;
import com.gmail.virustotalop.obsidianauctions.area.AreaManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

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
     * Check to see if the participant is currently located within the AuctionScope in which he's participating. Nonparticipating players always return true.
     *
     * @param playerUUID player name to check
     * @return whether he is in the appropriate scope
     */
    public static boolean checkLocation(UUID playerUUID) {
        AuctionParticipant participant = AuctionParticipant.getParticipant(playerUUID);
        if(participant == null) return true;
        return (participant.auctionScope.equals(AuctionScope.getPlayerScope(Bukkit.getPlayer(playerUUID))));
    }

    /**
     * Check to see if the participant would be located within the AuctionScope in which he's participating if he were located elsewhere. Nonparticipating players always return true.
     *
     * @param playerUUID player name to check
     * @param location   location to check
     * @return whether he would be in the appropriate scope
     */
    public static boolean checkLocation(UUID playerUUID, Location location) {
        AuctionParticipant participant = AuctionParticipant.getParticipant(playerUUID);
        if(participant == null) {
            return true;
        }
        return (participant.auctionScope.equals(AuctionScope.getLocationScope(location)));
    }

    /**
     * Force a player back into the AuctionScope in which he's participating at the last known location he was spotted inside the scope.  Sends a one time message when moving the player. If a locationForGaze is included, it will make the player look the direction that location is looking.  Does nothing to nonparticipating players or players already in their scope.
     *
     * @param playerUUID      player to force
     * @param locationForGaze location for gaze
     */
    public static void forceLocation(UUID playerUUID, Location locationForGaze) {
        AuctionParticipant participant = AuctionParticipant.getParticipant(playerUUID);
        if(participant == null) {
            return;
        } else if(!participant.isParticipating()) {
            return;
        }

        Player player = Bukkit.getPlayer(playerUUID);
        Location location = player.getLocation();
        if(locationForGaze != null) {
            location.setDirection(new Vector(0, 0, 0));
            location.setPitch(locationForGaze.getPitch());
            location.setYaw(locationForGaze.getYaw());
        } else if(!AuctionParticipant.checkLocation(playerUUID)) {
            player.teleport(participant.lastKnownGoodLocation);
            participant.sendEscapeWarning();
            return;
        } else if(AreaManager.isInArena(player)) {
            player.teleport(participant.lastKnownGoodLocation);
            participant.sendArenaWarning();
            return;
        }

        participant.lastKnownGoodLocation = location;
    }

    /**
     * Checks whether to teleport a participant based on whether the destination would be outside the participants AuctionScope.  Sends a one time notification if it's not okay to teleport.
     *
     * @param playerUUID name of player to check
     * @param location   teleport destination to check
     * @return true if it IS okay to teleport this player
     */
    public static boolean checkTeleportLocation(UUID playerUUID, Location location) {
        AuctionParticipant participant = AuctionParticipant.getParticipant(playerUUID);
        if(participant == null) {
            return true;
        } else if(!participant.isParticipating()) {
            return true;
        } else if(!AuctionParticipant.checkLocation(playerUUID, location)) {
            participant.sendEscapeWarning();
            return false;
        } else if(AreaManager.isInArena(location)) {
            participant.sendArenaWarning();
            return false;
        }
        return true;
    }

    /**
     * Send a one time warning about attempting to enter an arena.
     */
    private void sendArenaWarning() {
        if(this.sentArenaWarning) {
            return;
        }
        ObsidianAuctions.get().getMessageManager().sendPlayerMessage("arena-warning", this.playerUUID, (AuctionScope) null);
        this.sentArenaWarning = true;
    }

    /**
     * Send a one time warning about attempting to escape the auction scope.
     */
    private void sendEscapeWarning() {
        if(this.sentEscapeWarning) {
            return;
        }
        ObsidianAuctions.get().getMessageManager().sendPlayerMessage("auctionscope-escape-warning", this.playerUUID, (AuctionScope) null);
        this.sentEscapeWarning = true;
    }

    /**
     * Checks to see if a player is participating in an auction in any AuctionScope.
     *
     * @param playerUUID player to check
     * @return whether the player is participating
     */
    public static boolean isParticipating(UUID playerUUID) {
        boolean participating = false;
        for(int i = 0; i < ObsidianAuctions.auctionParticipants.size(); i++) {
            AuctionParticipant participant = ObsidianAuctions.auctionParticipants.get(i);
            if(participant.isParticipating() && playerUUID.equals(participant.playerUUID)) {
                participating = true;
            }
        }
        return participating;
    }

    /**
     * Adds a player as a participant in an AuctionScope if they are participating.
     *
     * @param playerUUID   player to add
     * @param auctionScope scope for which to add player
     */
    public static void addParticipant(UUID playerUUID, AuctionScope auctionScope) {
        Player player = Bukkit.getServer().getPlayer(playerUUID);
        if(AuctionParticipant.getParticipant(playerUUID) == null) {
            AuctionParticipant participant = new AuctionParticipant(playerUUID, auctionScope);
            participant.lastKnownGoodLocation = player.getLocation();
            ObsidianAuctions.auctionParticipants.add(participant);
            participant.isParticipating();
        }
    }

    /**
     * Retrieve the participant instance for a given player name.
     *
     * @param playerUUID name of participant
     * @return participant instance
     */
    private static AuctionParticipant getParticipant(UUID playerUUID) {
        for(int i = 0; i < ObsidianAuctions.auctionParticipants.size(); i++) {
            AuctionParticipant participant = ObsidianAuctions.auctionParticipants.get(i);
            if(playerUUID.equals(participant.playerUUID)) {
                return participant;
            }
        }
        return null;
    }

    /**
     * Constructor to build instance by player name and scope.
     *
     * @param playerUUID   player name
     * @param auctionScope auction scope
     */
    private AuctionParticipant(UUID playerUUID, AuctionScope auctionScope) {
        this.playerUUID = playerUUID;
        this.auctionScope = auctionScope;
    }

    /**
     * Determines whether or not the player of participant instance is actually participating in an auction and will purge the participant instance if it is not participating.
     *
     * @return whether participant is actually participating
     */
    public boolean isParticipating() {
        boolean participating = false;
        Auction scopeAuction = this.auctionScope.getActiveAuction();

        if(scopeAuction != null) {
            if(scopeAuction.getOwnerName().equals(this.playerUUID)) {
                participating = true;
            } else if(scopeAuction.getCurrentBid() != null && scopeAuction.getCurrentBid().getBidderUUID().equals(this.playerUUID)) {
                participating = true;
            }
            for(int i = 0; i < scopeAuction.sealedBids.size(); i++) {
                if(scopeAuction.sealedBids.get(i).getBidderName().equals(playerUUID)) {
                    participating = true;
                }
            }
        }
        for(int i = 0; i < this.auctionScope.getAuctionQueueLength(); i++) {
            Auction queuedAuction = this.auctionScope.getAuctionQueue().get(i);
            if(queuedAuction != null) {
                if(queuedAuction.getOwnerName().equals(this.playerUUID)) {
                    participating = true;
                }
                if(queuedAuction.getCurrentBid() != null && queuedAuction.getCurrentBid().getBidderUUID().equals(this.playerUUID)) {
                    participating = true;
                }
            }
        }

        if(!participating) ObsidianAuctions.auctionParticipants.remove(this);
        {
            return participating;
        }
    }
}