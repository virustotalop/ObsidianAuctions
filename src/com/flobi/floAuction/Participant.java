package com.flobi.floAuction;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class Participant {
	private String playerName = null;
	private AuctionScope auctionScope = null;
	private Location lastKnownGoodLocation = null;
	private boolean sentEscapeWarning = false;
	private boolean sentArenaWarning = false;
	
	public static boolean checkLocation(String playerName) {
		Participant participant = Participant.getParticipant(playerName);
		if (participant == null) return true;
		return (participant.auctionScope.equals(AuctionScope.getPlayerScope(Bukkit.getPlayer(playerName))));
	}

	public static boolean checkLocation(String playerName, Location location) {
		Participant participant = Participant.getParticipant(playerName);
		if (participant == null) return true;
		return (participant.auctionScope.equals(AuctionScope.getLocationScope(location)));
	}

	public static void forceLocation(String playerName, Location locationForGaze) {
		Participant participant = Participant.getParticipant(playerName);
		if (participant == null) return;
		if (!participant.isParticipating()) return;
		
		Player player = Bukkit.getPlayer(playerName);
		Location location = player.getLocation();
		if (locationForGaze != null) {
			location.setDirection(new Vector(0, 0, 0));
			location.setPitch(locationForGaze.getPitch());
			location.setYaw(locationForGaze.getYaw());
		}
		
		if (!Participant.checkLocation(playerName)) {
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
	
	// Returns whether or not to cancel the teleport.
	public static boolean checkTeleportLocation(String playerName, Location location) {
		Participant participant = Participant.getParticipant(playerName);
		if (participant == null) return false;
		if (!participant.isParticipating()) return false;
		
		if (!Participant.checkLocation(playerName, location)) {
			participant.sendEscapeWarning();
			return true;
		}
		
		if (ArenaManager.isInArena(location)) {
			participant.sendArenaWarning();
			return true;
		}
		return false;
	}
	
	private void sendArenaWarning() {
		if (sentArenaWarning) return;
		floAuction.sendMessage("arena-warning", playerName, null);
		sentArenaWarning = true;
	}

	private void sendEscapeWarning() {
		if (sentEscapeWarning) return;
		floAuction.sendMessage("auctionscope-escape-warning", playerName, null);
		sentEscapeWarning = true;
	}

	public static boolean isParticipating(String playerName) {
		boolean participating = false;
		for (int i = 0; i < floAuction.auctionParticipants.size(); i++) {
			Participant participant = floAuction.auctionParticipants.get(i);
			if (participant.isParticipating() && playerName.equalsIgnoreCase(participant.getPlayerName())) {
				participating = true;
			}
		}
		return participating;
	}
	
	public static void addParticipant(String playerName, AuctionScope auctionScope) {
		Player player = Bukkit.getPlayer(playerName);
		if (Participant.getParticipant(playerName) == null) {
			Participant participant = new Participant(playerName, auctionScope);
			participant.lastKnownGoodLocation = player.getLocation();
			floAuction.auctionParticipants.add(participant);
			participant.isParticipating();
		}
	}
	
	public static Participant getParticipant(String playerName) {
		for (int i = 0; i < floAuction.auctionParticipants.size(); i++) {
			Participant participant = floAuction.auctionParticipants.get(i);
			if (playerName.equalsIgnoreCase(participant.getPlayerName())) {
				return participant;
			}
		}
		return null;
	}
	
	public Participant(String playerName, AuctionScope auctionScope) {
		this.playerName = playerName;
		this.auctionScope = auctionScope;
	}

	public String getPlayerName() {
		return playerName;
	}

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
		
		if (!participating) floAuction.auctionParticipants.remove(this);
		return participating;
	}

}
