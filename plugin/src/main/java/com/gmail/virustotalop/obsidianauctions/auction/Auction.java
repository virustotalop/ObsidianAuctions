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
import com.gmail.virustotalop.obsidianauctions.event.AuctionBidEvent;
import com.gmail.virustotalop.obsidianauctions.event.AuctionEndEvent;
import com.gmail.virustotalop.obsidianauctions.event.AuctionStartEvent;
import com.gmail.virustotalop.obsidianauctions.message.MessageManager;
import com.gmail.virustotalop.obsidianauctions.util.AdventureUtil;
import com.gmail.virustotalop.obsidianauctions.util.Functions;
import com.gmail.virustotalop.obsidianauctions.util.Items;
import com.gmail.virustotalop.obsidianauctions.util.LegacyUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Main auction class.
 *
 * @author Joshua "flobi" Hatfield
 */
public class Auction {

    private final ObsidianAuctions plugin;
    private final String[] args;
    private UUID ownerUUID;
    private String ownerName;
    private final AuctionScope scope;

    private double extractedPreTax = 0;
    private double extractedPostTax = 0;

    private long startingBid = 0;
    private long minBidIncrement = 0;
    private long buyNow = 0;
    private int quantity = 0;
    private int time = 0;
    private boolean active = false;

    private AuctionLot lot;
    private AuctionBid currentBid = null;
    private final List<AuctionBid> sealedBids = new ArrayList<>();

    private boolean sealed;

    private long nextTickTime = 0;

    // Scheduled timers:
    private int countdown = 0;
    private int countdownTimer = 0;

    //added
    private final ItemStack guiItem;
    private final MessageManager messageManager;

    /**
     * Gets the AuctionScope which hosts this auction.
     *
     * @return the hosting AuctionScope
     */
    public AuctionScope getScope() {
        return this.scope;
    }

    /**
     * Instantiates an auction instance.
     *
     * @param plugin       the active floAuction plugin instance
     * @param auctionOwner the player who is starting the auction
     * @param inputArgs    the command parameters entered in chat
     * @param scope        the hosting AuctionScope
     * @param sealed       whether it is a sealed auction
     */
    public Auction(ObsidianAuctions plugin, Player auctionOwner, String[] inputArgs, AuctionScope scope, boolean sealed, MessageManager messageManager, ItemStack guiItem) {
        this.ownerUUID = auctionOwner.getUniqueId();
        this.ownerName = auctionOwner.getName();
        this.args = Functions.mergeInputArgs(auctionOwner.getUniqueId(), inputArgs, false);
        this.plugin = plugin;
        this.scope = scope;
        this.sealed = sealed;
        this.messageManager = messageManager;
        this.guiItem = this.buildGuiItem(guiItem);
    }

    private ItemStack buildGuiItem(ItemStack guiItem) {
        guiItem = this.addLore(guiItem);
        return guiItem;
    }

    private ItemStack addLore(ItemStack guiItem) {
        ItemMeta itemMeta = guiItem.getItemMeta();
        boolean hasLore = itemMeta.hasLore();
        List<String> lore = hasLore ? itemMeta.getLore() : new ArrayList<>();
        lore.add(AdventureUtil.miniToLegacy(
                AuctionConfig.getLanguageString(Key.QUEUE_GUI_ITEM_AUCTIONED_BY, this.scope)
                .replace("%player-name%", this.ownerName)));
        itemMeta.setLore(lore);
        guiItem.setItemMeta(itemMeta);
        return guiItem;
    }

