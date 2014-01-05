package com.flobi.floAuction;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class AuctionScope {
	private Auction activeAuction = null;
	private String name = "";
	private ArrayList<Auction> auctionQueue = new ArrayList<Auction>();
	private long lastAuctionDestroyTime = 0;
	private List<String> worlds = null;
	private ConfigurationSection config = null;

	public AuctionScope(String name, List<String> worlds, ConfigurationSection config) {
		this.name = name;
		this.worlds = worlds;
		this.config = config;
	}
	
	public Auction getActiveAuction() {
		return activeAuction;
	}
	
	public int getAuctionQueueLength() {
		return auctionQueue.size();
	}
	
	public void setActiveAuction(Auction auction) {
		if (activeAuction != null && auction == null) {
			lastAuctionDestroyTime = System.currentTimeMillis();
			checkAuctionQueue();
		}
		activeAuction = auction;
	}
	
	public void cancelAllAuctions() {
		auctionQueue.clear();
		if (activeAuction != null) {
			activeAuction.cancel();
		}
	}

    public void queueAuction(Auction auctionToQueue, Player player, Auction currentAuction) {
		String playerName = player.getName();

		if (currentAuction == null) {
			// Queuing because of interval not yet timed out.
			// Allow a queue of 1 to override if 0 for this condition.
	    	if (Math.max(floAuction.maxAuctionQueueLength, 1) <= auctionQueue.size()) {
	    		floAuction.sendMessage("auction-queue-fail-full", player, currentAuction, false);
				return;
			}
		} else {
	    	if (floAuction.maxAuctionQueueLength <= 0) {
	    		floAuction.sendMessage("auction-fail-auction-exists", player, currentAuction, false);
				return;
			}
			if (currentAuction.getOwner().equalsIgnoreCase(playerName)) {
				floAuction.sendMessage("auction-queue-fail-current-auction", player, currentAuction, false);
				return;
			}
			if (floAuction.maxAuctionQueueLength <= auctionQueue.size()) {
				floAuction.sendMessage("auction-queue-fail-full", player, currentAuction, false);
				return;
			}
		}
		for(int i = 0; i < auctionQueue.size(); i++) {
			if (auctionQueue.get(i) != null) {
				Auction queuedAuction = auctionQueue.get(i);
				if (queuedAuction.getOwner().equalsIgnoreCase(playerName)) {
					floAuction.sendMessage("auction-queue-fail-in-queue", player, currentAuction, false);
					return;
				}
			}
		}
		if ((auctionQueue.size() == 0 && System.currentTimeMillis() - lastAuctionDestroyTime >= floAuction.minAuctionIntervalSecs * 1000) || auctionToQueue.isValid()) {
			auctionQueue.add(auctionToQueue);
			Participant.addParticipant(playerName);
			checkAuctionQueue();
			if (auctionQueue.contains(auctionToQueue)) {
				floAuction.sendMessage("auction-queue-enter", player, currentAuction, false);
			}
		}
    }

	public void checkAuctionQueue() {
		if (activeAuction != null) {
			return;
		}
		if (System.currentTimeMillis() - lastAuctionDestroyTime < floAuction.minAuctionIntervalSecs * 1000) {
			return;
		}
		if (auctionQueue.size() == 0) {
			return;
		}
		Auction auction = auctionQueue.remove(0);
		if (auction == null) {
			return;
		}
		
		Player player = floAuction.server.getPlayer(auction.getOwner());
		if (player == null || !player.isOnline()) {
			return;
		}
		if (!floAuction.allowCreativeMode && player.getGameMode() == GameMode.CREATIVE) {
			floAuction.sendMessage("auction-fail-gamemode-creative", player, null, false);
			return;
		}
			
		if (!floAuction.perms.has(player, "auction.start")) {
			floAuction.sendMessage("no-permission", player, null, false);
			return;
		}
		if (!auction.isValid()) {
			return;
		}
		if (auction.start()) {
			activeAuction = auction;
		}
	}
	
	public boolean isPlayerInScope(Player player) {
		if (player == null) return false;
		World playerWorld = player.getWorld();
		if (playerWorld == null) return false;
		String playerWorldName = playerWorld.getName();
		for (int i = 0; i < worlds.size(); i++) {
			if (worlds.get(i).equalsIgnoreCase(playerWorldName) || worlds.get(i).equalsIgnoreCase("*")) return true;
			player.sendMessage(playerWorldName + " vs. " + worlds.get(i));
		}
		return false;
	}

	public ConfigurationSection getConfig() {
		return config;
	}

	public String getName() {
		return name;
	}
}

