package com.flobi.floauction;

import me.virustotal.floauction.utility.CArrayList;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * Class to track and manipulate the participants of auctions, which are auction starters (a.k.a. owners), the current highest bidder for unsealed auctions and all bidders on sealed auctions.
 * 
 * @author Joshua "flobi" Hatfield
 */
public class AuctionParticipant {
	private String playerName = null;
	private AuctionScope auctionScope = null;
	private Location lastKnownGoodLocation = null;
	private boolean sentEscapeWarning = false;
	private boolean sentArenaWarning = false;
	
	/**
	 * Check to see if the participant is currently located within the AuctionScope in which he's participating. Nonparticipating players always return true.
	 * 
	 * @param playerName player name to check
	 * @return whether he is in the appropriate scope
	 */
	public static boolean checkLocation(String playerName) {
		AuctionParticipant participant = AuctionParticipant.getParticipant(playerName);
		if (participant == null) return true;
		return (participant.auctionScope.equals(AuctionScope.getPlayerScope(Bukkit.getPlayer(playerName))));
	}

	/**
	 * Check to see if the participant would be located within the AuctionScope in which he's participating if he were located elsewhere. Nonparticipating players always return true.
	 * 
	 * @param playerName player name to check
	 * @param location location to check
	 * @return whether he would be in the appropriate scope
	 */
	public static boolean checkLocation(String playerName, Location location) {
		AuctionParticipant participant = AuctionParticipant.getParticipant(playerName);
		if (participant == null) return true;
		return (participant.auctionScope.equals(AuctionScope.getLocationScope(location)));
	}

	/**
	 * Force a player back into the AuctionScope in which he's participating at the last known location he was spotted inside the scope.  Sends a one time message when moving the player. If a locationForGaze is included, it will make the player look the direction that location is looking.  Does nothing to nonparticipating players or players already in their scope.  
	 * 
	 * @param playerName player to force
	 * @param locationForGaze location for gaze
	 */
	public static void forceLocation(String playerName, Location locationForGaze) {
		AuctionParticipant participant = AuctionParticipant.getParticipant(playerName);
		if (participant == null) return;
		if (!participant.isParticipating()) return;
		
		Player player = Bukkit.getPlayer(playerName);
		Location location = player.getLocation();
		if (locationForGaze != null) {
			location.setDirection(new Vector(0, 0, 0));
			location.setPitch(locationForGaze.getPitch());
			location.setYaw(locationForGaze.getYaw());
		}
		
		if (!AuctionParticipant.checkLocation(playerName)) {
			player.teleport(participant.lastKnownGoodLocation);
			participant.sendEscapeWarning();
			return;
		}
		
		if (ArenaManager.isInArena(player)) {
			player.teleport(participant.lastKnownGoodLocation);
			participant.sendArenaWarning();
			return;
		}
		
		participant.lastKnownGoodLocation = location;
	}
	
	/**
	 * Checks whether to teleport a participant based on whether the destination would be outside the participants AuctionScope.  Sends a one time notification if it's not okay to teleport.  
	 * 
	 * @param playerName name of player to check
	 * @param location teleport destination to check
	 * @return true if it IS okay to teleport this player
	 */
	public static boolean checkTeleportLocation(String playerName, Location location) {
		AuctionParticipant participant = AuctionParticipant.getParticipant(playerName);
		if (participant == null) return true;
		if (!participant.isParticipating()) return true;
		
		if (!AuctionParticipant.checkLocation(playerName, location)) {
			participant.sendEscapeWarning();
			return false;
		}
		
		if (ArenaManager.isInArena(location)) {
			participant.sendArenaWarning();
			return false;
		}
		return true;
	}
	
	/**
	 * Send a one time warning about attempting to enter an arena.
	 */
	private void sendArenaWarning() {
		if (sentArenaWarning) return;
		FloAuction.getMessageManager().sendPlayerMessage(new CArrayList<String>("arena-warning"), playerName, (AuctionScope) null);
		sentArenaWarning = true;
	}

