package com.flobi.floAuction;

public class Participant {
	private String playerName = null;
	
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