    /**
     * Attempts to start this auction instance.  Returns success.
     *
     * @return whether or not the auction start succeeded
     */
    public boolean start() {
        Player owner = this.plugin.getServer().getPlayer(this.ownerUUID);

        if (ObsidianAuctions.get().getAuctionLocationManager().isInArena(owner)) {
            this.messageManager.sendPlayerMessage(Key.AUCTION_FAIL_ARENA, this.ownerUUID, this);
            return false;
        }

        ItemStack typeStack = this.lot.getTypeStack();
        double preAuctionTax = AuctionConfig.getDouble(Key.AUCTION_START_TAX, this.scope);

        // Check banned items:
        List<String> bannedItems = AuctionConfig.getStringList(Key.BANNED_ITEMS, this.scope);
        for (String bannedItem : bannedItems) {
            if (Items.isSameItem(typeStack, bannedItem)) {
                this.messageManager.sendPlayerMessage(Key.AUCTION_FAIL_BANNED, this.ownerUUID, this);
                return false;
            }
        }

        Map<String, String> taxedItems = AuctionConfig.getStringStringMap(Key.TAXED_ITEMS, this.scope);
        for (Map.Entry<String, String> entry : taxedItems.entrySet()) {
            if (Items.isSameItem(typeStack, entry.getKey())) {
                String itemTax = entry.getValue();

                if (itemTax.endsWith("a")) {
                    try {
                        preAuctionTax = Double.parseDouble(itemTax.substring(0, itemTax.length() - 1));
                    } catch (Exception e) {
                        // Clearly this isn't a valid number, just forget about it.
                        preAuctionTax = AuctionConfig.getDouble(Key.AUCTION_START_TAX, this.scope);
                    }
                } else if (!itemTax.endsWith("%")) {
                    try {
                        preAuctionTax = Double.parseDouble(itemTax);
                        preAuctionTax *= this.quantity;
                    } catch (Exception e) {
                        // Clearly this isn't a valid number, just forget about it.
                        preAuctionTax = AuctionConfig.getDouble(Key.AUCTION_START_TAX, this.scope);
                    }
                }
                break;
            }
        }

        if (preAuctionTax > 0D) {
            if (!Functions.hasBalance(this.ownerUUID, preAuctionTax)) {
                this.messageManager.sendPlayerMessage(Key.AUCTION_FAIL_START_TAX, this.ownerUUID, this);
                return false;
            }
        }

        if (!this.lot.addItems(this.quantity, true)) {
            this.messageManager.sendPlayerMessage(Key.AUCTION_FAIL_INSUFFICIENT_SUPPLY, this.ownerUUID, this);
            return false;
        }

        if (preAuctionTax > 0D) {
            if (Functions.hasBalance(this.ownerUUID, preAuctionTax)) {
                Functions.withdrawPlayer(this.ownerUUID, preAuctionTax);
                this.extractedPreTax = preAuctionTax;
                this.messageManager.sendPlayerMessage(Key.AUCTION_START_TAX, this.ownerUUID, this);
                UUID taxDestinationUser = AuctionConfig.getUUID(Key.DEPOSIT_TAX_TO_USER, scope);
                if (taxDestinationUser != null) {
                    Functions.depositPlayer(taxDestinationUser, preAuctionTax);
                }
            }
        }

        if (this.buyNow < getStartingBid()) {
            this.buyNow = 0;
        }

        // Check to see if any other plugins have a reason...or they can forever hold their piece.
        AuctionStartEvent auctionStartEvent = new AuctionStartEvent(owner, this);
        this.plugin.getServer().getPluginManager().callEvent(auctionStartEvent);

        if (auctionStartEvent.isCancelled()) {
            this.messageManager.sendPlayerMessage(Key.AUCTION_FAIL_BLOCKED_BY_OTHER_PLUGIN, this.ownerUUID, this);
        } else {
            this.active = true;
            this.messageManager.broadcastAuctionMessage(Key.AUCTION_START, this);

            // Set timer:
            final Auction thisAuction = this;
            this.countdown = this.time;

            this.countdownTimer = this.plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
                if (thisAuction.nextTickTime > System.currentTimeMillis()) {
                    return;
                }
                thisAuction.nextTickTime = thisAuction.nextTickTime + 1000;

                thisAuction.countdown--;
                if (thisAuction.countdown <= 0) {
                    thisAuction.end();
                    return;
                }
                if (!AuctionConfig.getBoolean(Key.SUPPRESS_COUNTDOWN, scope)) {
                    if (thisAuction.countdown < 4) {
                        messageManager.broadcastAuctionMessage(Key.TIMER_COUNTDOWN_NOTIFICATION, thisAuction);
                        return;
                    }
                    if (thisAuction.time >= 20) {
                        if (thisAuction.countdown == (thisAuction.time / 2)) {
                            messageManager.broadcastAuctionMessage(Key.TIMER_COUNTDOWN_NOTIFICATION, thisAuction);
                        }
                    }
                }
            }, 1L, 1L);
            this.nextTickTime = System.currentTimeMillis() + 1000;

