package com.flobi.floAuction;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.flobi.utility.functions;

public class Auction {
	protected floAuction plugin;
	private String[] args;
	private Player owner;

	private int startingBid = 0;
	private int minBidIncrement = 0;
	private int quantity = 0;
	private int time = 0;
	private boolean active = false;
	
	private AuctionLot lot;
	private AuctionBid currentBid;
	
	// Scheduled timers:
	private int countdown = 0;
	private int countdownTimer = 0;
	
	
	
	public Auction(floAuction plugin, Player auctionOwner, String[] inputArgs) {
		owner = auctionOwner;
		args = inputArgs;
		this.plugin = plugin; 

		// Remove the optional "start" arg:
		if (args.length > 0) {
			if (args[0].equalsIgnoreCase("start")) {
				args = new String[0];
				System.arraycopy(inputArgs, 1, args, 0, inputArgs.length - 1);
			}
		}
		
	}
	public Boolean start() {
		if (!lot.AddItems(quantity, true)) {
			plugin.sendMessage("auction-fail-insufficient-supply", owner, this);
			return false;
		}
		active = true;
		plugin.sendMessage("auction-start", null, this);
		
		// Set timer:
		final Auction thisAuction = this;
		countdown = time;
		
		countdownTimer = plugin.getServer().getScheduler().scheduleAsyncRepeatingTask(plugin, new Runnable() {
		    public void run() {
		    	thisAuction.countdown--;
		    	if (thisAuction.countdown == 0) {
		    		thisAuction.end(null);
		    		return;
		    	}
		    	if (thisAuction.countdown < 4) {
			    	plugin.sendMessage("timer-countdown-notification", null, thisAuction);
			    	return;
		    	}
		    	if (thisAuction.time >= 20) {
		    		if (thisAuction.countdown == (int) (thisAuction.time / 2)) {
				    	plugin.sendMessage("timer-countdown-notification", null, thisAuction);
		    		}
		    	}
		    }
		}, 20L, 20L);

		info(null);
		return true;
	}
	public void info(CommandSender sender) {
		if (!active) {
			plugin.sendMessage("auction-info-no-auction", sender, this);
		} else if (currentBid == null) {
			plugin.sendMessage("auction-info-header-nobids", sender, this);
			plugin.sendMessage("auction-info-enchantment", sender, this);
			plugin.sendMessage("auction-info-footer-nobids", sender, this);
		} else {
			plugin.sendMessage("auction-info-header", sender, this);
			plugin.sendMessage("auction-info-enchantment", sender, this);
			plugin.sendMessage("auction-info-footer", sender, this);
		}
	}
	public void cancel(Player canceller) {
		plugin.sendMessage("auction-cancel", null, this);
		if (lot != null) lot.cancelLot();
		if (currentBid != null) currentBid.cancelBid();
		plugin.detachAuction(this);
	}
	public void end(Player ender) {

		plugin.getServer().getScheduler().cancelTask(countdownTimer);
		if (currentBid == null || lot == null) {
			plugin.sendMessage("auction-end-nobids", null, this);
			if (lot != null) lot.cancelLot();
			if (currentBid != null) currentBid.cancelBid();
		} else {
			plugin.sendMessage("auction-end", null, this);
			lot.winLot(currentBid.getBidder());
			currentBid.winBid();
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
		if (owner.equals(bidder) && !plugin.getConfig().getBoolean("allow-bid-on-own-auction")) {
			failBid(bid, "bid-fail-is-auction-owner");
			return;
		}
		if (currentBid == null) {
			setNewBid(bid, "bid-success-no-challenger");
			return;
		}
		Integer previousBidAmount = currentBid.getBidAmount();
		if (currentBid.getBidder().equals(bidder)) {
			if (bid.raiseOwnBid(currentBid)) {
				setNewBid(bid, "bid-success-update-own-bid");
			} else {
				if (bid.getMaxBidAmount() > currentBid.getMaxBidAmount()) {
					setNewBid(bid, "bid-success-update-own-maxbid");
				} else {
					failBid(bid, "bid-fail-already-current-bidder");
				}
			}
			return;
		}
		AuctionBid winner = null;
		AuctionBid looser = null;
		
		if (plugin.getConfig().getBoolean("use-old-bid-logic")) {
			if (bid.getMaxBidAmount() > currentBid.getMaxBidAmount()) {
				winner = bid;
				looser = currentBid;
			} else {
				winner = currentBid;
				looser = bid;
			}
			winner.raiseBid(Math.max(winner.getBidAmount(), Math.min(winner.getMaxBidAmount(), looser.getBidAmount() + minBidIncrement)));
		} else {
			// If you follow what this does, congratulations.  
			Integer baseBid = 0;
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
				looser = currentBid;
			} else {
				winner = currentBid;
				winner.raiseBid(baseBid + (Math.max(0, newSteps + 1) * minBidIncrement * 2) - minBidIncrement);
				looser = bid;
			}
			
		}

		if (previousBidAmount <= winner.getBidAmount()) {
			// Did the new bid win?
			if (winner.equals(bid)) {
				setNewBid(bid, "bid-success-outbid");
			} else {
				// Did the old bid have to raise the bid to stay winner?
				if (previousBidAmount < winner.getBidAmount()) {
					failBid(bid, "bid-fail-auto-outbid");
				} else {
					failBid(bid, null);
				}
			}
		} else {
			// Seriously don't know what could cause this, but might as well take care of it.
			plugin.sendMessage("bid-fail-too-low", bid.getBidder(), this);
		}
		
		
		
	}
	private void failBid(AuctionBid newBid, String reason) {
		newBid.cancelBid();
		plugin.sendMessage(reason, newBid.getBidder(), this);
	}
	private void setNewBid(AuctionBid newBid, String reason) {
		if (currentBid != null) {
			currentBid.cancelBid();
		}
		currentBid = newBid;
		plugin.sendMessage(reason, null, this);
	}
	private Boolean parseHeldItem() {
		ItemStack heldItem = owner.getItemInHand();
		if (heldItem == null || heldItem.getAmount() == 0) {
			plugin.sendMessage("auction-fail-hand-is-empty", owner, this);
			return false;
		}
		lot = new AuctionLot(plugin, heldItem, owner);
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
		if (owner == null) {
			plugin.sendMessage("auction-fail-invalid-owner", (Player) plugin.getServer().getConsoleSender(), this);
			return false;
		}
		return true;
	}
	private Boolean isValidAmount() {
		if (quantity <= 0) {
			plugin.sendMessage("auction-fail-quantity-too-low", owner, this);
			return false;
		}
		if (!functions.hasAmount(owner, quantity, lot.getTypeStack())) {
			plugin.sendMessage("auction-fail-insufficient-supply", owner, this);
			return false;
		}
		return true;
	}
	private Boolean isValidStartingBid() {
		if (startingBid < 0) {
			plugin.sendMessage("auction-fail-starting-bid-too-low", owner, this);
			return false;
		} else if (startingBid > plugin.maxStartingBid) {
			plugin.sendMessage("auction-fail-starting-bid-too-high", owner, this);
			return false;
		}
		return true;
	}
	private Boolean isValidIncrement() {
		if (getMinBidIncrement() < plugin.minIncrement) {
			plugin.sendMessage("auction-fail-increment-too-low", owner, this);
			return false;
		}
		if (getMinBidIncrement() > plugin.maxIncrement) {
			plugin.sendMessage("auction-fail-increment-too-high", owner, this);
			return false;
		}
		return true;
	}
	private Boolean isValidTime() {
		if (time < plugin.minTime) {
			plugin.sendMessage("auction-fail-time-too-low", owner, this);
			return false;
		}
		if (time > plugin.maxTime) {
			plugin.sendMessage("auction-fail-time-too-high", owner, this);
			return false;
		}
		return true;
	}
	private Boolean parseArgAmount() {
		ItemStack lotType = lot.getTypeStack();
		if (args.length > 0) {
			if (args[0].equalsIgnoreCase("this")) {
				quantity = lotType.getAmount();
			} else if (args[0].equalsIgnoreCase("all")) {
				quantity = functions.getAmount(owner, lotType);
			} else if (args[0].matches("[0-9]+")) {
				quantity = Integer.parseInt(args[0]);
			} else {
				plugin.getServer().broadcastMessage(args[0]);
				plugin.sendMessage("parse-error-invalid-quantity", owner, this);
				return false;
			}
		} else {
			quantity = lotType.getAmount();
		}
		return true;
	}
	private Boolean parseArgStartingBid() {
		if (args.length > 1) {
			if (args[1].matches("([0-9]*(\\.[0-9][0-9]?)?)")) {
				startingBid = functions.safeMoney(Double.parseDouble(args[1]));
			} else {
				plugin.sendMessage("parse-error-invalid-starting-bid", owner, this);
				return false;
			}
		} else {
			startingBid = plugin.defaultStartingBid;
		}
		return true;
	}
	private Boolean parseArgIncrement() {
		if (args.length > 2) {
			if (args[2].matches("([0-9]*(\\.[0-9][0-9]?)?)")) {
				minBidIncrement = functions.safeMoney(Double.parseDouble(args[2]));
			} else {
				plugin.sendMessage("parse-error-invalid-bid-increment", owner, this);
				return false;
			}
		} else {
			minBidIncrement = plugin.defaultBidIncrement;
		}
		return true;
	}
	private Boolean parseArgTime() {
		if (args.length > 3) {
			if (args[3].matches("[0-9]+")) {
				time = Integer.parseInt(args[3]);
			} else {
				plugin.sendMessage("parse-error-invalid-time", owner, this);
				return false;
			}
		} else {
			time = plugin.defaultAuctionTime;
		}
		return true;
	}
	public int getMinBidIncrement() {
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
	public int getStartingBid() {
		return startingBid;
	}
	public AuctionBid getCurrentBid() {
		return currentBid;
	}
	public Player getOwner() {
		return owner;
	}
	public int getRemainingTime() {
		return countdown;
	}
}
