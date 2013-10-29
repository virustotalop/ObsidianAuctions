package com.flobi.floAuction;

import java.util.Map;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.flobi.utility.functions;
import com.flobi.utility.items;

public class AuctionBid {
	private Auction auction;
	private String bidderName;
	private long bidAmount = 0;
	private long maxBidAmount = 0;
	private String error;
	private String[] args;
	private double reserve = 0;

	public AuctionBid(Auction theAuction, Player player, String[] inputArgs) {
		auction = theAuction;
		bidderName = player.getName();
		args = inputArgs;
		if (!validateBidder()) return;
		if (!parseArgs()) return;
		if (!reserveBidFunds()) return;
	}
	private boolean reserveBidFunds() {
		long amountToReserve = 0;
		long previousSealedReserve = 0;
		AuctionBid currentBid = auction.getCurrentBid();
		
		for(int i = 0; i < auction.sealedBids.size(); i++) {
			if (auction.sealedBids.get(i).getBidder().equalsIgnoreCase(this.getBidder())) {
				previousSealedReserve += auction.sealedBids.get(i).getBidAmount();
				auction.sealedBids.remove(i);
				i--;
			}
		}
		
		if (currentBid != null && currentBid.getBidder().equalsIgnoreCase(bidderName)) {
			// Same bidder: only reserve difference.
			if (maxBidAmount > currentBid.getMaxBidAmount() + previousSealedReserve) {
				amountToReserve = maxBidAmount - currentBid.getMaxBidAmount() - previousSealedReserve;
			} else {
				// Nothing needing reservation.
				return true;
			}
		} else {
			amountToReserve = maxBidAmount;
		}
		if (functions.withdrawPlayer(bidderName, amountToReserve)) {
			reserve = functions.getUnsafeMoney(amountToReserve);
			return true;
		} else {
			error = "bid-fail-cant-allocate-funds";
			return false;
		}
	}
	public void cancelBid() {
		if (auction.sealed) {
			// Queue reserve refund.
			auction.sealedBids.add(this);
			Participant.addParticipant(getBidder());
		} else {
			// Refund reserve.
			functions.depositPlayer(bidderName, reserve);
			reserve = 0;
		}
		
	}
	public void winBid() {
		Double unsafeBidAmount = functions.getUnsafeMoney(bidAmount);
		
		// Extract taxes:
		Double taxes = 0D;
		double taxPercent = floAuction.taxPercentage; 
		ItemStack typeStack = auction.getLotType();

		for (Map.Entry<String, String> entry : floAuction.taxedItems.entrySet()) {
			if (items.isSameItem(typeStack, entry.getKey())) {
				if (entry.getValue().endsWith("%")) {
					try {
						taxPercent = Double.valueOf(entry.getValue().substring(0, entry.getValue().length() - 1));
					} catch (Exception e) {
						// Clearly this isn't a valid number, just forget about it.
						taxPercent = floAuction.taxPercentage;
					}
				}
				break;
			}
		}
		
		
		if (taxPercent > 0D) {
			taxes = unsafeBidAmount * (taxPercent / 100D);
			
			auction.extractedPostTax = taxes;
			
			floAuction.sendMessage("auction-end-tax", auction.getOwner(), auction);
			unsafeBidAmount -= taxes;
			
			if (!floAuction.taxDestinationUser.isEmpty()) floAuction.econ.depositPlayer(floAuction.taxDestinationUser, taxes);
		}
		
		// Apply winnings to auction owner.
		floAuction.econ.depositPlayer(auction.getOwner(), unsafeBidAmount);

		// Refund remaining reserve.
		floAuction.econ.depositPlayer(bidderName, reserve - unsafeBidAmount - taxes);
		
		reserve = 0;
	}
	
