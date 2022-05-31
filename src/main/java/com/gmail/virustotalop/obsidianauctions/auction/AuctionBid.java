/*
 *     ObsidianAuctions
 *     Copyright (C) 2012-2022 flobi and contributors
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.gmail.virustotalop.obsidianauctions.auction;

import com.gmail.virustotalop.obsidianauctions.AuctionConfig;
import com.gmail.virustotalop.obsidianauctions.Key;
import com.gmail.virustotalop.obsidianauctions.ObsidianAuctions;
import com.gmail.virustotalop.obsidianauctions.util.Functions;
import com.gmail.virustotalop.obsidianauctions.util.Items;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;

/**
 * Structure to handle auction bids.
 *
 * @author Joshua "flobi" Hatfield
 */
public class AuctionBid {

    private final Auction auction;
    private final UUID bidderUUID;
    private final String bidderName;
    private long bidAmount = 0;
    private long maxBidAmount = 0;
    private Key error;
    private final String[] args;
    private double reserve = 0;

    /**
     * Constructor that validates bidder, parses arguments and reserves maximum bid funds.
     *
     * @param auction   the auction being bid upon
     * @param player    the player doing the bidding
     * @param inputArgs the parameters entered in chat
     */
    public AuctionBid(Auction auction, Player player, String[] inputArgs) {
        this.auction = auction;
        this.bidderUUID = player.getUniqueId();
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
    private boolean reserveBidFunds() {
        long amountToReserve = 0;
        long previousSealedReserve = 0;
        AuctionBid currentBid = this.auction.getCurrentBid();

        for (int i = 0; i < this.auction.getSealedBids().size(); i++) {
            if (this.auction.getSealedBids().get(i).getBidderName().equalsIgnoreCase(this.getBidderName())) {
                previousSealedReserve += this.auction.getSealedBids().get(i).getBidAmount();
                this.auction.getSealedBids().remove(i);
                i--;
            }
        }

        if (currentBid != null && currentBid.getBidderName().equalsIgnoreCase(bidderName)) {
            // Same bidder: only reserve difference.
            if (this.maxBidAmount > currentBid.getMaxBidAmount() + previousSealedReserve) {
                amountToReserve = this.maxBidAmount - currentBid.getMaxBidAmount() - previousSealedReserve;
            } else {
                // Nothing needing reservation.
                return true;
            }
        } else {
            amountToReserve = this.maxBidAmount;
        }
        if (Functions.withdrawPlayer(this.bidderUUID, amountToReserve)) {
            this.reserve = Functions.getUnsafeMoney(amountToReserve);
            return true;
        } else {
            this.error = Key.BID_FAIL_CANNOT_ALLOCATE_FUNDS;
            return false;
        }
    }

    /**
     * Refunds reserve for unsealed auctions or queues reserve refund for sealed auctions.
     */
    public void cancelBid() {
        if (this.auction.isSealed()) {
            // Queue reserve refund.
            this.auction.getSealedBids().add(this);
            ObsidianAuctions.get().getAuctionManager().addParticipant(this.getBidderUUID(), this.auction.getScope());
        } else {
            // Refund reserve.
            Functions.depositPlayer(this.bidderUUID, this.reserve);
            this.reserve = 0;
        }

    }

    /**
     * Process winning bid, gives winnings to auction owner, returns the remainder of the reserve and appropriates end of auction taxes.
     */
    public void winBid() {
        double unsafeBidAmount = Functions.getUnsafeMoney(this.bidAmount);

        // Extract taxes:
        double taxes = 0D;
        double taxPercent = AuctionConfig.getDouble(Key.AUCTION_END_TAX_PERCENT, this.auction.getScope());
        ItemStack typeStack = this.auction.getLotType();

        // TODO: Check this line for possible NULL
        for (Map.Entry<String, String> entry : AuctionConfig.getStringStringMap(Key.TAXED_ITEMS, this.auction.getScope()).entrySet()) {
            if (Items.isSameItem(typeStack, entry.getKey())) {
                if (entry.getValue().endsWith("%")) {
                    try {
                        taxPercent = Double.parseDouble(entry.getValue().substring(0, entry.getValue().length() - 1));
                    } catch (Exception e) {
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
            this.auction.setExtractedPostTax(taxes);
            this.auction.getMessageManager().sendPlayerMessage(Key.AUCTION_END_TAX, this.auction.getOwnerUUID(), this.auction);
            unsafeBidAmount -= taxes;
            UUID taxDestinationUser = AuctionConfig.getUUID(Key.DEPOSIT_TAX_TO_USER, this.auction.getScope());
            if (taxDestinationUser != null) {
                Functions.depositPlayer(taxDestinationUser, taxes);
            }
        }

        // Apply winnings to auction owner.
        Functions.depositPlayer(this.auction.getOwnerUUID(), unsafeBidAmount);

        // Refund remaining reserve.
        Functions.depositPlayer(this.bidderUUID, this.reserve - unsafeBidAmount - taxes);

        this.reserve = 0;
    }

    /**
     * Checks existence, permission, scope and prohibitions on player trying to bid.
     *
     * @return whether player can bid
     */
    private boolean validateBidder() {
        if (this.bidderName == null) {
            this.error = Key.BID_FAIL_NO_BIDDER;
            return false;
        } else if (ObsidianAuctions.get().getProhibitionManager().isOnProhibition(this.bidderUUID, false)) {
            this.error = Key.REMOTE_PLUGIN_PROHIBITION_REMINDER;
            return false;
        } else if (!ObsidianAuctions.get().getAuctionLocationManager().checkLocation(this.bidderUUID)) {
            this.error = Key.BID_FAIL_OUTSIDE_AUCTIONHOUSE;
            return false;
        } else if (bidderUUID.equals(auction.getOwnerUUID()) && !AuctionConfig.getBoolean(Key.ALLOW_BID_ON_OWN_AUCTION, this.auction.getScope())) {
            this.error = Key.BID_FAIL_IS_AUCTION_OWNER;
            return false;
        }
        return true;
    }

    /**
     * Parses bid arguments.
     *
     * @return whether args are acceptable
     */
    private boolean parseArgs() {
        if (!parseArgBid()) {
            return false;
        } else return parseArgMaxBid();
    }

    /**
     * Prepares two bids from the same player to compete against each other.
     *
     * @param otherBid the previous bid
     * @return whether it's the same player bidding
     */
    public boolean raiseOwnBid(AuctionBid otherBid) {
        if (this.bidderName.equalsIgnoreCase(otherBid.bidderName)) {
            // Move reserve money here.
            this.reserve = this.reserve + otherBid.reserve;
            otherBid.reserve = 0;

            // Maxbid only updates up.
            this.maxBidAmount = Math.max(this.maxBidAmount, otherBid.maxBidAmount);
            otherBid.maxBidAmount = this.maxBidAmount;

            if (this.bidAmount > otherBid.bidAmount) {
                // The bid has been raised.
                return true;
            } else {
                // Put the reserve on the other bid because we're cancelling this one.
                otherBid.reserve = this.reserve;
                this.reserve = 0;
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
    public boolean raiseBid(Long newBidAmount) {
        if (newBidAmount <= this.maxBidAmount && newBidAmount >= this.bidAmount) {
            this.bidAmount = newBidAmount;
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
    private boolean parseArgBid() {
        if (this.args.length > 0) {
            if (!this.args[0].isEmpty() && args[0].matches(ObsidianAuctions.decimalRegex)) {
                this.bidAmount = Functions.getSafeMoney(Double.parseDouble(this.args[0]));
                /*Should fix the bug that allowed over-sized payments
                 */
                if (!Functions.hasBalance(this.bidderUUID, this.bidAmount)) {
                    this.error = Key.BID_FAIL_CANNOT_ALLOCATE_FUNDS;
                    return false;
                } else if (this.bidAmount == 0) {
                    this.error = Key.PARSE_ERROR_INVALID_BID;
                    return false;
                }
            } else {
                this.error = Key.PARSE_ERROR_INVALID_BID;
                return false;
            }
        } else {
            if (this.auction.isSealed() || !AuctionConfig.getBoolean(Key.ALLOW_AUTO_BID, this.auction.getScope())) {
                this.error = Key.BID_FAIL_BID_REQUIRED;
                return false;
            } else {
                // Leaving it up to automatic:
                this.bidAmount = 0;
            }
        }
        // If the person bids 0, make it automatically the next increment (unless it's the current bidder).
        if (this.bidAmount == 0) {
            AuctionBid currentBid = this.auction.getCurrentBid();
            if (currentBid == null) {
                // Use the starting bid if no one has bid yet.
                this.bidAmount = auction.getStartingBid();
                if (this.bidAmount == 0) {
                    // Unless the starting bid is 0, then use the minimum bid increment.
                    this.bidAmount = auction.getMinBidIncrement();
                }
            } else if (currentBid.getBidderName().equalsIgnoreCase(this.bidderName)) {
                // We are the current bidder, so use previous.  Don't auto-up our own bid.
                this.bidAmount = currentBid.bidAmount;
            } else {
                this.bidAmount = currentBid.getBidAmount() + this.auction.getMinBidIncrement();
            }
        }
        if (this.bidAmount <= 0) {
            this.error = Key.PARSE_ERROR_INVALID_BID;
            return false;
        }
        return true;
    }

    /**
     * Parse max bid argument.
     *
     * @return acceptability of max bid
     */
    private boolean parseArgMaxBid() {
        if (!AuctionConfig.getBoolean(Key.ALLOW_MAX_BIDS, this.auction.getScope()) || this.auction.isSealed()) {
            // Just ignore it.
            this.maxBidAmount = this.bidAmount;
            return true;
        }
        if (this.args.length > 1) {
            if (!args[1].isEmpty() && args[1].matches(ObsidianAuctions.decimalRegex)) {
                this.maxBidAmount = Functions.getSafeMoney(Double.parseDouble(this.args[1]));
            } else {
                this.error = Key.PARSE_ERROR_INVALID_MAX_BID;
                return false;
            }
        }
        this.maxBidAmount = Math.max(this.bidAmount, this.maxBidAmount);
        if (this.maxBidAmount <= 0) {
            this.error = Key.PARSE_ERROR_INVALID_MAX_BID;
            return false;
        }
        return true;
    }

    /**
     * Gets the error which may have occurred.
     *
     * @return the error
     */
    public Key getError() {
        return this.error;
    }

    /**
     * Gets the uuid of the bidder.
     *
     * @return uuid of bidder
     */
    public UUID getBidderUUID() {
        return this.bidderUUID;
    }

    /**
     * Gets the name of the bidder.
     *
     * @return name of bidder
     */
    public String getBidderName() {
        return this.bidderName;
    }

    public String getBidderDisplayName() {
        Player bidderPlayer = Bukkit.getPlayer(this.bidderName);
        if (bidderPlayer != null) {
            return bidderPlayer.getDisplayName();
        } else {
            return this.bidderName;
        }
    }

    /**
     * Gets the amount currently bid in floAuction's proprietary "safe money."
     *
     * @return
     */
    public long getBidAmount() {
        return this.bidAmount;
    }

    /**
     * Gets the amount maximum bid for this instance in floAuction's proprietary "safe money."
     *
     * @return
     */
    public long getMaxBidAmount() {
        return this.maxBidAmount;
    }
}
