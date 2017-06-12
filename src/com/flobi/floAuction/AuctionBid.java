package com.flobi.floauction;

import java.util.Map;

import me.virustotal.floauction.utility.CArrayList;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.flobi.floauction.utilities.Functions;
import com.flobi.floauction.utilities.Items;

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
		this.bidderName = player.getName();
		this.args = inputArgs;
		if (!validateBidder()) return;
		if (!parseArgs()) return;
		if (!reserveBidFunds()) return;
	}
	
	/**
	 * Removes funds from bidder's account and stores them in the AuctionBid instance.
	 * 
	 * @return true if funds are reserved, false if an issue was encountered
	 */
	private boolean reserveBidFunds() 
	{
		long amountToReserve = 0;
		long previousSealedReserve = 0;
		AuctionBid currentBid = this.auction.getCurrentBid();
		
		for(int i = 0; i < this.auction.sealedBids.size(); i++) 
		{
			if (this.auction.sealedBids.get(i).getBidder().equalsIgnoreCase(this.getBidder())) 
			{
				previousSealedReserve += this.auction.sealedBids.get(i).getBidAmount();
				this.auction.sealedBids.remove(i);
				i--;
			}
		}
		
		if (currentBid != null && currentBid.getBidder().equalsIgnoreCase(bidderName)) 
		{
			// Same bidder: only reserve difference.
			if (this.maxBidAmount > currentBid.getMaxBidAmount() + previousSealedReserve) 
			{
				amountToReserve = this.maxBidAmount - currentBid.getMaxBidAmount() - previousSealedReserve;
			} 
			else 
			{
				// Nothing needing reservation.
				return true;
			}
		} 
		else 
		{
			amountToReserve = this.maxBidAmount;
		}
		if (Functions.withdrawPlayer(this.bidderName, amountToReserve)) 
		{
			this.reserve = Functions.getUnsafeMoney(amountToReserve);
			return true;
		} 
		else 
		{
			this.error = "bid-fail-cant-allocate-funds";
			return false;
		}
	}
	
	/**
	 * Refunds reserve for unsealed auctions or queues reserve refund for sealed auctions. 
	 */
	public void cancelBid() 
	{
		if (this.auction.sealed) 
		{
			// Queue reserve refund.
			this.auction.sealedBids.add(this);
			AuctionParticipant.addParticipant(getBidder(), this.auction.getScope());
		} 
		else 
		{
			// Refund reserve.
			Functions.depositPlayer(this.bidderName, this.reserve);
			this.reserve = 0;
		}
		
	}
	
	/**
	 * Process winning bid, gives winnings to auction owner, returns the remainder of the reserve and appropriates end of auction taxes.
	 */
	public void winBid() 
	{
		Double unsafeBidAmount = Functions.getUnsafeMoney(this.bidAmount);
		
		// Extract taxes:
		Double taxes = 0D;
		double taxPercent = AuctionConfig.getDouble("auction-end-tax-percent", this.auction.getScope()); 
		ItemStack typeStack = this.auction.getLotType();

		// TODO: Check this line for possible NULL
		for (Map.Entry<String, String> entry : AuctionConfig.getStringStringMap("taxed-items", this.auction.getScope()).entrySet()) 
		{
			if (Items.isSameItem(typeStack, entry.getKey())) 
			{
				if (entry.getValue().endsWith("%")) 
				{
					try 
					{
						taxPercent = Double.valueOf(entry.getValue().substring(0, entry.getValue().length() - 1));
					} 
					catch (Exception e) 
					{
						/* Clearly this isn't a valid number, just forget about it.
						   taxPercent = AuctionConfig.getDouble("auction-end-tax-percent", auction.getScope());
						   On second thought, this is already the value, so just keep it.
						 */
					}
				}
				break;
			}
		}
		
		
		if (taxPercent > 0D) {
			taxes = unsafeBidAmount * (taxPercent / 100D);
			
			this.auction.extractedPostTax = taxes;
			this.auction.messageManager.sendPlayerMessage(new CArrayList<String>("auction-end-tax"), this.auction.getOwner(), this.auction);
			unsafeBidAmount -= taxes;
			String taxDestinationUser = AuctionConfig.getString("deposit-tax-to-user", this.auction.getScope());
			if (!taxDestinationUser.isEmpty()) FloAuction.econ.depositPlayer(taxDestinationUser, taxes);
		}
		
		// Apply winnings to auction owner.
		FloAuction.econ.depositPlayer(this.auction.getOwner(), unsafeBidAmount);

		// Refund remaining reserve.
		FloAuction.econ.depositPlayer(this.bidderName, this.reserve - unsafeBidAmount - taxes);
		
		this.reserve = 0;
	}
	
	/**
	 * Checks existence, permission, scope and prohibitions on player trying to bid.
	 * 
	 * @return whether player can bid
	 */
	private Boolean validateBidder() 
	{
		if (this.bidderName == null) 
		{
			this.error = "bid-fail-no-bidder";
			return false;
		}
		else if (AuctionProhibition.isOnProhibition(this.bidderName, false)) 
		{
			this.error = "remote-plugin-prohibition-reminder";
			return false;
		}
		else if (!AuctionParticipant.checkLocation(this.bidderName)) 
		{
			this.error = "bid-fail-outside-auctionhouse";
			return false;
		}
		else if (bidderName.equalsIgnoreCase(auction.getOwner()) && !AuctionConfig.getBoolean("allow-bid-on-own-auction", this.auction.getScope())) 
		{
			this.error = "bid-fail-is-auction-owner";
			return false;
		}
		return true;
	}
	
	/**
	 * Parses bid arguments.
	 * 
	 * @return whether args are acceptable
	 */
	private Boolean parseArgs() 
	{
		if (!parseArgBid()) 
		{
			return false;
		}
		else if (!parseArgMaxBid()) 
		{
			return false;
		}
		return true;
	}
	
	/**
	 * Prepares two bids from the same player to compete against each other.
	 * 
	 * @param otherBid the previous bid
	 * @return whether it's the same player bidding
	 */
	public Boolean raiseOwnBid(AuctionBid otherBid) 
	{
		if (this.bidderName.equalsIgnoreCase(otherBid.bidderName)) 
		{
			// Move reserve money here.
			this.reserve = this.reserve + otherBid.reserve;
			otherBid.reserve = 0;

			// Maxbid only updates up.
			this.maxBidAmount = Math.max(this.maxBidAmount, otherBid.maxBidAmount);
			otherBid.maxBidAmount = this.maxBidAmount;

			if (this.bidAmount > otherBid.bidAmount) 
			{
				// The bid has been raised.
				return true;
			}
			else 
			{
				// Put the reserve on the other bid because we're cancelling this one.
				otherBid.reserve = this.reserve;
				this.reserve = 0;
				return false;
			}
		} 
		else 
		{
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
	public Boolean raiseBid(Long newBidAmount) 
	{
		if (newBidAmount <= this.maxBidAmount && newBidAmount >= this.bidAmount) 
		{
			this.bidAmount = newBidAmount;
			return true;
		} 
		else 
		{
			return false;
		}
	}
	
	/**
	 * Parses main bid argument.
	 * 
	 * @return acceptability of bid argument
	 */
	private Boolean parseArgBid() 
	{
		if (this.args.length > 0) 
		{
			if (!this.args[0].isEmpty() && args[0].matches(FloAuction.decimalRegex)) 
			{
				this.bidAmount = Functions.getSafeMoney(Double.parseDouble(this.args[0]));
				/*Should fix the bug that allowed over-sized payments
				 */
				if(this.bidAmount > FloAuction.econ.getBalance(this.bidderName))
				{
					this.error = "bid-fail-cant-allocate-funds";
					return false;
				}
				else if (this.bidAmount == 0) 
				{
					this.error = "parse-error-invalid-bid";
					return false;
				}
			} 
			else 
			{
				this.error = "parse-error-invalid-bid";
				return false;
			}
		} 
		else 
		{
			if (this.auction.sealed || !AuctionConfig.getBoolean("allow-auto-bid", this.auction.getScope())) 
			{
				this.error = "bid-fail-bid-required";
				return false;
			} 
			else 
			{
				// Leaving it up to automatic:
				this.bidAmount = 0;
			}
		}
		// If the person bids 0, make it automatically the next increment (unless it's the current bidder).
		if (this.bidAmount == 0) 
		{
			AuctionBid currentBid = this.auction.getCurrentBid();
			if (currentBid == null) 
			{
				// Use the starting bid if no one has bid yet.
				this.bidAmount = auction.getStartingBid();
				if (this.bidAmount == 0) 
				{
					// Unless the starting bid is 0, then use the minimum bid increment.
					this.bidAmount = auction.getMinBidIncrement();
				}
			} else if (currentBid.getBidder().equalsIgnoreCase(this.bidderName)) 
			{
				// We are the current bidder, so use previous.  Don't auto-up our own bid.
				this.bidAmount = currentBid.bidAmount;
			} 
			else 
			{
				this.bidAmount = currentBid.getBidAmount() + this.auction.getMinBidIncrement();
			}
		}
		if (this.bidAmount <= 0) 
		{
			this.error = "parse-error-invalid-bid";
			return false;
		}
		return true;
	}
	
	/**
	 * Parse max bid argument.
	 * 
	 * @return acceptability of max bid
	 */
	private Boolean parseArgMaxBid() 
	{
		if (!AuctionConfig.getBoolean("allow-max-bids", this.auction.getScope()) || this.auction.sealed) 
		{
			// Just ignore it.
			this.maxBidAmount = this.bidAmount;
			return true;
		}
		if (this.args.length > 1) 
		{
			if (!args[1].isEmpty() && args[1].matches(FloAuction.decimalRegex)) 
			{
				this.maxBidAmount = Functions.getSafeMoney(Double.parseDouble(this.args[1]));
			} 
			else 
			{
				this.error = "parse-error-invalid-max-bid";
				return false;
			}
		}
		this.maxBidAmount = Math.max(this.bidAmount, this.maxBidAmount);
		if (this.maxBidAmount <= 0) {
			this.error = "parse-error-invalid-max-bid";
			return false;
		}
		return true;
	}

	/**
	 * Gets the error which may have occurred.
	 * 
	 * @return the error
	 */
	public String getError() 
	{
		return this.error;
	}
	
	/**
	 * Gets the name of the bidder.
	 * @return name of bidder
	 */
	public String getBidder() 
	{
		return this.bidderName;
	}
	
	public String getBidderDisplayName() 
	{
		Player bidderPlayer = Bukkit.getPlayer(this.bidderName);
		if (bidderPlayer != null) 
		{
			return bidderPlayer.getDisplayName();
		} 
		else 
		{
			return this.bidderName;
		}
	}

	/**
	 * Gets the amount currently bid in FloAuction's proprietary "safe money."
	 * 
	 * @return
	 */
	public long getBidAmount() 
	{
		return this.bidAmount;
	}
	
	/**
	 * Gets the amount maximum bid for this instance in FloAuction's proprietary "safe money."
	 * 
	 * @return
	 */
	public long getMaxBidAmount() 
	{
		return this.maxBidAmount;
	}
}