	/**
	 * Send a one time warning about attempting to escape the auction scope.
	 */
	private void sendEscapeWarning() {
		if (sentEscapeWarning) return;
		FloAuction.getMessageManager().sendPlayerMessage(new CArrayList<String>("auctionscope-escape-warning"), playerName, (AuctionScope) null);
		sentEscapeWarning = true;
	}

	/**
	 * Checks to see if a player is participating in an auction in any AuctionScope.
	 * 
	 * @param playerName player to check
	 * @return whether the player is participating
	 */
	public static boolean isParticipating(String playerName) {
		boolean participating = false;
		for (int i = 0; i < FloAuction.auctionParticipants.size(); i++) {
			AuctionParticipant participant = FloAuction.auctionParticipants.get(i);
			if (participant.isParticipating() && playerName.equalsIgnoreCase(participant.playerName)) {
				participating = true;
			}
		}
		return participating;
	}
	
	/**
	 * Adds a player as a participant in an AuctionScope if they are participating.  
	 * 
	 * @param playerName player to add
	 * @param auctionScope scope for which to add player
	 */
	public static void addParticipant(String playerName, AuctionScope auctionScope) {
		Player player = Bukkit.getPlayer(playerName);
		if (AuctionParticipant.getParticipant(playerName) == null) {
			AuctionParticipant participant = new AuctionParticipant(playerName, auctionScope);
			participant.lastKnownGoodLocation = player.getLocation();
			FloAuction.auctionParticipants.add(participant);
			participant.isParticipating();
		}
	}
	
	/**
	 * Retrieve the participant instance for a given player name.
	 * 
	 * @param playerName name of participant
	 * @return participant instance
	 */
	private static AuctionParticipant getParticipant(String playerName) {
		for (int i = 0; i < FloAuction.auctionParticipants.size(); i++) {
			AuctionParticipant participant = FloAuction.auctionParticipants.get(i);
			if (playerName.equalsIgnoreCase(participant.playerName)) {
				return participant;
			}
		}
		return null;
	}
	
	/**
	 * Constructor to build instance by player name and scope.
	 * 
	 * @param playerName player name
	 * @param auctionScope auction scope
	 */
	private AuctionParticipant(String playerName, AuctionScope auctionScope) {
		this.playerName = playerName;
		this.auctionScope = auctionScope;
	}

	/**
	 * Determines whether or not the player of participant instance is actually participating in an auction and will purge the participant instance if it is not participating.
	 * 
	 * @return whether participant is actually participating
	 */
	public boolean isParticipating() {
		boolean participating = false;
		Auction scopeAuction = auctionScope.getActiveAuction();
        if (scopeAuction != null) {
            if (scopeAuction.getOwner().equalsIgnoreCase(playerName)) {
            	participating = true;
            }
            if (scopeAuction.getCurrentBid() != null && scopeAuction.getCurrentBid().getBidder().equalsIgnoreCase(playerName)) {
            	participating = true;
            }
            for (int i = 0; i < scopeAuction.sealedBids.size(); i++) {
            	if (scopeAuction.sealedBids.get(i).getBidder().equalsIgnoreCase(playerName)) {
                	participating = true;
            	}
            }
        }
		for (int i = 0; i < auctionScope.getAuctionQueueLength(); i++) {
			Auction queuedAuction = auctionScope.getAuctionQueue().get(i);
            if (queuedAuction != null) {
                if (queuedAuction.getOwner().equalsIgnoreCase(playerName)) {
                	participating = true;
                }
                if (queuedAuction.getCurrentBid() != null && queuedAuction.getCurrentBid().getBidder().equalsIgnoreCase(playerName)) {
                	participating = true;
                }
            }
		}
		
		if (!participating) FloAuction.auctionParticipants.remove(this);
		return participating;
	}

}