            info(null, true);
        }

        return this.active;
    }

    /**
     * Sends auction info to chat.
     *
     * @param sender        the CommandSender initiating the request
     * @param fullBroadcast whether to send the message to everyone in the hosting AuctionScope
     */
    public void info(CommandSender sender, boolean fullBroadcast) {
        List<Key> messageKeys = new ArrayList<>();
        UUID playerUUID = null;
        if (sender instanceof Player) {
            playerUUID = ((Player) sender).getUniqueId();
        }
        if (!this.active) {
            if (sender instanceof Player) {
                this.messageManager.sendPlayerMessage(Key.AUCTION_INFO_NO_AUCTION, playerUUID, this);
            }
            return;
        } else {
            messageKeys.add(Key.AUCTION_INFO);
        }
        if (fullBroadcast) {
            this.messageManager.broadcastAuctionMessage(messageKeys, this);
        } else {
            this.messageManager.sendPlayerMessage(messageKeys, playerUUID, this);
        }
    }

    /**
     * Cancels the Auction instance and disposes of it normally.
     */
    public void cancel() {
        this.plugin.getServer().getPluginManager().callEvent(new AuctionEndEvent(this, true));
        this.messageManager.broadcastAuctionMessage(Key.AUCTION_CANCEL, this);

        if (this.lot != null) {
            this.lot.cancelLot();
        }
        if (this.currentBid != null) {
            this.currentBid.cancelBid();
        }
        this.dispose();
    }

    /**
     * Cancels the Auction instance redirecting all goods to an approved authority.
     * If the authority is not approved, Auction instance will not be cancelled.
     *
     * @param authority the name of a player authorized to confiscate auctions
     */
    public void confiscate(Player authority) {
        this.plugin.getServer().getPluginManager().callEvent(new AuctionEndEvent(this, true));
        this.ownerName = authority.getName();
        this.ownerUUID = authority.getUniqueId();
        this.messageManager.broadcastAuctionMessage(Key.CONFISCATE_SUCCESS, this);
        if (this.lot != null) {
            this.lot.winLot(authority.getUniqueId(), authority.getName());
        }
        if (this.currentBid != null) {
            this.currentBid.cancelBid();
        }
        this.dispose();
    }

    /**
     * Ends an auction normally sending money and goods to their earned destinations.
     */
    public void end() {
        AuctionEndEvent auctionEndEvent = new AuctionEndEvent(this, false);
        this.plugin.getServer().getPluginManager().callEvent(auctionEndEvent);
        if (auctionEndEvent.isCancelled()) {
            this.messageManager.broadcastAuctionMessage(Key.AUCTION_CANCEL, this);
            if (this.lot != null) {
                this.lot.cancelLot();
            }
            if (this.currentBid != null) {
                this.currentBid.cancelBid();
            }
        } else {
            if (this.currentBid == null || this.lot == null) {
                this.messageManager.broadcastAuctionMessage(Key.AUCTION_END_NOBIDS, this);
                if (this.lot != null) {
                    this.lot.cancelLot();
                }
                if (this.currentBid != null) {
                    this.currentBid.cancelBid();
                }
            } else {
                this.messageManager.broadcastAuctionMessage(Key.AUCTION_END, this);
                this.lot.winLot(currentBid.getBidderUUID(), currentBid.getBidderName());
                this.currentBid.winBid();
            }
        }
        this.dispose();
    }

    /**
     * Disposes of the remains of a terminated auction, purging the timer, refunding sealed bid losers and removing self from host scope.
     */
    private void dispose() {
        this.plugin.getServer().getScheduler().cancelTask(this.countdownTimer);
        this.sealed = false;
        for (AuctionBid sealedBid : this.sealedBids) {
            sealedBid.cancelBid();
        }
        this.scope.setActiveAuction(null);
    }

    /**
     * Checks all auction parameters and environment factors to determine if the Auction instance can legitimately start.
     *
     * @return whether the auction can begin
     */
    public boolean isValid() {
        if (!isValidOwner()) {
            return false;
        } else if (!parseHeldItem()) {
            return false;
        } else if (!parseArgs()) {
            return false;
        } else if (!isValidAmount()) {
            return false;
        } else if (!isValidStartingBid()) {
            return false;
        } else if (!isValidIncrement()) {
            return false;
        } else if (!isValidTime()) {
            return false;
        }
        return isValidBuyNow();
    }

    /**
     * Parses a bid command.
     *
     * @param bidder    Player attempting to bid
     * @param inputArgs parameters entered in chat
     */
    public void bid(Player bidder, String[] inputArgs) {
        if (bidder == null) {
            return;
        }
        UUID playerUUID = bidder.getUniqueId();

        if (ObsidianAuctions.get().getAuctionLocationManager().isInArena(bidder)) {
            this.messageManager.sendPlayerMessage(Key.BID_FAIL_ARENA, playerUUID, this);
            return;
        }

        // BuyNow
        if (AuctionConfig.getBoolean(Key.ALLOW_BUYNOW, scope) && inputArgs.length > 0) {
            if (inputArgs[0].equalsIgnoreCase("buy")) {

                if (this.buyNow == 0 || (this.currentBid != null && currentBid.getBidAmount() >= this.buyNow)) {
                    this.messageManager.sendPlayerMessage(Key.BID_FAIL_BUYNOW_EXPIRED, playerUUID, this);
                } else {
                    inputArgs[0] = Double.toString(Functions.getUnsafeMoney(this.buyNow));
                    if (inputArgs[0].endsWith(".0")) {
                        inputArgs[0] = inputArgs[0].substring(0, inputArgs[0].length() - 2);
                    }
                    AuctionBid bid = new AuctionBid(this, bidder, inputArgs);
                    if (bid.getError() != null) {
                        failBid(bid, bid.getError());
                        return;
                    } else {
                        // raisOwnBid does nothing if it's not the current bidder.
                        if (this.currentBid != null) {
                            bid.raiseOwnBid(this.currentBid);
                        }

                        // Let other plugins figure out any reasons why this buy shouldn't happen.
                        AuctionBidEvent auctionBidEvent = new AuctionBidEvent(bidder, this, Functions.getUnsafeMoney(bid.getBidAmount()), Functions.getUnsafeMoney(bid.getMaxBidAmount()), true);
                        this.plugin.getServer().getPluginManager().callEvent(auctionBidEvent);
                        if (auctionBidEvent.isCancelled()) {
                            this.failBid(bid, Key.BID_FAIL_BLOCKED_BY_OTHER_PLUGIN);
                        } else {
                            this.setNewBid(bid, null);
                            this.end();
                        }
                    }
                }
                return;
            }
        }

        // Normal bid
        AuctionBid bid = new AuctionBid(this, bidder, inputArgs);
        if (bid.getError() != null) {
            this.failBid(bid, bid.getError());
            return;
        }

        if (this.currentBid == null) {
            if (bid.getBidAmount() < getStartingBid()) {
                this.failBid(bid, Key.BID_FAIL_UNDER_STARTING_BID);
                return;
            }
            // Let other plugins figure out any reasons why this buy shouldn't happen.
            AuctionBidEvent auctionBidEvent = new AuctionBidEvent(bidder, this, Functions.getUnsafeMoney(bid.getBidAmount()), Functions.getUnsafeMoney(bid.getMaxBidAmount()), true);
            this.plugin.getServer().getPluginManager().callEvent(auctionBidEvent);
            if (auctionBidEvent.isCancelled()) {
                failBid(bid, Key.BID_FAIL_BLOCKED_BY_OTHER_PLUGIN);
            } else {
                setNewBid(bid, Key.BID_SUCCESS_NO_CHALLENGER);
            }
            return;
        }

        long previousBidAmount = this.currentBid.getBidAmount();
        long previousMaxBidAmount = this.currentBid.getMaxBidAmount();
        if (this.currentBid.getBidderName().equals(bidder.getName())) {
            if (bid.raiseOwnBid(this.currentBid)) {
                // Let other plugins figure out any reasons why this buy shouldn't happen.
                AuctionBidEvent auctionBidEvent = new AuctionBidEvent(bidder, this, Functions.getUnsafeMoney(bid.getBidAmount()), Functions.getUnsafeMoney(bid.getMaxBidAmount()), true);
                this.plugin.getServer().getPluginManager().callEvent(auctionBidEvent);
                if (auctionBidEvent.isCancelled()) {
                    this.failBid(bid, Key.BID_FAIL_BLOCKED_BY_OTHER_PLUGIN);
                } else {
                    this.setNewBid(bid, Key.BID_SUCCESS_UPDATE_OWN_BID);
                }
            } else {
                if (previousMaxBidAmount < currentBid.getMaxBidAmount()) {
                    this.failBid(bid, Key.BID_SUCCESS_UPDATE_OWN_MAXBID);
                } else {
                    this.failBid(bid, Key.BID_FAIL_ALREADY_CURRENT_BIDDER);
                }
            }
            return;
        }

        AuctionBid winner;
        AuctionBid loser;

        if (AuctionConfig.getBoolean(Key.USE_OLD_BID_LOGIC, this.scope)) {
            if (bid.getMaxBidAmount() > this.currentBid.getMaxBidAmount()) {
                winner = bid;
                loser = this.currentBid;
            } else {
                winner = this.currentBid;
                loser = bid;
            }
            winner.raiseBid(Math.max(winner.getBidAmount(), Math.min(winner.getMaxBidAmount(), loser.getBidAmount() + this.minBidIncrement)));
        } else {
            // If you follow what this does, congratulations.
            long baseBid = Math.max(bid.getBidAmount(), this.currentBid.getBidAmount() + this.minBidIncrement);

            int prevSteps = (int) Math.floor((double) (this.currentBid.getMaxBidAmount() - baseBid + this.minBidIncrement) / this.minBidIncrement / 2);
            int newSteps = (int) Math.floor((double) (bid.getMaxBidAmount() - baseBid) / this.minBidIncrement / 2);

            if (newSteps >= prevSteps) {
                winner = bid;
                winner.raiseBid(baseBid + (Math.max(0, prevSteps) * this.minBidIncrement * 2));
                loser = this.currentBid;
            } else {
                winner = this.currentBid;
                winner.raiseBid(baseBid + (Math.max(0, newSteps + 1) * this.minBidIncrement * 2) - this.minBidIncrement);
                loser = bid;
            }
        }

        if (previousBidAmount <= winner.getBidAmount()) {
            // Did the new bid win?
            if (winner.equals(bid)) {
                // Let other plugins figure out any reasons why this buy shouldn't happen.
                AuctionBidEvent auctionBidEvent = new AuctionBidEvent(bidder, this, Functions.getUnsafeMoney(bid.getBidAmount()), Functions.getUnsafeMoney(bid.getMaxBidAmount()), true);
                this.plugin.getServer().getPluginManager().callEvent(auctionBidEvent);
                if (auctionBidEvent.isCancelled()) {
                    failBid(bid, Key.BID_FAIL_BLOCKED_BY_OTHER_PLUGIN);
                } else {
                    setNewBid(bid, Key.BID_SUCCESS_OUTBID);
                }
            } else {
                // Did the old bid have to raise the bid to stay winner?
                if (previousBidAmount < winner.getBidAmount()) {
                    if (!this.sealed && !AuctionConfig.getBoolean(Key.BROADCAST_BID_UPDATES, scope)) {
                        this.messageManager.broadcastAuctionMessage(Key.BID_AUTO_OUTBID, this);
                    }
                    failBid(bid, Key.BID_FAIL_AUTO_OUTBID);
                } else {
                    if (!this.sealed) {
                        this.messageManager.sendPlayerMessage(Key.BID_FAIL_TOO_LOW, bid.getBidderUUID(), this);
                    }
                    this.failBid(bid, null);
                }
            }
        } else {
            // Seriously don't know what could cause this, but might as well take care of it.
            this.messageManager.sendPlayerMessage(Key.BID_FAIL_TOO_LOW, bid.getBidderUUID(), this);
        }
    }

    /**
     * Disposes of a failed bid attempt.
     *
     * @param attemptedBid the attempted bid
     * @param reason       message key to send to looser
     */
    private void failBid(AuctionBid attemptedBid, Key reason) {
        attemptedBid.cancelBid();
        if (this.sealed && (attemptedBid.getError() == null)) {
            this.messageManager.sendPlayerMessage(Key.BID_SUCCESS_SEALED, attemptedBid.getBidderUUID(), this);
        } else {
            this.messageManager.sendPlayerMessage(reason, attemptedBid.getBidderUUID(), this);
        }
    }

    /**
     * Assigns new bid and alerts those in the hosting AuctionScope.
     *
     * @param newBid the new bid
     * @param reason message key to broadcast
     */
    private void setNewBid(AuctionBid newBid, Key reason) {
        AuctionBid prevBid = this.currentBid;

        if (AuctionConfig.getBoolean(Key.EXPIRE_BUYNOW_AT_FIRST_BID, this.scope)) {
            this.buyNow = 0;
        }

        if (this.currentBid != null) {
            this.currentBid.cancelBid();
        }
        this.currentBid = newBid;
        if (this.sealed) {
            this.messageManager.sendPlayerMessage(Key.BID_SUCCESS_SEALED, newBid.getBidderUUID(), this);
        } else if (AuctionConfig.getBoolean(Key.BROADCAST_BID_UPDATES, this.scope)) {
            this.messageManager.broadcastAuctionMessage(reason, this);
        } else {
            this.messageManager.sendPlayerMessage(reason, newBid.getBidderUUID(), this);
            if (prevBid != null && newBid.getBidderName().equalsIgnoreCase(prevBid.getBidderName())) {
                this.messageManager.sendPlayerMessage(reason, prevBid.getBidderUUID(), this);
            }
        }
        ObsidianAuctions.get().getAuctionManager().addParticipant(newBid.getBidderUUID(), this.scope);
        if (this.currentBid.getBidAmount() >= this.buyNow) {
            this.buyNow = 0;
        }

        // see if antisnipe is enabled...
        if (!this.sealed && AuctionConfig.getBoolean(Key.ANTI_SNIPE, this.scope) && this.getRemainingTime() <= AuctionConfig.getInt(Key.ANTI_SNIPE_PREVENTION_SECONDS, this.scope)) {
            this.addToRemainingTime(AuctionConfig.getInt(Key.ANTI_SNIPE_PREVENTION_SECONDS, this.scope));
            this.messageManager.broadcastAuctionMessage(Key.ANTI_SNIPE_TIME_ADDED, this);
        }
    }

    /**
     * Checks the item in hand to see if it's valid and allowed.
     *
     * @return acceptability of held item for auctioning
     */
    private boolean parseHeldItem() {
        Player owner = this.plugin.getServer().getPlayer(this.ownerUUID);
        if (this.lot != null) {
            return true;
        }
        ItemStack heldItem = LegacyUtil.getItemInMainHand(owner);
        if (heldItem == null || heldItem.getAmount() == 0) {
            this.messageManager.sendPlayerMessage(Key.AUCTION_FAIL_HAND_IS_EMPTY, this.ownerUUID, this);
            return false;
        }
        this.lot = new AuctionLot(heldItem, this.ownerUUID, this.ownerName);

        ItemStack itemType = this.lot.getTypeStack();

        if (!AuctionConfig.getBoolean(Key.ALLOW_DAMAGED_ITEMS, scope) && itemType.getType().getMaxDurability() > 0 && LegacyUtil.getDurability(itemType) > 0) {
            this.messageManager.sendPlayerMessage(Key.AUCTION_FAIL_DAMAGED_ITEM, this.ownerUUID, this);
            this.lot = null;
            return false;
        }

        String displayName = Items.getDisplayName(itemType);
        if (displayName == null) {
            displayName = "";
        }
        //Implementing allowing auctioned mob-spawners
        //if display name is not empty do function if not fall through to next if
        if (!displayName.isEmpty()) {
            if (AuctionConfig.getBoolean(Key.NAME_BLACKLIST_ENABLED, this.scope)) {
                String lowerCaseDisplay = displayName.toLowerCase();
                for (String string : AuctionConfig.getStringList(Key.NAME_BLACKLIST, this.scope)) {
                    if (lowerCaseDisplay.contains(string)) {
                        this.messageManager.sendPlayerMessage(Key.AUCTION_FAIL_BLACKLIST_NAME, this.ownerUUID, this);
                        return false;
                    }
                }
            }
        }

        String typeStr = itemType.getType().toString();
        if ((typeStr.equals("MOB_SPAWNER") || typeStr.equals("SPAWNER")) && !AuctionConfig.getBoolean(Key.ALLOW_MOBSPAWNERS, scope)) {
            this.messageManager.sendPlayerMessage(Key.AUCTION_FAIL_SPAWNER, this.ownerUUID, this);
            this.lot = null;
            return false;
        }

        if (!displayName.isEmpty() && !AuctionConfig.getBoolean(Key.ALLOW_RENAMED_ITEMS, scope)) {
            this.messageManager.sendPlayerMessage(Key.AUCTION_FAIL_RENAMED_ITEM, this.ownerUUID, this);
            this.lot = null;
            return false;
        }

        // Check lore:
        String[] lore = Items.getLore(heldItem);
        List<String> bannedLore = AuctionConfig.getStringList(Key.BANNED_LORE, scope);
        if (lore != null && bannedLore != null) {
            for (String s : bannedLore) {
                for (String value : lore) {
                    if (value.toLowerCase().contains(s.toLowerCase())) {
                        this.messageManager.sendPlayerMessage(Key.AUCTION_FAIL_BANNED_LORE, this.ownerUUID, this);
                        this.lot = null;
                        return false;
                    }
                }
            }
        }

        return true;
    }

    /**
     * Parses arguments entered into chat.
     *
     * @return acceptability of entered arguments
     */
    private boolean parseArgs() {
        // (amount) (starting price) (increment) (time) (buynow)
        if (!parseArgAmount()) {
            return false;
        } else if (!parseArgStartingBid()) {
            return false;
        } else if (!parseArgIncrement()) {
            return false;
        } else if (!parseArgTime()) {
            return false;
        } else return parseArgBuyNow();
    }

    /**
     * Checks auction starter ability to start auction.
     *
     * @return acceptability of starter auctioning
     */
    private boolean isValidOwner() {
        if (this.ownerName == null) {
            this.messageManager.sendPlayerMessage(Key.AUCTION_FAIL_INVALID_OWNER, null, this);
            return false;
        }
        return true;
    }

    /**
     * Checks lot quantity range and availability in starter's inventory.
     *
     * @return acceptability of lot quantity
     */
    private boolean isValidAmount() {
        if (this.quantity <= 0) {
            this.messageManager.sendPlayerMessage(Key.AUCTION_FAIL_QUANTITY_TOO_LOW, this.ownerUUID, this);
            return false;
        }

        // TODO: Add config setting for max quantity.

        if (!Items.hasAmount(this.ownerName, this.quantity, this.lot.getTypeStack())) {
            this.messageManager.sendPlayerMessage(Key.AUCTION_FAIL_INSUFFICIENT_SUPPLY, this.ownerUUID, this);
            return false;
        }
        return true;
    }

    /**
     * Checks starting bid.
     *
     * @return if starting bid is ok
     */
    private boolean isValidStartingBid() {
        if (this.startingBid < 0) {
            this.messageManager.sendPlayerMessage(Key.AUCTION_FAIL_STARTING_BID_TOO_LOW, this.ownerUUID, this);
            return false;
        } else if (this.startingBid > AuctionConfig.getSafeMoneyFromDouble(Key.MAX_STARTING_BID, scope)) {
            this.messageManager.sendPlayerMessage(Key.AUCTION_FAIL_STARTING_BID_TOO_HIGH, this.ownerUUID, this);
            return false;
        }
        return true;
    }

    /**
     * Checks minimum bid increment.
     *
     * @return if minimum bid increment is okay
     */
    private boolean isValidIncrement() {
        if (getMinBidIncrement() < AuctionConfig.getSafeMoneyFromDouble(Key.MIN_BID_INCREMENT, this.scope)) {
            this.messageManager.sendPlayerMessage(Key.AUCTION_FAIL_INCREMENT_TOO_LOW, this.ownerUUID, this);
            return false;
        }
        if (getMinBidIncrement() > AuctionConfig.getSafeMoneyFromDouble(Key.MAX_BID_INCREMENT, scope)) {
            this.messageManager.sendPlayerMessage(Key.AUCTION_FAIL_INCREMENT_TOO_HIGH, this.ownerUUID, this);
            return false;
        }
        return true;
    }

    /**
     * Checks BuyNow amount.
     *
     * @return if BuyNow amount is okay
     */
    private boolean isValidBuyNow() {
        if (getBuyNow() < 0) {
            this.messageManager.sendPlayerMessage(Key.AUCTION_FAIL_BUYNOW_TOO_LOW, this.ownerUUID, this);
            return false;
        } else if (getBuyNow() > AuctionConfig.getSafeMoneyFromDouble(Key.MAX_BUYNOW, scope)) {
            this.messageManager.sendPlayerMessage(Key.AUCTION_FAIL_BUYNOW_TOO_HIGH, this.ownerUUID, this);
            return false;
        }
        return true;
    }

    /**
     * Checks auction time limit.
     *
     * @return if auction time limit is okiedokie
     */
    private boolean isValidTime() {
        if (this.time < AuctionConfig.getInt(Key.MIN_AUCTION_TIME, this.scope)) {
            this.messageManager.sendPlayerMessage(Key.AUCTION_FAIL_TIME_TOO_LOW, this.ownerUUID, this);
            return false;
        } else if (this.time > AuctionConfig.getInt(Key.MAX_AUCTION_TIME, this.scope)) {
            this.messageManager.sendPlayerMessage(Key.AUCTION_FAIL_TIME_TOO_HIGH, this.ownerUUID, this);
            return false;
        }
        return true;
    }

    /**
     * Parses quantity argument.
     *
     * @return if quantity argument parsed correctly
     */
    private boolean parseArgAmount() {
        if (this.quantity > 0) {
            return true;
        }

        ItemStack lotType = this.lot.getTypeStack();
        if (this.args.length > 0) {
            if (this.args[0].equalsIgnoreCase("this") || this.args[0].equalsIgnoreCase("hand")) {
                this.quantity = lotType.getAmount();
            } else if (this.args[0].equalsIgnoreCase("all")) {
                this.quantity = Items.getAmount(this.ownerName, lotType);
            } else if (args[0].matches("[0-9]{1,7}")) {
                this.quantity = Integer.parseInt(this.args[0]);
            } else {
                this.messageManager.sendPlayerMessage(Key.PARSE_ERROR_INVALID_QUANTITY, this.ownerUUID, this);
                return false;
            }
        } else {
            this.quantity = lotType.getAmount();
        }
        if (this.quantity < 0) {
            this.messageManager.sendPlayerMessage(Key.PARSE_ERROR_INVALID_QUANTITY, this.ownerUUID, this);
            return false;
        }
        return true;
    }

    /**
     * Parses starting bid argument.
     *
     * @return if argument parsed correctly
     */
    private boolean parseArgStartingBid() {
        if (this.startingBid > 0) {
            return true;
        }

        if (this.args.length > 1) {
            if (this.args[1].isEmpty()) {
                this.messageManager.sendPlayerMessage(Key.PARSE_ERROR_INVALID_STARTING_BID, this.ownerUUID, this);
                return false;
            } else if (!args[1].matches(ObsidianAuctions.decimalRegex)) {
                this.messageManager.sendPlayerMessage(Key.PARSE_ERROR_INVALID_STARTING_BID, this.ownerUUID, this);
                return false;
            }
            this.startingBid = Functions.getSafeMoney(Double.parseDouble(args[1]));
        } else {
            this.startingBid = AuctionConfig.getSafeMoneyFromDouble(Key.DEFAULT_STARTING_BID, this.scope);
        }
        if (this.startingBid < 0) {
            this.messageManager.sendPlayerMessage(Key.PARSE_ERROR_INVALID_STARTING_BID, this.ownerUUID, this);
            return false;
        }
        return true;
    }

    /**
     * Parses minimum bid increment.
     *
     * @return if minimum bid increment parsed correctly
     */
    private boolean parseArgIncrement() {
        if (this.minBidIncrement > 0) {
            return true;
        }

        if (this.args.length > 2) {
            if (!this.args[2].isEmpty() && args[2].matches(ObsidianAuctions.decimalRegex)) {
                this.minBidIncrement = Functions.getSafeMoney(Double.parseDouble(this.args[2]));
            } else {
                this.messageManager.sendPlayerMessage(Key.PARSE_ERROR_INVALID_BID_INCREMENT, this.ownerUUID, this);
                return false;
            }
        } else {
            this.minBidIncrement = AuctionConfig.getSafeMoneyFromDouble(Key.DEFAULT_BID_INCREMENT, this.scope);
        }
        if (this.minBidIncrement < 0) {
            this.messageManager.sendPlayerMessage(Key.PARSE_ERROR_INVALID_BID_INCREMENT, this.ownerUUID, this);
            return false;
        }
        return true;
    }

    /**
     * Parses time argument.
     *
     * @return if time argument parsed correctly
     */
    private boolean parseArgTime() {
        if (this.time > 0) {
            return true;
        }

        if (this.args.length > 3) {
            if (this.args[3].matches("[0-9]{1,7}")) {
                this.time = Integer.parseInt(this.args[3]);
            } else {
                this.messageManager.sendPlayerMessage(Key.PARSE_ERROR_INVALID_TIME, this.ownerUUID, this);
                return false;
            }
        } else {
            this.time = AuctionConfig.getInt(Key.DEFAULT_AUCTION_TIME, this.scope);
        }
        if (this.time < 0) {
            this.messageManager.sendPlayerMessage(Key.PARSE_ERROR_INVALID_TIME, this.ownerUUID, this);
            return false;
        }
        return true;
    }

    /**
     * Parses BuyNow argument.
     *
     * @return if BuyNow argument parsed correctly
     */
    private boolean parseArgBuyNow() {

        if (this.sealed || !AuctionConfig.getBoolean(Key.ALLOW_BUYNOW, this.scope)) {
            this.buyNow = 0;
            return true;
        }

        if (getBuyNow() > 0) {
            return true;
        }

        if (this.args.length > 4) {
            if (!this.args[4].isEmpty() && this.args[4].matches(ObsidianAuctions.decimalRegex)) {
                this.buyNow = Functions.getSafeMoney(Double.parseDouble(args[4]));
            } else {
                this.messageManager.sendPlayerMessage(Key.PARSE_ERROR_INVALID_BUYNOW, this.ownerUUID, this);
                return false;
            }
        } else {
            this.buyNow = 0;
        }
        if (getBuyNow() < 0) {
            this.messageManager.sendPlayerMessage(Key.PARSE_ERROR_INVALID_BUYNOW, this.ownerUUID, this);
            return false;
        }
        return true;
    }

    /**
     * Gets the specified minimum bid increment for this Auction instance.
     *
     * @return minimum bid increment
     */
    public long getMinBidIncrement() {
        return this.minBidIncrement;
    }

    /**
     * Gets a type stack of the items being auctioned.
     *
     * @return stack of example items
     */
    public ItemStack getLotType() {
        if (this.lot == null) {
            return null;
        }
        return this.lot.getTypeStack();
    }

    /**
     * Gets quantity of the auctioned lot.
     *
     * @return amount being auctioned
     */
    public int getLotQuantity() {
        if (this.lot == null) {
            return 0;
        }
        return this.lot.getQuantity();
    }

    /**
     * Gets the lowest amount first bid can be.
     *
     * @return lowest possible starting bid in floAuction's proprietary "safe money"
     */
    public long getStartingBid() {
        long effectiveStartingBid = this.startingBid;
        if (effectiveStartingBid == 0) {
            effectiveStartingBid = this.minBidIncrement;
        }
        return effectiveStartingBid;
    }

    /**
     * Gets the AuctionBid object for the current winning bid.
     *
     * @return AuctionBid object of leader
     */
    public AuctionBid getCurrentBid() {
        return this.currentBid;
    }

    /**
     * Gets the UUID of the player who started and therefore "owns" the auction.
     *
     * @return auction owner uuid
     */
    public UUID getOwnerUUID() {
        return this.ownerUUID;
    }

    /**
     * Gets the name of the player who started and therefore "owns" the auction.
     *
     * @return auction owner name
     */
    public String getOwnerName() {
        return this.ownerName;
    }

    /**
     * Gets the amount of time remaining in the auction.
     *
     * @return number of seconds remaining in auction
     */
    public int getRemainingTime() {
        return this.countdown;
    }

    /**
     * Gets the originally specified auction time limit.  This does not take into account time added by anti-snipe being triggered.
     *
     * @return original auction length in seconds
     */
    public int getTotalTime() {
        return this.time;
    }

    /**
     * Adds to the remaining auction countdown.
     *
     * @param secondsToAdd
     * @return
     */
    public int addToRemainingTime(int secondsToAdd) {
        this.countdown += secondsToAdd;
        return this.countdown;
    }

    /**
     * Gets the amount specified for BuyNow.
     *
     * @return BuyNow amount in floAuction's proprietary "safe money"
     */
    public long getBuyNow() {
        return this.buyNow;
    }

    public String getOwnerDisplayName() {
        Player ownerPlayer = this.plugin.getServer().getPlayer(this.ownerUUID);
        if (ownerPlayer != null) {
            return ownerPlayer.getDisplayName();
        } else {
            return this.ownerName;
        }
    }

    public ItemStack getGuiItem() {
        return this.guiItem;
    }

    public boolean isSealed() {
        return this.sealed;
    }

    public MessageManager getMessageManager() {
        return this.messageManager;
    }

    public List<AuctionBid> getSealedBids() {
        return this.sealedBids;
    }

    public double getExtractedPreTax() {
        return this.extractedPreTax;
    }

    public double getExtractedPostTax() {
        return this.extractedPostTax;
    }

    public void setExtractedPostTax(double taxes) {
        this.extractedPostTax = taxes;
    }
}