	private Boolean validateBidder() {
		if (bidderName == null) {
			error = "bid-fail-no-bidder";
			return false;
		}

		if (!Participant.checkLocation(bidderName)) {
			error = "bid-fail-outside-auctionhouse";
			return false;
		}
		
		if (bidderName.equalsIgnoreCase(auction.getOwner()) && !floAuction.allowBidOnOwn) {
			error = "bid-fail-is-auction-owner";
			return false;
		}

		return true;
	}
	private Boolean parseArgs() {
		if (!parseArgBid()) return false;
		if (!parseArgMaxBid()) return false;
		return true;
	}
	public Boolean raiseOwnBid(AuctionBid otherBid) {
		if (bidderName.equalsIgnoreCase(otherBid.bidderName)) {
			// Move reserve money here.
			reserve = reserve + otherBid.reserve;
			otherBid.reserve = 0;

			// Maxbid only updates up.
			maxBidAmount = Math.max(maxBidAmount, otherBid.maxBidAmount);
			otherBid.maxBidAmount = maxBidAmount;

			if (bidAmount > otherBid.bidAmount) {
				// The bid has been raised.
				return true;
			} else {
				// Put the reserve on the other bid because we're cancelling this one.
				otherBid.reserve = reserve;
				reserve = 0;

				return false;
			}
		} else {
			// Don't take reserve unless it's the same person's money.
			return false;
		}
	}
	public Boolean raiseBid(Long newBidAmount) {
		if (newBidAmount <= maxBidAmount && newBidAmount >= bidAmount) {
			bidAmount = newBidAmount;
			return true;
		} else {
			return false;
		}
	}
	private Boolean parseArgBid() {
		if (args.length > 0) {
			if (!args[0].isEmpty() && args[0].matches(floAuction.decimalRegex)) {
				bidAmount = functions.getSafeMoney(Double.parseDouble(args[0]));
				if (bidAmount == 0) {
					error = "parse-error-invalid-bid";
					return false;
				}
			} else {
				error = "parse-error-invalid-bid";
				return false;
			}
		} else {
			if (auction.sealed || !floAuction.allowAutoBid) {
				error = "bid-fail-bid-required";
				return false;
			} else {
				// Leaving it up to automatic:
				bidAmount = 0;
			}
		}
		// If the person bids 0, make it automatically the next increment (unless it's the current bidder).
		if (bidAmount == 0) {
			AuctionBid currentBid = auction.getCurrentBid();
			if (currentBid == null) {
				// Use the starting bid if no one has bid yet.
				bidAmount = auction.getStartingBid();
				if (bidAmount == 0) {
					// Unless the starting bid is 0, then use the minimum bid increment.
					bidAmount = auction.getMinBidIncrement();
				}
			} else if (currentBid.getBidder().equalsIgnoreCase(bidderName)) {
				// We are the current bidder, so use previous.  Don't auto-up our own bid.
				bidAmount = currentBid.bidAmount;
			} else {
				bidAmount = currentBid.getBidAmount() + auction.getMinBidIncrement();
			}
		}
		if (bidAmount <= 0) {
			error = "parse-error-invalid-bid";
			return false;
		}
		return true;
	}
	private Boolean parseArgMaxBid() {
		if (!floAuction.allowMaxBids || auction.sealed) {
			// Just ignore it.
			maxBidAmount = bidAmount;
			return true;
		}
		if (args.length > 1) {
			if (!args[1].isEmpty() && args[1].matches(floAuction.decimalRegex)) {
				maxBidAmount = functions.getSafeMoney(Double.parseDouble(args[1]));
			} else {
				error = "parse-error-invalid-max-bid";
				return false;
			}
		}
		maxBidAmount = Math.max(bidAmount, maxBidAmount);
		if (maxBidAmount <= 0) {
			error = "parse-error-invalid-max-bid";
			return false;
		}
		return true;
	}

	public String getError() {
		return error;
	}
	public String getBidder() {
		return bidderName;
	}
	public long getBidAmount() {
		return bidAmount;
	}
	public long getMaxBidAmount() {
		return maxBidAmount;
	}
	public void setReserve(double newReserve) {
		reserve = newReserve;
	}
	public double getReserve() {
		return reserve;
	}
}
