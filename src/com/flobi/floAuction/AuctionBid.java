package com.flobi.floAuction;

import net.milkbowl.vault.economy.EconomyResponse;

import org.bukkit.entity.Player;

import com.flobi.utility.functions;

public class AuctionBid {
	private Auction auction;
	private Player bidder;
	private int bidAmount = 0;
	private int maxBidAmount = 0;
	private AuctionMessage error;
	private String[] args;
	private double reserve = 0;

	public AuctionBid(Auction theAuction, Player player, String[] inputArgs) {
		auction = theAuction;
		bidder = player;
		args = inputArgs;
		if (!validateBidder()) return;
		if (!parseArgs()) return;
		if (!reserveBidFunds()) return;
	}
	private boolean reserveBidFunds() {
		EconomyResponse receipt = floAuction.econ.withdrawPlayer(bidder.getName(), functions.unsafeMoney(maxBidAmount));
		if (receipt.transactionSuccess()) {
			reserve = receipt.amount;
			return true;
		} else {
			error = AuctionMessage.BID_FAIL_CANT_ALLOCATE_FUNDS;
			return false;
		}
	}
	public void cancelBid() {
		// Refund reserve.
		floAuction.econ.depositPlayer(bidder.getName(), reserve);
		reserve = 0;
	}
	public void winBid() {
		Double unsafeBidAmount = functions.unsafeMoney(bidAmount);
		
		// Apply winnings to auction owner.
		floAuction.econ.depositPlayer(auction.getOwner().getName(), unsafeBidAmount);

		// Refund remaining reserve.
		floAuction.econ.depositPlayer(bidder.getName(), reserve - unsafeBidAmount);
		
		reserve = 0;
	}
	
	private Boolean validateBidder() {
		if (bidder == null) {
			error = AuctionMessage.BID_FAIL_NO_BIDDER;
			return false;
		}
		
		// TODO:
		// Check if any orphan lots belong to this player, disallow bid while
		// orphan lots exist to lessen likeliness of using orphan lots as a
		// storage utility.
		
		
		return true;
	}
	private Boolean parseArgs() {
		if (!parseArgBid()) return false;
		if (!parseArgMaxBid()) return false;
		return false;
	}
	private Boolean parseArgBid() {
		if (args.length > 0) {
			if (args[0].matches("([0-9]*(\\.[0-9][0-9]?)?)")) {
				bidAmount = functions.safeMoney(Double.parseDouble(args[0]));
			} else {
				error = AuctionMessage.PARSE_ERROR_INVALID_BID;
				return false;
			}
		} else {
			// Leaving it up to automatic:
			bidAmount = 0;
		}
		// If the person bids 0, make it automatically the next increment (unless it's the current bidder).
		if (bidAmount == 0) {
			AuctionBid currentBid =  auction.getCurrentBid();
			if (currentBid == null) {
				// Use the starting bid if no one has bid yet.
				bidAmount = auction.getStartingBid();
				if (bidAmount == 0) {
					// Unless the starting bid is 0, then use the minimum bid increment.
					bidAmount = auction.getMinBidIncrement();
				}
			} else if (currentBid.getBidder().equals(bidder)) {
				// We are the current bidder, so use previous.  Don't auto-up our own bid.
				bidAmount = currentBid.bidAmount;
			} else {
				bidAmount = currentBid.getBidAmount() + auction.getMinBidIncrement();
			}
		}
		return true;
	}
	private Boolean parseArgMaxBid() {
		if (args.length > 1) {
			if (args[1].matches("([0-9]*(\\.[0-9][0-9]?)?)")) {
				maxBidAmount = functions.safeMoney(Double.parseDouble(args[1]));
			} else {
				error = AuctionMessage.PARSE_ERROR_INVALID_MAX_BID;
				return false;
			}
		} else {
			maxBidAmount = bidAmount;
		}
		return true;
	}

	public AuctionMessage getError() {
		return error;
	}
	public Player getBidder() {
		return bidder;
	}
	public int getBidAmount() {
		return bidAmount;
	}
	public Boolean outbid(AuctionBid challenger) {
		// TODO:
		return false;
	}
}
