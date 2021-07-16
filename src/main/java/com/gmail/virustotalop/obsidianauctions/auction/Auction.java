package com.gmail.virustotalop.obsidianauctions.auction;

import com.gmail.virustotalop.obsidianauctions.AuctionConfig;
import com.gmail.virustotalop.obsidianauctions.ObsidianAuctions;
import com.gmail.virustotalop.obsidianauctions.area.AreaManager;
import com.gmail.virustotalop.obsidianauctions.event.AuctionBidEvent;
import com.gmail.virustotalop.obsidianauctions.event.AuctionEndEvent;
import com.gmail.virustotalop.obsidianauctions.event.AuctionStartEvent;
import com.gmail.virustotalop.obsidianauctions.message.MessageManager;
import com.gmail.virustotalop.obsidianauctions.util.Functions;
import com.gmail.virustotalop.obsidianauctions.util.Items;
import com.gmail.virustotalop.obsidianauctions.util.LegacyUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
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

    protected ObsidianAuctions plugin;
    private final String[] args;
    private final UUID ownerUUID;
    private String ownerName;
    private final AuctionScope scope;

    public double extractedPreTax = 0;
    public double extractedPostTax = 0;

    private long startingBid = 0;
    private long minBidIncrement = 0;
    private long buyNow = 0;
    private int quantity = 0;
    private int time = 0;
    private boolean active = false;

    private AuctionLot lot;
    private AuctionBid currentBid = null;
    public ArrayList<AuctionBid> sealedBids = new ArrayList<>();

    public boolean sealed = false;

    public long nextTickTime = 0;

    // Scheduled timers:
    private int countdown = 0;
    private int countdownTimer = 0;

    //added
    private final ItemStack guiItem;

    public MessageManager messageManager = null;

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
     * @param sealed       whether or not it is a sealed auction
     */
    public Auction(ObsidianAuctions plugin, Player auctionOwner, String[] inputArgs, AuctionScope scope, boolean sealed, MessageManager messageManager, ItemStack guiItem) {
        this.ownerUUID = auctionOwner.getUniqueId();
        this.ownerName = auctionOwner.getName();
        this.args = Functions.mergeInputArgs(auctionOwner.getUniqueId(), inputArgs, false);
        this.plugin = plugin;
        this.scope = scope;
        this.sealed = sealed;
        this.messageManager = messageManager;
        this.guiItem = guiItem;

        //Override gui item item meta
        if(this.guiItem.hasItemMeta()) {
            if(this.guiItem.getItemMeta().hasLore()) {
                List<String> lore = this.guiItem.getItemMeta().getLore();
                lore.add(ChatColor.BLUE + "Auction by: " + this.getOwnerDisplayName());
                //lore.add(ChatColor.BLUE + "Starting price: " + this.getStartingBid());
                //lore.add(ChatColor.BLUE + "Buy it now price: " + this.buyNow);
                ItemMeta itemMeta = this.guiItem.getItemMeta();
                itemMeta.setLore(lore);
                this.guiItem.setItemMeta(itemMeta);
            } else {
                List<String> lore = new ArrayList<String>();
                lore.add(ChatColor.BLUE + "Auction by: " + this.getOwnerDisplayName());
                //lore.add(ChatColor.BLUE + "Starting price: " + this.getStartingBid());
                //lore.add(ChatColor.BLUE + "Buy it now price: " + this.buyNow);
                ItemMeta itemMeta = this.guiItem.getItemMeta();
                itemMeta.setLore(lore);
                this.guiItem.setItemMeta(itemMeta);
            }
        } else {
            List<String> lore = new ArrayList<String>();
            lore.add(ChatColor.BLUE + "Auction by: " + this.getOwnerDisplayName());
            //lore.add(ChatColor.BLUE + "Starting price: " + this.getStartingBid());
            //lore.add(ChatColor.BLUE + "Buy it now price: " + this.buyNow);
            ItemMeta itemMeta = this.guiItem.getItemMeta();
            itemMeta.setLore(lore);
            this.guiItem.setItemMeta(itemMeta);
        }

    }

    /**
     * Attempts to start this auction instance.  Returns success.
     *
     * @return whether or not the auction start succeeded
     */
    public boolean start() {
        Player owner = Bukkit.getServer().getPlayer(this.ownerUUID);

        if(AreaManager.isInArena(owner)) {
            this.messageManager.sendPlayerMessage("auction-fail-arena", this.ownerUUID, this);
            return false;
        }

        ItemStack typeStack = this.lot.getTypeStack();
        double preAuctionTax = AuctionConfig.getDouble("auction-start-tax", this.scope);

        // Check banned items:
        List<String> bannedItems = AuctionConfig.getStringList("banned-items", this.scope);
        for(int i = 0; i < bannedItems.size(); i++) {
            if(Items.isSameItem(typeStack, bannedItems.get(i))) {
                this.messageManager.sendPlayerMessage("auction-fail-banned", this.ownerUUID, this);
                return false;
            }
        }

        Map<String, String> taxedItems = AuctionConfig.getStringStringMap("taxed-items", this.scope);
        if(taxedItems != null) {
            for(Map.Entry<String, String> entry : taxedItems.entrySet()) {
                if(Items.isSameItem(typeStack, entry.getKey())) {
                    String itemTax = entry.getValue();

                    if(itemTax.endsWith("a")) {
                        try {
                            preAuctionTax = Double.valueOf(itemTax.substring(0, itemTax.length() - 1));
                        } catch(Exception e) {
                            // Clearly this isn't a valid number, just forget about it.
                            preAuctionTax = AuctionConfig.getDouble("auction-start-tax", this.scope);
                        }
                    } else if(!itemTax.endsWith("%")) {
                        try {
                            preAuctionTax = Double.valueOf(itemTax);
                            preAuctionTax *= this.quantity;
                        } catch(Exception e) {
                            // Clearly this isn't a valid number, just forget about it.
                            preAuctionTax = AuctionConfig.getDouble("auction-start-tax", this.scope);
                        }
                    }
                    break;
                }
            }
        }

        if(preAuctionTax > 0D) {
            if(!ObsidianAuctions.get().getEconomy().has(this.ownerName, preAuctionTax)) {
                this.messageManager.sendPlayerMessage("auction-fail-start-tax", this.ownerUUID, this);
                return false;
            }
        }

        if(!this.lot.addItems(this.quantity, true)) {
            this.messageManager.sendPlayerMessage("auction-fail-insufficient-supply", this.ownerUUID, this);
            return false;
        }

        if(preAuctionTax > 0D) {
            if(ObsidianAuctions.get().getEconomy().has(this.ownerName, preAuctionTax)) {
                ObsidianAuctions.get().getEconomy().withdrawPlayer(this.ownerName, preAuctionTax);
                this.extractedPreTax = preAuctionTax;
                this.messageManager.sendPlayerMessage("auction-start-tax", this.ownerUUID, this);
                String taxDestinationUser = AuctionConfig.getString("deposit-tax-to-user", scope);
                if(!taxDestinationUser.isEmpty()) {
                    ObsidianAuctions.get().getEconomy().depositPlayer(taxDestinationUser, preAuctionTax);
                }
            }
        }

        if(this.buyNow < getStartingBid()) {
            this.buyNow = 0;
        }

        // Check to see if any other plugins have a reason...or they can forever hold their piece.
        AuctionStartEvent auctionStartEvent = new AuctionStartEvent(owner, this);
        Bukkit.getServer().getPluginManager().callEvent(auctionStartEvent);

        if(auctionStartEvent.isCancelled()) {
            this.messageManager.sendPlayerMessage("auction-fail-blocked-by-other-plugin", this.ownerUUID, this);
        } else {
            this.active = true;
            this.messageManager.broadcastAuctionMessage("auction-start", this);

            // Set timer:
            final Auction thisAuction = this;
            this.countdown = this.time;

            this.countdownTimer = this.plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
                if(thisAuction.nextTickTime > System.currentTimeMillis()) {
                    return;
                }
                thisAuction.nextTickTime = thisAuction.nextTickTime + 1000;

                thisAuction.countdown--;
                if(thisAuction.countdown <= 0) {
                    thisAuction.end();
                    return;
                }
                if(!AuctionConfig.getBoolean("suppress-countdown", scope)) {
                    if(thisAuction.countdown < 4) {
                        messageManager.broadcastAuctionMessage("timer-countdown-notification", thisAuction);
                        return;
                    }
                    if(thisAuction.time >= 20) {
                        if(thisAuction.countdown == (thisAuction.time / 2)) {
                            messageManager.broadcastAuctionMessage("timer-countdown-notification", thisAuction);
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
        List<String> messageKeys = new ArrayList<String>();
        String playerName = null;
        UUID playerUUID = null;
        if(sender instanceof Player) {
            playerName = sender.getName();
            playerUUID = ((Player) sender).getUniqueId();
        }

        ItemStack itemType = this.getLotType();
        Map<Enchantment, Integer> enchantments = itemType.getEnchantments();
        if(enchantments == null || enchantments.size() == 0) {
            enchantments = Items.getStoredEnchantments(itemType);
        }
        if(!this.active) {
            if(sender instanceof Player) {
                this.messageManager.sendPlayerMessage("auction-info-no-auction", playerUUID, this);
            }
            return;
        } else {
            messageKeys.add("auction-info");
        }
        if(fullBroadcast) {
            this.messageManager.broadcastAuctionMessage(messageKeys, this);
        } else {
            this.messageManager.sendPlayerMessage(messageKeys, playerUUID, this);
        }
    }

    /**
     * Cancels the Auction instance and disposes of it normally.
     */
    public void cancel() {
        Bukkit.getServer().getPluginManager().callEvent(new AuctionEndEvent(this, true));
        this.messageManager.broadcastAuctionMessage("auction-cancel", this);

        if(this.lot != null) {
            this.lot.cancelLot();
        }
        if(this.currentBid != null) {
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
        Bukkit.getServer().getPluginManager().callEvent(new AuctionEndEvent(this, true));
        this.ownerName = authority.getName();
        this.messageManager.broadcastAuctionMessage("confiscate-success", this);
        if(this.lot != null) {
            this.lot.winLot(authority.getUniqueId(), authority.getName());
        }
        if(this.currentBid != null) {
            this.currentBid.cancelBid();
        }
        this.dispose();
    }

    /**
     * Ends an auction normally sending money and goods to their earned destinations.
     */
    public void end() {
        AuctionEndEvent auctionEndEvent = new AuctionEndEvent(this, false);
        Bukkit.getServer().getPluginManager().callEvent(auctionEndEvent);
        if(auctionEndEvent.isCancelled()) {
            this.messageManager.broadcastAuctionMessage("auction-cancel", this);
            if(this.lot != null) {
                this.lot.cancelLot();
            }
            if(this.currentBid != null) {
                this.currentBid.cancelBid();
            }
        } else {
            if(this.currentBid == null || this.lot == null) {
                this.messageManager.broadcastAuctionMessage("auction-end-nobids", this);
                if(this.lot != null) {
                    this.lot.cancelLot();
                }
                if(this.currentBid != null) {
                    this.currentBid.cancelBid();
                }
            } else {
                this.messageManager.broadcastAuctionMessage("auction-end", this);
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
        this.plugin.getServer().getScheduler().cancelTask(countdownTimer);
        this.sealed = false;
        for(int i = 0; i < this.sealedBids.size(); i++) {
            this.sealedBids.get(i).cancelBid();
        }
        this.scope.setActiveAuction(null);
    }

    /**
     * Checks all auction parameters and environment factors to determine if the Auction instance can legitimately start.
     *
     * @return whether the auction can begin
     */
    public boolean isValid() {
        if(!isValidOwner()) {
            return false;
        } else if(!parseHeldItem()) {
            return false;
        } else if(!parseArgs()) {
            return false;
        } else if(!isValidAmount()) {
            return false;
        } else if(!isValidStartingBid()) {
            return false;
        } else if(!isValidIncrement()) {
            return false;
        } else if(!isValidTime()) {
            return false;
        } else return isValidBuyNow();
    }

    /**
     * Parses a bid command.
     *
     * @param bidder    Player attempting to bid
     * @param inputArgs parameters entered in chat
     */
    public void bid(Player bidder, String[] inputArgs) {
        if(bidder == null) {
            return;
        }
        String playerName = bidder.getName();
        UUID playerUUID = bidder.getUniqueId();

        if(AreaManager.isInArena(bidder)) {
            this.messageManager.sendPlayerMessage("bid-fail-arena", playerUUID, this);
            return;
        }

        // BuyNow
        if(AuctionConfig.getBoolean("allow-buynow", scope) && inputArgs.length > 0) {
            if(inputArgs[0].equalsIgnoreCase("buy")) {

                if(this.buyNow == 0 || (this.currentBid != null && currentBid.getBidAmount() >= this.buyNow)) {
                    this.messageManager.sendPlayerMessage("bid-fail-buynow-expired", playerUUID, this);
                } else {
                    inputArgs[0] = Double.toString(Functions.getUnsafeMoney(this.buyNow));
                    if(inputArgs[0].endsWith(".0")) {
                        inputArgs[0] = inputArgs[0].substring(0, inputArgs[0].length() - 2);
                    }
                    AuctionBid bid = new AuctionBid(this, bidder, inputArgs);
                    if(bid.getError() != null) {
                        failBid(bid, bid.getError());
                        return;
                    } else {
                        // raisOwnBid does nothing if it's not the current bidder.
                        if(this.currentBid != null) {
                            bid.raiseOwnBid(this.currentBid);
                        }

                        // Let other plugins figure out any reasons why this buy shouldn't happen.
                        AuctionBidEvent auctionBidEvent = new AuctionBidEvent(bidder, this, Functions.getUnsafeMoney(bid.getBidAmount()), Functions.getUnsafeMoney(bid.getMaxBidAmount()), true);
                        Bukkit.getServer().getPluginManager().callEvent(auctionBidEvent);
                        if(auctionBidEvent.isCancelled()) {
                            this.failBid(bid, "bid-fail-blocked-by-other-plugin");
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
        if(bid.getError() != null) {
            this.failBid(bid, bid.getError());
            return;
        }

        if(this.currentBid == null) {
            if(bid.getBidAmount() < getStartingBid()) {
                this.failBid(bid, "bid-fail-under-starting-bid");
                return;
            }
            // Let other plugins figure out any reasons why this buy shouldn't happen.
            AuctionBidEvent auctionBidEvent = new AuctionBidEvent(bidder, this, Functions.getUnsafeMoney(bid.getBidAmount()), Functions.getUnsafeMoney(bid.getMaxBidAmount()), true);
            Bukkit.getServer().getPluginManager().callEvent(auctionBidEvent);
            if(auctionBidEvent.isCancelled()) {
                failBid(bid, "bid-fail-blocked-by-other-plugin");
            } else {
                setNewBid(bid, "bid-success-no-challenger");
            }
            return;
        }

        long previousBidAmount = this.currentBid.getBidAmount();
        long previousMaxBidAmount = this.currentBid.getMaxBidAmount();
        if(this.currentBid.getBidderName().equals(bidder.getName())) {
            if(bid.raiseOwnBid(this.currentBid)) {
                // Let other plugins figure out any reasons why this buy shouldn't happen.
                AuctionBidEvent auctionBidEvent = new AuctionBidEvent(bidder, this, Functions.getUnsafeMoney(bid.getBidAmount()), Functions.getUnsafeMoney(bid.getMaxBidAmount()), true);
                Bukkit.getServer().getPluginManager().callEvent(auctionBidEvent);
                if(auctionBidEvent.isCancelled()) {
                    this.failBid(bid, "bid-fail-blocked-by-other-plugin");
                } else {
                    this.setNewBid(bid, "bid-success-update-own-bid");
                }
            } else {
                if(previousMaxBidAmount < currentBid.getMaxBidAmount()) {
                    this.failBid(bid, "bid-success-update-own-maxbid");
                } else {
                    this.failBid(bid, "bid-fail-already-current-bidder");
                }
            }
            return;
        }

        AuctionBid winner = null;
        AuctionBid loser = null;

        if(AuctionConfig.getBoolean("use-old-bid-logic", this.scope)) {
            if(bid.getMaxBidAmount() > this.currentBid.getMaxBidAmount()) {
                winner = bid;
                loser = this.currentBid;
            } else {
                winner = this.currentBid;
                loser = bid;
            }
            winner.raiseBid(Math.max(winner.getBidAmount(), Math.min(winner.getMaxBidAmount(), loser.getBidAmount() + this.minBidIncrement)));
        } else {
            // If you follow what this does, congratulations.
            long baseBid = 0;
            if(bid.getBidAmount() >= this.currentBid.getBidAmount() + this.minBidIncrement) {
                baseBid = bid.getBidAmount();
            } else {
                baseBid = this.currentBid.getBidAmount() + this.minBidIncrement;
            }

            Integer prevSteps = (int) Math.floor((double) (this.currentBid.getMaxBidAmount() - baseBid + this.minBidIncrement) / this.minBidIncrement / 2);
            Integer newSteps = (int) Math.floor((double) (bid.getMaxBidAmount() - baseBid) / this.minBidIncrement / 2);

            if(newSteps >= prevSteps) {
                winner = bid;
                winner.raiseBid(baseBid + (Math.max(0, prevSteps) * this.minBidIncrement * 2));
                loser = this.currentBid;
            } else {
                winner = this.currentBid;
                winner.raiseBid(baseBid + (Math.max(0, newSteps + 1) * this.minBidIncrement * 2) - this.minBidIncrement);
                loser = bid;
            }
        }

        if(previousBidAmount <= winner.getBidAmount()) {
            // Did the new bid win?
            if(winner.equals(bid)) {
                // Let other plugins figure out any reasons why this buy shouldn't happen.
                AuctionBidEvent auctionBidEvent = new AuctionBidEvent(bidder, this, Functions.getUnsafeMoney(bid.getBidAmount()), Functions.getUnsafeMoney(bid.getMaxBidAmount()), true);
                Bukkit.getServer().getPluginManager().callEvent(auctionBidEvent);
                if(auctionBidEvent.isCancelled()) {
                    failBid(bid, "bid-fail-blocked-by-other-plugin");
                } else {
                    setNewBid(bid, "bid-success-outbid");
                }
            } else {
                // Did the old bid have to raise the bid to stay winner?
                if(previousBidAmount < winner.getBidAmount()) {
                    if(!this.sealed && !AuctionConfig.getBoolean("broadcast-bid-updates", scope)) {
                        this.messageManager.broadcastAuctionMessage("bid-auto-outbid", this);
                    }
                    failBid(bid, "bid-fail-auto-outbid");
                } else {
                    if(!this.sealed) {
                        this.messageManager.sendPlayerMessage("bid-fail-too-low", bid.getBidderUUID(), this);
                    }
                    this.failBid(bid, null);
                }
            }
        } else {
            // Seriously don't know what could cause this, but might as well take care of it.
            this.messageManager.sendPlayerMessage("bid-fail-too-low", bid.getBidderUUID(), this);
        }
    }

    /**
     * Disposes of a failed bid attempt.
     *
     * @param attemptedBid the attempted bid
     * @param reason       message key to send to looser
     */
    private void failBid(AuctionBid attemptedBid, String reason) {
        attemptedBid.cancelBid();
        if(this.sealed && (attemptedBid.getError() == null || attemptedBid.getError().isEmpty())) {
            this.messageManager.sendPlayerMessage("bid-success-sealed", attemptedBid.getBidderUUID(), this);
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
    private void setNewBid(AuctionBid newBid, String reason) {
        AuctionBid prevBid = this.currentBid;

        if(AuctionConfig.getBoolean("expire-buynow-at-first-bid", this.scope)) {
            this.buyNow = 0;
        }

        if(this.currentBid != null) {
            this.currentBid.cancelBid();
        }
        this.currentBid = newBid;
        if(this.sealed) {
            this.messageManager.sendPlayerMessage("bid-success-sealed", newBid.getBidderUUID(), this);
        } else if(AuctionConfig.getBoolean("broadcast-bid-updates", this.scope)) {
            this.messageManager.broadcastAuctionMessage(reason, this);
        } else {
            this.messageManager.sendPlayerMessage(reason, newBid.getBidderUUID(), this);
            if(prevBid != null && newBid.getBidderName().equalsIgnoreCase(prevBid.getBidderName())) {
                this.messageManager.sendPlayerMessage(reason, prevBid.getBidderUUID(), this);
            }
        }
        AuctionParticipant.addParticipant(newBid.getBidderUUID(), this.scope);
        if(this.currentBid.getBidAmount() >= this.buyNow) {
            this.buyNow = 0;
        }

        // see if antisnipe is enabled...
        if(!this.sealed && AuctionConfig.getBoolean("anti-snipe", this.scope) == true && this.getRemainingTime() <= AuctionConfig.getInt("anti-snipe-prevention-seconds", this.scope)) {
            this.addToRemainingTime(AuctionConfig.getInt("anti-snipe-prevention-seconds", this.scope));
            this.messageManager.broadcastAuctionMessage("anti-snipe-time-added", this);
        }
    }

    /**
     * Checks the item in hand to see if it's valid and allowed.
     *
     * @return acceptability of held item for auctioning
     */
    private boolean parseHeldItem() {
        Player owner = Bukkit.getServer().getPlayer(this.ownerUUID);
        if(this.lot != null) {
            return true;
        }
        ItemStack heldItem = LegacyUtil.getItemInMainHand(owner);
        if(heldItem == null || heldItem.getAmount() == 0) {
            this.messageManager.sendPlayerMessage("auction-fail-hand-is-empty", this.ownerUUID, this);
            return false;
        }
        this.lot = new AuctionLot(heldItem, this.ownerUUID, this.ownerName);

        ItemStack itemType = this.lot.getTypeStack();

        if(!AuctionConfig.getBoolean("allow-damaged-items", scope) && itemType.getType().getMaxDurability() > 0 && itemType.getDurability() > 0) {
            this.messageManager.sendPlayerMessage("auction-fail-damaged-item", this.ownerUUID, this);
            this.lot = null;
            return false;
        }

        String displayName = Items.getDisplayName(itemType);
        if(displayName == null) {
            displayName = "";
        }
        //Implementing allowing auctioned mob-spawners
        //if display name is not empty do function if not fall through to next if
        if(!displayName.isEmpty()) {
            if(ObsidianAuctions.itemNameBlackListEnabled) {
                String lowerCaseDisplay = displayName.toLowerCase();
                for(String string : ObsidianAuctions.itemBlacklist) {
                    if(lowerCaseDisplay.contains(string)) {
                        this.messageManager.sendPlayerMessage("auction-fail-blacklist-name", this.ownerUUID, this);
                        return false;
                    }
                }
            }
        }

        String typeStr = itemType.getType().toString();
        if((typeStr.equals("MOB_SPAWNER") || typeStr.equals("SPAWNER")) && !AuctionConfig.getBoolean("allow-mobspawners", scope)) {
            this.messageManager.sendPlayerMessage("auction-fail-spawner", this.ownerUUID, this);
            this.lot = null;
            return false;
        }

        if(!displayName.isEmpty() && !AuctionConfig.getBoolean("allow-renamed-items", scope)) {
            this.messageManager.sendPlayerMessage("auction-fail-renamed-item", this.ownerUUID, this);
            this.lot = null;
            return false;
        }

        // Check lore:
        String[] lore = Items.getLore(heldItem);
        List<String> bannedLore = AuctionConfig.getStringList("banned-lore", scope);
        if(lore != null && bannedLore != null) {
            for(int i = 0; i < bannedLore.size(); i++) {
                for(int j = 0; j < lore.length; j++) {
                    if(lore[j].toLowerCase().contains(bannedLore.get(i).toLowerCase())) {
                        this.messageManager.sendPlayerMessage("auction-fail-banned-lore", this.ownerUUID, this);
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
        if(!parseArgAmount()) {
            return false;
        } else if(!parseArgStartingBid()) {
            return false;
        } else if(!parseArgIncrement()) {
            return false;
        } else if(!parseArgTime()) {
            return false;
        } else return parseArgBuyNow();
    }

    /**
     * Checks auction starter ability to start auction.
     *
     * @return acceptability of starter auctioning
     */
    private boolean isValidOwner() {
        if(this.ownerName == null) {
            this.messageManager.sendPlayerMessage("auction-fail-invalid-owner", null, this);
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
        if(this.quantity <= 0) {
            this.messageManager.sendPlayerMessage("auction-fail-quantity-too-low", this.ownerUUID, this);
            return false;
        }

        // TODO: Add config setting for max quantity.

        if(!Items.hasAmount(this.ownerName, this.quantity, this.lot.getTypeStack())) {
            this.messageManager.sendPlayerMessage("auction-fail-insufficient-supply", this.ownerUUID, this);
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
        if(this.startingBid < 0) {
            this.messageManager.sendPlayerMessage("auction-fail-starting-bid-too-low", this.ownerUUID, this);
            return false;
        } else if(this.startingBid > AuctionConfig.getSafeMoneyFromDouble("max-starting-bid", scope)) {
            this.messageManager.sendPlayerMessage("auction-fail-starting-bid-too-high", this.ownerUUID, this);
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
        if(getMinBidIncrement() < AuctionConfig.getSafeMoneyFromDouble("min-bid-increment", this.scope)) {
            this.messageManager.sendPlayerMessage("auction-fail-increment-too-low", this.ownerUUID, this);
            return false;
        }
        if(getMinBidIncrement() > AuctionConfig.getSafeMoneyFromDouble("max-bid-increment", scope)) {
            this.messageManager.sendPlayerMessage("auction-fail-increment-too-high", this.ownerUUID, this);
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
        if(getBuyNow() < 0) {
            this.messageManager.sendPlayerMessage("auction-fail-buynow-too-low", this.ownerUUID, this);
            return false;
        } else if(getBuyNow() > AuctionConfig.getSafeMoneyFromDouble("max-buynow", scope)) {
            this.messageManager.sendPlayerMessage("auction-fail-buynow-too-high", this.ownerUUID, this);
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
        if(this.time < AuctionConfig.getInt("min-auction-time", this.scope)) {
            this.messageManager.sendPlayerMessage("auction-fail-time-too-low", this.ownerUUID, this);
            return false;
        } else if(this.time > AuctionConfig.getInt("max-auction-time", this.scope)) {
            this.messageManager.sendPlayerMessage("auction-fail-time-too-high", this.ownerUUID, this);
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
        if(this.quantity > 0) {
            return true;
        }

        ItemStack lotType = this.lot.getTypeStack();
        if(this.args.length > 0) {
            if(this.args[0].equalsIgnoreCase("this") || this.args[0].equalsIgnoreCase("hand")) {
                this.quantity = lotType.getAmount();
            } else if(this.args[0].equalsIgnoreCase("all")) {
                this.quantity = Items.getAmount(this.ownerName, lotType);
            } else if(args[0].matches("[0-9]{1,7}")) {
                this.quantity = Integer.parseInt(this.args[0]);
            } else {
                this.messageManager.sendPlayerMessage("parse-error-invalid-quantity", this.ownerUUID, this);
                return false;
            }
        } else {
            this.quantity = lotType.getAmount();
        }
        if(this.quantity < 0) {
            this.messageManager.sendPlayerMessage("parse-error-invalid-quantity", this.ownerUUID, this);
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
        if(this.startingBid > 0) {
            return true;
        }

        if(this.args.length > 1) {
            if(this.args[1].isEmpty()) {
                this.messageManager.sendPlayerMessage("parse-error-invalid-starting-bid", this.ownerUUID, this);
                return false;
            } else if(!args[1].matches(ObsidianAuctions.decimalRegex)) {
                this.messageManager.sendPlayerMessage("parse-error-invalid-starting-bid", this.ownerUUID, this);
                return false;
            }
            this.startingBid = Functions.getSafeMoney(Double.parseDouble(args[1]));
        } else {
            this.startingBid = AuctionConfig.getSafeMoneyFromDouble("default-starting-bid", this.scope);
        }
        if(this.startingBid < 0) {
            this.messageManager.sendPlayerMessage("parse-error-invalid-starting-bid", this.ownerUUID, this);
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
        if(this.minBidIncrement > 0) {
            return true;
        }

        if(this.args.length > 2) {
            if(!this.args[2].isEmpty() && args[2].matches(ObsidianAuctions.decimalRegex)) {
                this.minBidIncrement = Functions.getSafeMoney(Double.parseDouble(this.args[2]));
            } else {
                this.messageManager.sendPlayerMessage("parse-error-invalid-bid-increment", this.ownerUUID, this);
                return false;
            }
        } else {
            this.minBidIncrement = AuctionConfig.getSafeMoneyFromDouble("default-bid-increment", this.scope);
        }
        if(this.minBidIncrement < 0) {
            this.messageManager.sendPlayerMessage("parse-error-invalid-bid-increment", this.ownerUUID, this);
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
        if(this.time > 0) {
            return true;
        }

        if(this.args.length > 3) {
            if(this.args[3].matches("[0-9]{1,7}")) {
                this.time = Integer.parseInt(this.args[3]);
            } else {
                this.messageManager.sendPlayerMessage("parse-error-invalid-time", this.ownerUUID, this);
                return false;
            }
        } else {
            this.time = AuctionConfig.getInt("default-auction-time", this.scope);
        }
        if(this.time < 0) {
            this.messageManager.sendPlayerMessage("parse-error-invalid-time", this.ownerUUID, this);
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

        if(this.sealed || !AuctionConfig.getBoolean("allow-buynow", this.scope)) {
            this.buyNow = 0;
            return true;
        }

        if(getBuyNow() > 0) {
            return true;
        }

        if(this.args.length > 4) {
            if(!this.args[4].isEmpty() && this.args[4].matches(ObsidianAuctions.decimalRegex)) {
                this.buyNow = Functions.getSafeMoney(Double.parseDouble(args[4]));
            } else {
                this.messageManager.sendPlayerMessage("parse-error-invalid-buynow", this.ownerUUID, this);
                return false;
            }
        } else {
            this.buyNow = 0;
        }
        if(getBuyNow() < 0) {
            this.messageManager.sendPlayerMessage("parse-error-invalid-buynow", this.ownerUUID, this);
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
        if(this.lot == null) {
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
        if(this.lot == null) {
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
        if(effectiveStartingBid == 0) {
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
        Player ownerPlayer = Bukkit.getServer().getPlayer(this.ownerUUID);
        if(ownerPlayer != null) {
            return ownerPlayer.getDisplayName();
        } else {
            return this.ownerName;
        }
    }

    public ItemStack getGuiItem() {
        return this.guiItem;
    }

}
