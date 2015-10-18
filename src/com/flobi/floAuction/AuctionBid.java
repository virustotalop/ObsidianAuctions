package com.flobi.floAuction;

import java.util.Map;

import me.virustotal.utility.CArrayList;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.flobi.floAuction.utilities.Functions;
import com.flobi.floAuction.utilities.Items;

/**
 * Structure to handle auction bids.
 * 
 * @author Joshua "flobi" Hatfield
 */
public class AuctionBid {
	private Auction auction;
	private String bidderName;
	private long bidAmount = 0;
	private long maxBidAmount = 0;
	private String error;
	private String[] args;
	private double reserve = 0;

	/**
	 * Constructor that validates bidder, parses arguments and reserves maximum bid funds.
	 * 
	 * @param auction the auction being bid upon
	 * @param player the player doing the bidding
	 * @param inputArgs the parameters entered in chat
	 */
	public AuctionBid(Auction auction, Player player, String[] inputArgs) {
		this.auction = auction;
		bidderName = player.getName();
		args = inputArgs;
		if (!validateBidder()) return;
		if (!parseArgs()) return;
		if (!reserveBidFunds()) return;
	}
	
	/**
	 * Removes funds from bidder's account and stores them in the AuctionBid instance.
	 * 
	 * @return true if funds are reserved, false if an issue was encountered
	 */
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
		if (Functions.withdrawPlayer(bidderName, amountToReserve)) {
			reserve = Functions.getUnsafeMoney(amountToReserve);
			return true;
		} else {
			error = "bid-fail-cant-allocate-funds";
			return false;
		}
	}
	
	/**
	 * Refunds reserve for unsealed auctions or queues reserve refund for sealed auctions. 
	 */
	public void cancelBid() {
		if (auction.sealed) {
			// Queue reserve refund.
			auction.sealedBids.add(this);
			AuctionParticipant.addParticipant(getBidder(), auction.getScope());
		} else {
			// Refund reserve.
			Functions.depositPlayer(bidderName, reserve);
			reserve = 0;
		}
		
	}
	
	/**
	 * Process winning bid, gives winnings to auction owner, returns the remainder of the reserve and appropriates end of auction taxes.
	 */
	public void winBid() {
		Double unsafeBidAmount = Functions.getUnsafeMoney(bidAmount);
		
		// Extract taxes:
		Double taxes = 0D;
		double taxPercent = AuctionConfig.getDouble("auction-end-tax-percent", auction.getScope()); 
		ItemStack typeStack = auction.getLotType();

		// TODO: Check this line for possible NULL
		for (Map.Entry<String, String> entry : AuctionConfig.getStringStringMap("taxed-items", auction.getScope()).entrySet()) {
			if (Items.isSameItem(typeStack, entry.getKey())) {
				if (entry.getValue().endsWith("%")) {
					try {
						taxPercent = Double.valueOf(entry.getValue().substring(0, entry.getValue().length() - 1));
					} catch (Exception e) {
						// Clearly this isn't a valid number, just forget about it.
						// taxPercent = AuctionConfig.getDouble("auction-end-tax-percent", auction.getScope());
						// On second thought, this is already the value, so just keep it.
					}
				}
				break;
			}
		}
		
		
		if (taxPercent > 0D) {
			taxes = unsafeBidAmount * (taxPercent / 100D);
			
			auction.extractedPostTax = taxes;
			auction.messageManager.sendPlayerMessage(new CArrayList<String>("auction-end-tax"), auction.getOwner(), auction);
			unsafeBidAmount -= taxes;
			String taxDestinationUser = AuctionConfig.getString("deposit-tax-to-user", auction.getScope());
			if (!taxDestinationUser.isEmpty()) floAuction.econ.depositPlayer(taxDestinationUser, taxes);
		}
		
		// Apply winnings to auction owner.
		floAuction.econ.depositPlayer(auction.getOwner(), unsafeBidAmount);

		// Refund remaining reserve.
		floAuction.econ.depositPlayer(bidderName, reserve - unsafeBidAmount - taxes);
		
		reserve = 0;
	}
	
	/**
	 * Checks existence, permission, scope and prohibitions on player trying to bid.
	 * 
	 * @return whether player can bid
	 */
	private Boolean validateBidder() {
		if (bidderName == null) {
			error = "bid-fail-no-bidder";
			return false;
		}

		if (AuctionProhibition.isOnProhibition(bidderName, false)) {
			error = "remote-plugin-prohibition-reminder";
			return false;
		}

		if (!AuctionParticipant.checkLocation(bidderName)) {
			error = "bid-fail-outside-auctionhouse";
			return false;
		}
		
		if (bidderName.equalsIgnoreCase(auction.getOwner()) && !AuctionConfig.getBoolean("allow-bid-on-own-auction", auction.getScope())) {
			error = "bid-fail-is-auction-owner";
			return false;
		}

		return true;
	}
	
	/**
	 * Parses bid arguments.
	 * 
	 * @return whether args are acceptable
	 */
	private Boolean parseArgs() {
		if (!parseArgBid()) return false;
		if (!parseArgMaxBid()) return false;
		return true;
	}
	
	/**
	 * Prepares two bids from the same player to compete against each other.
	 * 
	 * @param otherBid the previous bid
	 * @return whether it's the same player bidding
	 */
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
	
	/**
	 * Attempt to raise this bid.
	 * 
	 * @param newBidAmount
	 * @return success of raising bid
	 */
	public Boolean raiseBid(Long newBidAmount) {
		if (newBidAmount <= maxBidAmount && newBidAmount >= bidAmount) {
			bidAmount = newBidAmount;
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Parses main bid argument.
	 * 
	 * @return acceptability of bid argument
	 */
	private Boolean parseArgBid() {
		if (args.length > 0) {
			if (!args[0].isEmpty() && args[0].matches(floAuction.decimalRegex)) {
				bidAmount = Functions.getSafeMoney(Double.parseDouble(args[0]));
				/*Should fix the bug that allowed over-sized payments
				 */
				if(bidAmount > floAuction.econ.getBalance(this.bidderName))
				{
					this.error = "bid-fail-cant-allocate-funds";
					return false;
				}

				if (bidAmount == 0) {
					error = "parse-error-invalid-bid";
					return false;
				}
			} else {
				error = "parse-error-invalid-bid";
				return false;
			}
		} else {
			if (auction.sealed || !AuctionConfig.getBoolean("allow-auto-bid", auction.getScope())) {
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
	
	/**
	 * Parse max bid argument.
	 * 
	 * @return acceptability of max bid
	 */
	private Boolean parseArgMaxBid() {
		if (!AuctionConfig.getBoolean("allow-max-bids", auction.getScope()) || auction.sealed) {
			// Just ignore it.
			maxBidAmount = bidAmount;
			return true;
		}
		if (args.length > 1) {
			if (!args[1].isEmpty() && args[1].matches(floAuction.decimalRegex)) {
				maxBidAmount = Functions.getSafeMoney(Double.parseDouble(args[1]));
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

	/**
	 * Gets the error which may have occurred.
	 * 
	 * @return the error
	 */
	public String getError() {
		return error;
	}
	
	/**
	 * Gets the name of the bidder.
	 * @return name of bidder
	 */
	public String getBidder() {
		return bidderName;
	}
	
	public String getBidderDisplayName() {
		Player bidderPlayer = Bukkit.getPlayer(bidderName);
		if (bidderPlayer != null) {
			return bidderPlayer.getDisplayName();
		} else {
			return bidderName;
		}
	}

	/**
	 * Gets the amount currently bid in floAuction's proprietary "safe money."
	 * 
	 * @return
	 */
	public long getBidAmount() {
		return bidAmount;
	}
	
	/**
	 * Gets the amount maximum bid for this instance in floAuction's proprietary "safe money."
	 * 
	 * @return
	 */
	public long getMaxBidAmount() {
		return maxBidAmount;
	}
}
