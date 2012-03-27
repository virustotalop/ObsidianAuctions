package com.flobi.floAuction;

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
	
	private AuctionLot lot;
	private AuctionBid currentBid;
	
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
		AuctionMessage lotError = lot.AddItems(quantity, true); 
		if (lotError != null) {
			plugin.sendMessage(lotError, owner, this);
			return false;
		}
		plugin.sendMessage(AuctionMessage.AUCTION_START, owner, this);
		return true;
	}
	public void info(Player player) {
		plugin.sendMessage(AuctionMessage.AUCTION_INFO_HEADER, player, this);
		plugin.sendMessage(AuctionMessage.AUCTION_INFO_ENCHANTMENT, player, this);
		plugin.sendMessage(AuctionMessage.AUCTION_INFO_FOOTER, player, this);
	}
	public void cancel(Player canceller) {
		plugin.sendMessage(AuctionMessage.AUCTION_CANCEL, canceller, this);
		if (lot != null) lot.cancelLot();
		if (currentBid != null) currentBid.cancelBid();
	}
	public void end(Player ender) {
		// TODO: figure out how to clear auction object

		if (currentBid == null || lot == null) {
			plugin.sendMessage(AuctionMessage.AUCTION_END_NO_BIDS, ender, this);
			if (lot != null) lot.cancelLot();
			if (currentBid != null) currentBid.cancelBid();
			return;
		}
		plugin.sendMessage(AuctionMessage.AUCTION_END_WITH_BIDS, ender, this);
		lot.winLot(currentBid.getBidder());
		currentBid.winBid();
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
		if (owner.equals(bidder)) {
			failBid(bid, AuctionMessage.BID_FAIL_IS_AUCTION_OWNER);
			return;
		}
		if (currentBid == null) {
			setNewBid(bid, AuctionMessage.BID_SUCCESS_NO_CHALLENGER);
			return;
		}
		if (currentBid.getBidder().equals(bidder)) {
			if (bid.outbid(currentBid)) {
				// TODO: There is also the message, BID_SUCCESS_UPDATE_OWN_MAX_BID, for increasing that w/o increasing bid. 
				setNewBid(bid, AuctionMessage.BID_SUCCESS_UPDATE_OWN_BID);
			} else {
				failBid(bid, AuctionMessage.BID_FAIL_ALREADY_CURRENT_BIDDER);
			}
			return;
		}
		if (currentBid.outbid(bid)) {
			failBid(bid, AuctionMessage.BID_FAIL_AUTO_OUTBID);
			return;
		}
		if (bid.outbid(currentBid)) {
			setNewBid(bid, AuctionMessage.BID_SUCCESS_OUTBID);
			return;
		}
		failBid(bid, AuctionMessage.BID_FAIL_OUTBID_UNCERTAINTY);
	}
	private void failBid(AuctionBid newBid, AuctionMessage reason) {
		newBid.cancelBid();
		plugin.sendMessage(reason, newBid.getBidder(), this);
	}
	private void setNewBid(AuctionBid newBid, AuctionMessage reason) {
		currentBid.cancelBid();
		currentBid = newBid;
		plugin.sendMessage(reason, newBid.getBidder(), this);
	}
	private Boolean parseHeldItem() {
		ItemStack heldItem = owner.getItemInHand();
		if (heldItem == null) {
			plugin.sendMessage(AuctionMessage.AUCTION_FAIL_HAND_IS_EMPTY, owner, this);
			return false;
		}
		lot = new AuctionLot(heldItem, owner);
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
			plugin.sendMessage(AuctionMessage.AUCTION_FAIL_INVALID_OWNER, owner, this);
			return false;
		}
		return true;
	}
	private Boolean isValidAmount() {
		if (quantity <= 0) {
			plugin.sendMessage(AuctionMessage.AUCTION_FAIL_QUANTITY_TOO_LOW, owner, this);
			return false;
		}
		if (!functions.hasAmount(owner, quantity, lot.getTypeStack())) {
			plugin.sendMessage(AuctionMessage.AUCTION_FAIL_INSUFFICIENT_SUPPLY, owner, this);
			return false;
		}
		return true;
	}
	private Boolean isValidStartingBid() {
		if (startingBid > plugin.maxStartingBid) {
			plugin.sendMessage(AuctionMessage.AUCTION_FAIL_STARTING_BID_TOO_HIGH, owner, this);
			return false;
		}
		return true;
	}
	private Boolean isValidIncrement() {
		if (getMinBidIncrement() < plugin.minIncrement) {
			plugin.sendMessage(AuctionMessage.AUCTION_FAIL_INCREMENT_TOO_LOW, owner, this);
			return false;
		}
		if (getMinBidIncrement() > plugin.maxIncrement) {
			plugin.sendMessage(AuctionMessage.AUCTION_FAIL_INCREMENT_TOO_HIGH, owner, this);
			return false;
		}
		return true;
	}
	private Boolean isValidTime() {
		if (time < plugin.minTime) {
			plugin.sendMessage(AuctionMessage.AUCTION_FAIL_TIME_TOO_LOW, owner, this);
			return false;
		}
		if (time > plugin.maxTime) {
			plugin.sendMessage(AuctionMessage.AUCTION_FAIL_TIME_TOO_HIGH, owner, this);
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
				plugin.sendMessage(AuctionMessage.PARSE_ERROR_INVALID_QUANTITY, owner, this);
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
				plugin.sendMessage(AuctionMessage.PARSE_ERROR_INVALID_STARTING_BID, owner, this);
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
				plugin.sendMessage(AuctionMessage.PARSE_ERROR_INVALID_BID_INCREMENT, owner, this);
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
				plugin.sendMessage(AuctionMessage.PARSE_ERROR_INVALID_TIME, owner, this);
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
	public int getStartingBid() {
		return startingBid;
	}
	public AuctionBid getCurrentBid() {
		return currentBid;
	}
	public Player getOwner() {
		return owner;
	}
}
