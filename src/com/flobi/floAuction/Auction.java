package com.flobi.floAuction;

import java.util.ArrayList;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.flobi.utility.functions;
import com.flobi.utility.items;

public class Auction {
	protected floAuction plugin;
	private String[] args;
	private String ownerName;
	private String scope;

	private long startingBid = 0;
	private long minBidIncrement = 0;
	private int quantity = 0;
	private int time = 0;
	private boolean active = false;
	
	private AuctionLot lot;
	private AuctionBid currentBid;
	public ArrayList<AuctionBid> sealedBids = new ArrayList<AuctionBid>(); 
	
	public boolean sealed = false;
	
	public long prevTickTime = 0;
	
	// Scheduled timers:
	private int countdown = 0;
	private int countdownTimer = 0;
	
	public String getScope() {
		return scope;
	}
	
	public Auction(floAuction plugin, Player auctionOwner, String[] inputArgs, String scope, boolean sealed) {
		ownerName = auctionOwner.getName();
		args = inputArgs;
		this.plugin = plugin; 
		this.scope = scope;
		this.sealed = sealed;

		// Remove the optional "start" arg:
		if (args.length > 0) {
			if (args[0].equalsIgnoreCase("start") || args[0].equalsIgnoreCase("s")) {
				args = new String[inputArgs.length - 1];
				System.arraycopy(inputArgs, 1, args, 0, inputArgs.length - 1);
			}
		}
		
	}
	public Boolean start() {
		
		ItemStack typeStack = lot.getTypeStack();
		
		// Check banned items:
		for (int i = 0; i < floAuction.bannedItems.size(); i++) {
			if (items.isSameItem(typeStack, floAuction.bannedItems.get(i))) {
				floAuction.sendMessage("auction-fail-banned", ownerName, this);
				return false;
			}
		}
		
		if (floAuction.taxPerAuction > 0D) {
			if (!floAuction.econ.has(ownerName, floAuction.taxPerAuction)) {
				floAuction.sendMessage("auction-fail-start-tax", ownerName, this);
				return false;
			}
		}
		
		if (!lot.AddItems(quantity, true)) {
			floAuction.sendMessage("auction-fail-insufficient-supply", ownerName, this);
			return false;
		}

		if (floAuction.taxPerAuction > 0D) {
			if (floAuction.econ.has(ownerName, floAuction.taxPerAuction)) {
				floAuction.sendMessage("auction-start-tax", getOwner(), this);
				floAuction.econ.withdrawPlayer(ownerName, floAuction.taxPerAuction);
				if (!floAuction.taxDestinationUser.isEmpty()) floAuction.econ.depositPlayer(floAuction.taxDestinationUser, floAuction.taxPerAuction);
			}
		}

		active = true;
		floAuction.currentAuctionOwnerLocation = floAuction.server.getPlayer(ownerName).getLocation().clone();
		floAuction.currentAuctionOwnerGamemode = floAuction.server.getPlayer(ownerName).getGameMode();
		floAuction.sendMessage("auction-start", (CommandSender) null, this, true);
		
		// Set timer:
		final Auction thisAuction = this;
		countdown = time;
		
		countdownTimer = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
		    public void run() {
		    	if (thisAuction.prevTickTime + 1000 > System.currentTimeMillis()) return;
		    	thisAuction.prevTickTime = thisAuction.prevTickTime + 1000;
		    	
		    	thisAuction.countdown--;
		    	if (thisAuction.countdown == 0) {
		    		thisAuction.end(null);
		    		return;
		    	}
		    	if (!floAuction.suppressCountdown){
			    	if (thisAuction.countdown < 4) {
				    	floAuction.sendMessage("timer-countdown-notification", (CommandSender) null, thisAuction, true);
				    	return;
			    	}
			    	if (thisAuction.time >= 20) {
			    		if (thisAuction.countdown == (int) (thisAuction.time / 2)) {
					    	floAuction.sendMessage("timer-countdown-notification", (CommandSender) null, thisAuction, true);
			    		}
			    	}
		    	}
		    }
		}, 1L, 1L);
		prevTickTime = System.currentTimeMillis();

		info(null, true);
		return true;
	}
	public void info(CommandSender sender, boolean fullBroadcast) {
		ItemStack itemType = this.getLotType();
		short maxDurability = itemType.getType().getMaxDurability();
		short currentDurability = itemType.getDurability();
		if (!active) {
			floAuction.sendMessage("auction-info-no-auction", sender, this, fullBroadcast);
		} else if (sealed) {
			floAuction.sendMessage("auction-info-header-sealed", sender, this, fullBroadcast);
			if (!items.getDisplayName((CraftItemStack)itemType).isEmpty()) floAuction.sendMessage("auction-info-display-name", sender, this, fullBroadcast);
			if (!items.getBookTitle((CraftItemStack)itemType).isEmpty()) floAuction.sendMessage("auction-info-book-title", sender, this, fullBroadcast);
			if (!items.getBookAuthor((CraftItemStack)itemType).isEmpty()) floAuction.sendMessage("auction-info-book-author", sender, this, fullBroadcast);
			floAuction.sendMessage("auction-info-enchantment", sender, this, fullBroadcast);
			if (maxDurability > 0 && currentDurability > 0) floAuction.sendMessage("auction-info-damage", sender, this, fullBroadcast);
			floAuction.sendMessage("auction-info-footer-sealed", sender, this, fullBroadcast);
		} else if (currentBid == null) {
			floAuction.sendMessage("auction-info-header-nobids", sender, this, fullBroadcast);
			if (!items.getDisplayName((CraftItemStack)itemType).isEmpty()) floAuction.sendMessage("auction-info-display-name", sender, this, fullBroadcast);
			if (!items.getBookTitle((CraftItemStack)itemType).isEmpty()) floAuction.sendMessage("auction-info-book-title", sender, this, fullBroadcast);
			if (!items.getBookAuthor((CraftItemStack)itemType).isEmpty()) floAuction.sendMessage("auction-info-book-author", sender, this, fullBroadcast);
			floAuction.sendMessage("auction-info-enchantment", sender, this, fullBroadcast);
			if (maxDurability > 0 && currentDurability > 0) floAuction.sendMessage("auction-info-damage", sender, this, fullBroadcast);
			floAuction.sendMessage("auction-info-footer-nobids", sender, this, fullBroadcast);
		} else {
			floAuction.sendMessage("auction-info-header", sender, this, fullBroadcast);
			if (!items.getDisplayName((CraftItemStack)itemType).isEmpty()) floAuction.sendMessage("auction-info-display-name", sender, this, fullBroadcast);
			if (!items.getBookTitle((CraftItemStack)itemType).isEmpty()) floAuction.sendMessage("auction-info-book-title", sender, this, fullBroadcast);
			if (!items.getBookAuthor((CraftItemStack)itemType).isEmpty()) floAuction.sendMessage("auction-info-book-author", sender, this, fullBroadcast);
			floAuction.sendMessage("auction-info-enchantment", sender, this, fullBroadcast);
			if (maxDurability > 0 && currentDurability > 0) floAuction.sendMessage("auction-info-damage", sender, this, fullBroadcast);
			floAuction.sendMessage("auction-info-footer", sender, this, fullBroadcast);
		}
	}
	public void cancel() {
		floAuction.sendMessage("auction-cancel", (CommandSender) null, this, true);
		if (lot != null) lot.cancelLot();
		if (currentBid != null) currentBid.cancelBid();
		dispose();
	}
	public void end(Player ender) {
		if (currentBid == null || lot == null) {
			floAuction.sendMessage("auction-end-nobids", (CommandSender) null, this, true);
			if (lot != null) lot.cancelLot();
			if (currentBid != null) currentBid.cancelBid();
		} else {
			floAuction.sendMessage("auction-end", (CommandSender) null, this, true);
			lot.winLot(currentBid.getBidder());
			currentBid.winBid();
		}
		dispose();
	}
	private void dispose() {
		plugin.getServer().getScheduler().cancelTask(countdownTimer);

		sealed = false;
		for(int i = 0; i < sealedBids.size(); i++) {
			sealedBids.get(i).cancelBid();
		}
		
		plugin.detachAuction(this);
	}
	public Boolean isValid() {
		if (!parseHeldItem()) return false;
		if (!parseArgs()) return false;
		if (!isValidOwner()) return false;
		if (!isValidAmount()) return false;
		if (!isValidStartingBid()) return false;
		if (!isValidIncrement()) return false;
		if (!isValidTime()) return false;
		return true;
	}
	public void Bid(Player bidder, String[] inputArgs) {
		AuctionBid bid = new AuctionBid(this, bidder, inputArgs);
		if (bid.getError() != null) {
			failBid(bid, bid.getError());
			return;
		}
		if (currentBid == null) {
			if (bid.getBidAmount() < getStartingBid()) {
				failBid(bid, "bid-fail-under-starting-bid");
				return;
			}
			setNewBid(bid, "bid-success-no-challenger");
			return;
		}
		long previousBidAmount = currentBid.getBidAmount();
		long previousMaxBidAmount = currentBid.getMaxBidAmount();
		if (currentBid.getBidder().equals(bidder.getName())) {
			if (bid.raiseOwnBid(currentBid)) {
				setNewBid(bid, "bid-success-update-own-bid");
			} else {
				if (previousMaxBidAmount < currentBid.getMaxBidAmount()) {
					failBid(bid, "bid-success-update-own-maxbid");
				} else {
					failBid(bid, "bid-fail-already-current-bidder");
				}
			}
			return;
		}
		AuctionBid winner = null;
		AuctionBid loser = null;
		
		if (floAuction.useOldBidLogic) {
			if (bid.getMaxBidAmount() > currentBid.getMaxBidAmount()) {
				winner = bid;
				loser = currentBid;
			} else {
				winner = currentBid;
				loser = bid;
			}
			winner.raiseBid(Math.max(winner.getBidAmount(), Math.min(winner.getMaxBidAmount(), loser.getBidAmount() + minBidIncrement)));
		} else {
			// If you follow what this does, congratulations.  
			long baseBid = 0;
			if (bid.getBidAmount() >= currentBid.getBidAmount() + minBidIncrement) {
				baseBid = bid.getBidAmount();
			} else {
				baseBid = currentBid.getBidAmount() + minBidIncrement;
			}
			
			Integer prevSteps = (int) Math.floor((double)(currentBid.getMaxBidAmount() - baseBid + minBidIncrement) / minBidIncrement / 2);
			Integer newSteps = (int) Math.floor((double)(bid.getMaxBidAmount() - baseBid) / minBidIncrement / 2);

			if (newSteps >= prevSteps) {
				winner = bid;
				winner.raiseBid(baseBid + (Math.max(0, prevSteps) * minBidIncrement * 2));
				loser = currentBid;
			} else {
				winner = currentBid;
				winner.raiseBid(baseBid + (Math.max(0, newSteps + 1) * minBidIncrement * 2) - minBidIncrement);
				loser = bid;
			}
			
		}

		if (previousBidAmount <= winner.getBidAmount()) {
			// Did the new bid win?
			if (winner.equals(bid)) {
				setNewBid(bid, "bid-success-outbid");
			} else {
				// Did the old bid have to raise the bid to stay winner?
				if (previousBidAmount < winner.getBidAmount()) {
					if (!this.sealed && !floAuction.broadCastBidUpdates) floAuction.sendMessage("bid-auto-outbid", (CommandSender) null, this, true);
					failBid(bid, "bid-fail-auto-outbid");
				} else {
					if (!this.sealed) floAuction.sendMessage("bid-fail-too-low", bid.getBidder(), this);
					failBid(bid, null);
				}
			}
		} else {
			// Seriously don't know what could cause this, but might as well take care of it.
			floAuction.sendMessage("bid-fail-too-low", bid.getBidder(), this);
		}
		
		
		
	}
	private void failBid(AuctionBid newBid, String reason) {
		newBid.cancelBid();
		if (this.sealed && (newBid.getError() == null || newBid.getError().isEmpty())) {
			floAuction.sendMessage("bid-success-sealed", newBid.getBidder(), this);
		} else {
			floAuction.sendMessage(reason, newBid.getBidder(), this);
		}
	}
	private void setNewBid(AuctionBid newBid, String reason) {
		AuctionBid prevBid = currentBid;
		
		if (currentBid != null) {
			currentBid.cancelBid();
		}
		currentBid = newBid;
		floAuction.currentBidPlayerLocation = floAuction.server.getPlayer(newBid.getBidder()).getLocation().clone();
		floAuction.currentBidPlayerGamemode = floAuction.server.getPlayer(newBid.getBidder()).getGameMode();
		if (this.sealed) {
			floAuction.sendMessage("bid-success-sealed", newBid.getBidder(), this);
		} else if (floAuction.broadCastBidUpdates) {
			floAuction.sendMessage(reason, (CommandSender) null, this, true);
		} else {
			floAuction.sendMessage(reason, newBid.getBidder(), this);
			if (prevBid != null && newBid.getBidder().equalsIgnoreCase(prevBid.getBidder())) {
				floAuction.sendMessage(reason, prevBid.getBidder(), this);
			}
		}
	}
	private Boolean parseHeldItem() {
		Player owner = floAuction.server.getPlayer(ownerName);
		if (lot != null) {
			return true;
		}
		ItemStack heldItem = owner.getItemInHand();
		if (heldItem == null || heldItem.getAmount() == 0) {
			floAuction.sendMessage("auction-fail-hand-is-empty", owner, this, false);
			return false;
		}
		lot = new AuctionLot(heldItem, ownerName);
		
		ItemStack itemType = lot.getTypeStack();
		
		if (
				!floAuction.allowDamagedItems &&
				itemType.getType().getMaxDurability() > 0 &&
				itemType.getDurability() > 0
		) {
			floAuction.sendMessage("auction-fail-damaged-item", owner, this, false);
			lot = null;
			return false;
		}
		
		return true;
	}
	private Boolean parseArgs() {
		// (amount) (starting price) (increment) (time)
		if (!parseArgAmount()) return false;
		if (!parseArgStartingBid()) return false;
		if (!parseArgIncrement()) return false;
		if (!parseArgTime()) return false;
		return true;
	}
	private Boolean isValidOwner() {
		if (ownerName == null) {
			floAuction.sendMessage("auction-fail-invalid-owner", (Player) plugin.getServer().getConsoleSender(), this, false);
			return false;
		}
		return true;
	}
	private Boolean isValidAmount() {
		if (quantity <= 0) {
			floAuction.sendMessage("auction-fail-quantity-too-low", ownerName, this);
			return false;
		}
		if (!items.hasAmount(ownerName, quantity, lot.getTypeStack())) {
			floAuction.sendMessage("auction-fail-insufficient-supply", ownerName, this);
			return false;
		}
		return true;
	}
	private Boolean isValidStartingBid() {
		if (startingBid < 0) {
			floAuction.sendMessage("auction-fail-starting-bid-too-low", ownerName, this);
			return false;
		} else if (startingBid > floAuction.maxStartingBid) {
			floAuction.sendMessage("auction-fail-starting-bid-too-high", ownerName, this);
			return false;
		}
		return true;
	}
	private Boolean isValidIncrement() {
		if (getMinBidIncrement() < floAuction.minIncrement) {
			floAuction.sendMessage("auction-fail-increment-too-low", ownerName, this);
			return false;
		}
		if (getMinBidIncrement() > floAuction.maxIncrement) {
			floAuction.sendMessage("auction-fail-increment-too-high", ownerName, this);
			return false;
		}
		return true;
	}
	private Boolean isValidTime() {
		if (time < floAuction.minTime) {
			floAuction.sendMessage("auction-fail-time-too-low", ownerName, this);
			return false;
		}
		if (time > floAuction.maxTime) {
			floAuction.sendMessage("auction-fail-time-too-high", ownerName, this);
			return false;
		}
		return true;
	}
	private Boolean parseArgAmount() {
		if (quantity > 0) return true;

		ItemStack lotType = lot.getTypeStack();
		if (args.length > 0) {
			if (args[0].equalsIgnoreCase("this")) {
				quantity = lotType.getAmount();
			} else if (args[0].equalsIgnoreCase("all")) {
				quantity = items.getAmount(ownerName, lotType);
			} else if (args[0].matches("[0-9]{1,7}")) {
				quantity = Integer.parseInt(args[0]);
			} else {
				floAuction.sendMessage("parse-error-invalid-quantity", ownerName, this);
				return false;
			}
		} else {
			quantity = lotType.getAmount();
		}
		if (quantity < 0) {
			floAuction.sendMessage("parse-error-invalid-quantity", ownerName, this);
			return false;
		}
		return true;
	}
	private Boolean parseArgStartingBid() {
		if (startingBid > 0) return true;
		
		if (args.length > 1) {
			if (args[1].matches(floAuction.decimalRegex)) {
				startingBid = functions.getSafeMoney(Double.parseDouble(args[1]));
			} else {
				floAuction.sendMessage("parse-error-invalid-starting-bid", ownerName, this);
				return false;
			}
		} else {
			startingBid = floAuction.defaultStartingBid;
		}
		if (startingBid < 0) {
			floAuction.sendMessage("parse-error-invalid-starting-bid", ownerName, this);
			return false;
		}
		return true;
	}
	private Boolean parseArgIncrement() {
		if (minBidIncrement > 0) return true;

		if (args.length > 2) {
			if (args[2].matches(floAuction.decimalRegex)) {
				minBidIncrement = functions.getSafeMoney(Double.parseDouble(args[2]));
			} else {
				floAuction.sendMessage("parse-error-invalid-bid-increment", ownerName, this);
				return false;
			}
		} else {
			minBidIncrement = floAuction.defaultBidIncrement;
		}
		if (minBidIncrement < 0) {
			floAuction.sendMessage("parse-error-invalid-bid-increment", ownerName, this);
			return false;
		}
		return true;
	}
	private Boolean parseArgTime() {
		if (time > 0) return true;

		if (args.length > 3) {
			if (args[3].matches("[0-9]{1,7}")) {
				time = Integer.parseInt(args[3]);
			} else {
				floAuction.sendMessage("parse-error-invalid-time", ownerName, this);
				return false;
			}
		} else {
			time = floAuction.defaultAuctionTime;
		}
		if (time < 0) {
			floAuction.sendMessage("parse-error-invalid-time", ownerName, this);
			return false;
		}
		return true;
	}
	public long getMinBidIncrement() {
		return minBidIncrement;
	}
	
	public ItemStack getLotType() {
		if (lot == null) {
			return null;
		}
		return lot.getTypeStack();
	}
	
	public int getLotQuantity() {
		if (lot == null) {
			return 0;
		}
		return lot.getQuantity();
	}
	public long getStartingBid() {
		long effectiveStartingBid = startingBid;
		if (effectiveStartingBid == 0) {
			effectiveStartingBid = minBidIncrement; 
		}
		return effectiveStartingBid;
	}
	public AuctionBid getCurrentBid() {
		return currentBid;
	}
	public String getOwner() {
		return ownerName;
	}
	public int getRemainingTime() {
		return countdown;
	}
	public int getTotalTime() {
		return time;
	}

    public int addToRemainingTime(int i) {
            countdown += i;
            return countdown;
    }
}
