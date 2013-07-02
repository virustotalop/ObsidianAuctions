package com.flobi.floAuction;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class Participant {
	private String playerName = null;
	private static Location minLocation = null;
	private static Location maxLocation = null;
	private Location lastKnownGoodLocation = null;
	private boolean sentEscapeWarning = false;
	
	public static void setAuctionHouseBox(String world, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
		if (world.isEmpty()) {
			minLocation = null;
			maxLocation = null;
		} else {
			minLocation = new Location(Bukkit.getWorld(world), minX, minY, minZ);
			maxLocation = new Location(Bukkit.getWorld(world), maxX, maxY, maxZ);
		}
	}
	
	public static boolean checkLocation(String playerName) {
		if (minLocation == null) return true;
		Player player = floAuction.server.getPlayer(playerName);
		Location currentLocation = player.getLocation();
		if (!currentLocation.getWorld().equals(minLocation.getWorld())) return false;
		if (currentLocation.getX() > Math.max(minLocation.getX(), maxLocation.getX()) || currentLocation.getX() < Math.min(minLocation.getX(), maxLocation.getX())) return false;
		if (currentLocation.getZ() > Math.max(minLocation.getZ(), maxLocation.getZ()) || currentLocation.getZ() < Math.min(minLocation.getZ(), maxLocation.getZ())) return false;
		if (currentLocation.getY() > Math.max(minLocation.getY(), maxLocation.getY()) || currentLocation.getY() < Math.min(minLocation.getY(), maxLocation.getY())) return false;
		return true;
	}
	
	public static void forceLocation(String playerName) {
		if (minLocation == null) return;

		Participant participant = Participant.getParticipant(playerName);
		if (participant == null) return;
		if (!participant.isParticipating()) return;
		
		Player player = floAuction.server.getPlayer(playerName);
		Location location = player.getLocation();
		
		
		if (participant.lastKnownGoodLocation != null) {
			if (!Participant.checkLocation(playerName)) {
				player.teleport(participant.lastKnownGoodLocation);
				participant.sendEscapeWarning();
				return;
			}
		} else {
			boolean doMove = false;
			double x = location.getX();
			double y = location.getY();
			double z = location.getZ();
			
			if (!location.getWorld().equals(minLocation.getWorld())) {doMove = true; location.setWorld(minLocation.getWorld());}
			if (x > Math.max(minLocation.getX(), maxLocation.getX())) {doMove = true; location.setX(Math.max(minLocation.getX(), maxLocation.getX()));} 
			if (x < Math.min(minLocation.getX(), maxLocation.getX())) {doMove = true; location.setX(Math.min(minLocation.getX(), maxLocation.getX()));}
			if (y > Math.max(minLocation.getY(), maxLocation.getY())) {doMove = true; location.setY(Math.max(minLocation.getY(), maxLocation.getY()));}
			if (y < Math.min(minLocation.getY(), maxLocation.getY())) {doMove = true; location.setY(Math.min(minLocation.getY(), maxLocation.getY()));}
			if (z > Math.max(minLocation.getZ(), maxLocation.getZ())) {doMove = true; location.setZ(Math.max(minLocation.getZ(), maxLocation.getZ()));}
			if (z < Math.min(minLocation.getZ(), maxLocation.getZ())) {doMove = true; location.setZ(Math.min(minLocation.getZ(), maxLocation.getZ()));}
			
			if (doMove) {
				player.teleport(location);
				participant.sendEscapeWarning();
			}
		}
		participant.lastKnownGoodLocation = location;
	}
	
	private void sendEscapeWarning() {
		if (sentEscapeWarning) return;
		floAuction.sendMessage("auctionhouse-escape-warning", playerName, null);
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
	
	public static void addParticipant(String playerName) {
		if (Participant.getParticipant(playerName) == null) {
			Participant participant = new Participant(playerName);
			floAuction.auctionParticipants.add(participant);
			participant.isParticipating();
		}
	}
	
	public static Participant getParticipant(String playerName) {
		for (int i = 0; i < floAuction.auctionParticipants.size(); i++) {
			Participant participant = floAuction.auctionParticipants.get(i);
			if (participant.isParticipating() && playerName.equalsIgnoreCase(participant.getPlayerName())) {
				return participant;
			}
		}
		return null;
	}
	
	public Participant(String playerName) {
		this.playerName = playerName;
	}

	public String getPlayerName() {
		return playerName;
	}

	public boolean isParticipating() {
		boolean participating = false;
        if (floAuction.publicAuction != null) {
            if (floAuction.publicAuction.getOwner().equalsIgnoreCase(playerName)) {
            	participating = true;
            }
            if (floAuction.publicAuction.getCurrentBid() != null && floAuction.publicAuction.getCurrentBid().getBidder().equalsIgnoreCase(playerName)) {
            	participating = true;
            }
            for (int i = 0; i < floAuction.publicAuction.sealedBids.size(); i++) {
            	if (floAuction.publicAuction.sealedBids.get(i).getBidder().equalsIgnoreCase(playerName)) {
                	participating = true;
            	}
            }
        }
		for (int i = 0; i < floAuction.auctionQueue.size(); i++) {
			Auction queuedAuction = floAuction.auctionQueue.get(i);
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
