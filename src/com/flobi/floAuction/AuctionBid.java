package com.flobi.floAuction;

import net.milkbowl.vault.economy.EconomyResponse;

import org.bukkit.entity.Player;

import com.flobi.utility.functions;

public class AuctionBid {
	private Auction auction;
	private Player bidder;
	private int bidAmount = 0;
	private int maxBidAmount = 0;
	private String error;
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
		int amountToReserve = 0;
		AuctionBid currentBid = auction.getCurrentBid(); 
		if (currentBid.getBidder().equals(bidder)) {
			// Same bidder: only reserve difference.
			if (maxBidAmount > currentBid.getMaxBidAmount()) {
				amountToReserve = currentBid.getMaxBidAmount() - maxBidAmount;
			} else {
				// Nothing needing reservation.
				return true;
			}
		} else {
			amountToReserve = maxBidAmount;
		}
		EconomyResponse receipt = floAuction.econ.withdrawPlayer(bidder.getName(), functions.unsafeMoney(amountToReserve));
		if (receipt.transactionSuccess()) {
			reserve = receipt.amount;
			return true;
		} else {
			error = "bid-fail-cant-allocate-funds";
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
			error = "bid-fail-no-bidder";
			return false;
		}
		return true;
	}
	private Boolean parseArgs() {
		if (!parseArgBid()) return false;
		if (!parseArgMaxBid()) return false;
		return false;
	}
	public Boolean raiseOwnBid(AuctionBid otherBid) {
		if (bidder.equals(otherBid.bidder)) {
			// Move reserve money here.
			reserve = reserve + otherBid.reserve;
			otherBid.reserve = 0;

			// Maxbid only updates up.
			maxBidAmount = Math.max(maxBidAmount, otherBid.maxBidAmount);

			if (bidAmount > otherBid.bidAmount) {
				// The bid has been raised.
				return true;
			} else {
				// The bid has not been raised (don't forget, this bid still needs to replace the old one).
				bidAmount = otherBid.bidAmount;
				return false;
			}
		} else {
			// Don't take reserve unless it's the same person's money.
			return false;
		}
	}
	public Boolean raiseBid(Integer newBidAmount) {
		if (newBidAmount <= maxBidAmount && newBidAmount >= bidAmount) {
			bidAmount = newBidAmount;
			return true;
		} else {
			return false;
		}
	}
	private Boolean parseArgBid() {
		if (args.length > 0) {
			if (args[0].matches("([0-9]*(\\.[0-9][0-9]?)?)")) {
				bidAmount = functions.safeMoney(Double.parseDouble(args[0]));
			} else {
				error = "parse-error-invalid-bid";
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
				error = "parse-error-invalid-max-bid";
				return false;
			}
		}
		maxBidAmount = Math.max(bidAmount, maxBidAmount);
		return true;
	}

	public String getError() {
		return error;
	}
	public Player getBidder() {
		return bidder;
	}
	public int getBidAmount() {
		return bidAmount;
	}
	public int getMaxBidAmount() {
		return maxBidAmount;
	}
	public void setReserve(double newReserve) {
		reserve = newReserve;
	}
	public double getReserve() {
		return reserve;
	}
}
