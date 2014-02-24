package com.flobi.floAuction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.flobi.floAuction.events.AuctionBidEvent;
import com.flobi.floAuction.events.AuctionEndEvent;
import com.flobi.floAuction.events.AuctionStartEvent;
import com.flobi.floAuction.utility.functions;
import com.flobi.floAuction.utility.items;
import com.google.common.collect.Lists;

/**
 * Main auction class.
 * 
 * @author Joshua "flobi" Hatfield
 */
public class Auction {
	protected floAuction plugin;
	private String[] args;
	private String ownerName;
	private AuctionScope scope;
	
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
	public ArrayList<AuctionBid> sealedBids = new ArrayList<AuctionBid>(); 
	
	public boolean sealed = false;
	
	public long nextTickTime = 0;
	
	// Scheduled timers:
	private int countdown = 0;
	private int countdownTimer = 0;
	
	public MessageManager messageManager = null;
	
	/**
	 * Gets the AuctionScope which hosts this auction.
	 * 
	 * @return the hosting AuctionScope
	 */
	public AuctionScope getScope() {
		return scope;
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
	public Auction(floAuction plugin, Player auctionOwner, String[] inputArgs, AuctionScope scope, boolean sealed, MessageManager messageManager) {
		ownerName = auctionOwner.getName();
		args = functions.mergeInputArgs(auctionOwner.getName(), inputArgs, false);
		this.plugin = plugin; 
		this.scope = scope;
		this.sealed = sealed;
		this.messageManager = messageManager;
	}
	
	/**
	 * Attempts to start this auction instance.  Returns success.
	 * 
	 * @return whether or not the auction start succeeded
	 */
	public Boolean start() {
		Player owner = Bukkit.getPlayer(ownerName);
		
		if (ArenaManager.isInArena(owner)) {
			messageManager.sendPlayerMessage(Lists.newArrayList("auction-fail-arena"), ownerName, this);
			return false;
		}
		
		ItemStack typeStack = lot.getTypeStack();
		double preAuctionTax = AuctionConfig.getDouble("auction-start-tax", scope);
		
		// Check banned items:
		List<String> bannedItems = AuctionConfig.getStringList("banned-items", scope);
		for (int i = 0; i < bannedItems.size(); i++) {
			if (items.isSameItem(typeStack, bannedItems.get(i))) {
				messageManager.sendPlayerMessage(Lists.newArrayList("auction-fail-banned"), ownerName, this);
				return false;
			}
		}
		
		Map<String, String> taxedItems = AuctionConfig.getStringStringMap("taxed-items", scope);
		if (taxedItems != null) {
			for (Map.Entry<String, String> entry : taxedItems.entrySet()) {
				if (items.isSameItem(typeStack, entry.getKey())) {
					String itemTax = entry.getValue();
					
					if (itemTax.endsWith("a")) {
						try {
							preAuctionTax = Double.valueOf(itemTax.substring(0, itemTax.length() - 1));
						} catch (Exception e) {
							// Clearly this isn't a valid number, just forget about it.
							preAuctionTax = AuctionConfig.getDouble("auction-start-tax", scope);
						}
					} else if (!itemTax.endsWith("%")) {
						try {
							preAuctionTax = Double.valueOf(itemTax);
							preAuctionTax *= quantity;
						} catch (Exception e) {
							// Clearly this isn't a valid number, just forget about it.
							preAuctionTax = AuctionConfig.getDouble("auction-start-tax", scope);
						}
					}
					break;
				}
			}		
		}
		
		if (preAuctionTax > 0D) {
			if (!floAuction.econ.has(ownerName, preAuctionTax)) {
				messageManager.sendPlayerMessage(Lists.newArrayList("auction-fail-start-tax"), ownerName, this);
				return false;
			}
		}
		
		if (!lot.addItems(quantity, true)) {
			messageManager.sendPlayerMessage(Lists.newArrayList("auction-fail-insufficient-supply"), ownerName, this);
			return false;
		}

		if (preAuctionTax > 0D) {
			if (floAuction.econ.has(ownerName, preAuctionTax)) {
				floAuction.econ.withdrawPlayer(ownerName, preAuctionTax);
				extractedPreTax = preAuctionTax;
				messageManager.sendPlayerMessage(Lists.newArrayList("auction-start-tax"), ownerName, this);
				String taxDestinationUser = AuctionConfig.getString("deposit-tax-to-user", scope);
				if (!taxDestinationUser.isEmpty()) floAuction.econ.depositPlayer(taxDestinationUser, preAuctionTax);
			}
		}
		
		if (buyNow < getStartingBid()) {
			buyNow = 0;
		}

		// Check to see if any other plugins have a reason...or they can forever hold their piece.
		AuctionStartEvent auctionStartEvent = new AuctionStartEvent(owner, this);
		Bukkit.getServer().getPluginManager().callEvent(auctionStartEvent);
		
		if (auctionStartEvent.isCancelled()) {
			messageManager.sendPlayerMessage(Lists.newArrayList("auction-fail-blocked-by-other-plugin"), ownerName, this);
		} else {
			active = true;
			messageManager.broadcastAuctionMessage(Lists.newArrayList("auction-start"), this);
			
			// Set timer:
			final Auction thisAuction = this;
			countdown = time;
			
			countdownTimer = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
			    public void run() {
			    	if (thisAuction.nextTickTime > System.currentTimeMillis()) return;
			    	thisAuction.nextTickTime = thisAuction.nextTickTime + 1000;
			    	
			    	thisAuction.countdown--;
			    	if (thisAuction.countdown <= 0) {
			    		thisAuction.end();
			    		return;
			    	}
			    	if (!AuctionConfig.getBoolean("suppress-countdown", scope)){
				    	if (thisAuction.countdown < 4) {
				    		messageManager.broadcastAuctionMessage(Lists.newArrayList("timer-countdown-notification"), thisAuction);
					    	return;
				    	}
				    	if (thisAuction.time >= 20) {
				    		if (thisAuction.countdown == (int) (thisAuction.time / 2)) {
				    			messageManager.broadcastAuctionMessage(Lists.newArrayList("timer-countdown-notification"), thisAuction);
				    		}
				    	}
			    	}
			    }
			}, 1L, 1L);
			nextTickTime = System.currentTimeMillis() + 1000;
	
			info(null, true);
		}

		return active;
	}
	
	/**
	 * Sends auction info to chat.
	 * 
	 * @param sender the CommandSender initiating the request
	 * @param fullBroadcast whether to send the message to everyone in the hosting AuctionScope
	 */
	public void info(CommandSender sender, boolean fullBroadcast) {
		List<String> messageKeys = new ArrayList<String>();
		String playerName = null;
		if (sender instanceof Player) {
			playerName = ((Player)sender).getName();
		}
		
		ItemStack itemType = this.getLotType();
		Map<Enchantment, Integer> enchantments = itemType.getEnchantments();
		if (enchantments == null || enchantments.size() == 0) enchantments = items.getStoredEnchantments(itemType);
		if (!active) {
			if (sender instanceof Player) {
				messageManager.sendPlayerMessage(Lists.newArrayList("auction-info-no-auction"), playerName, this);
			}
			return;
		} else {
			messageKeys.add("auction-info");
		}
		if (fullBroadcast) {
			messageManager.broadcastAuctionMessage(messageKeys, this);
		} else {
			messageManager.sendPlayerMessage(messageKeys, playerName, this);
		}
	}
	
	/**
	 * Cancels the Auction instance and disposes of it normally.
	 */
	public void cancel() {
		Bukkit.getServer().getPluginManager().callEvent(new AuctionEndEvent(this, true));
		messageManager.broadcastAuctionMessage(Lists.newArrayList("auction-cancel"), this);
		if (lot != null) lot.cancelLot();
		if (currentBid != null) currentBid.cancelBid();
		dispose();
	}
	
	/**
	 * Cancels the Auction instance redirecting all goods to an approved authority. 
	 * If the authority is not approved, Auction instance will not be cancelled.
	 * 
	 * @param authority the name of a player authorized to confiscate auctions
	 */
	public void confiscate(Player authority) {
		Bukkit.getServer().getPluginManager().callEvent(new AuctionEndEvent(this, true));
		ownerName = authority.getName();
		messageManager.broadcastAuctionMessage(Lists.newArrayList("confiscate-success"), this);
		if (lot != null) {
			lot.winLot(authority.getName());
		}
		if (currentBid != null) currentBid.cancelBid();
		dispose();
	}
	
	/**
	 * Ends an auction normally sending money and goods to their earned destinations.
	 */
	public void end() {
		AuctionEndEvent auctionEndEvent = new AuctionEndEvent(this, false);
		Bukkit.getServer().getPluginManager().callEvent(auctionEndEvent);
		if (auctionEndEvent.isCancelled()) {
			messageManager.broadcastAuctionMessage(Lists.newArrayList("auction-cancel"), this);
			if (lot != null) lot.cancelLot();
			if (currentBid != null) currentBid.cancelBid();
		} else {
			if (currentBid == null || lot == null) {
				messageManager.broadcastAuctionMessage(Lists.newArrayList("auction-end-nobids"), this);
				if (lot != null) lot.cancelLot();
				if (currentBid != null) currentBid.cancelBid();
			} else {
				messageManager.broadcastAuctionMessage(Lists.newArrayList("auction-end"), this);
				lot.winLot(currentBid.getBidder());
				currentBid.winBid();
			}
		}
		dispose();
	}
	
	/**
	 * Disposes of the remains of a terminated auction, purging the timer, refunding sealed bid losers and removing self from host scope.
	 */
	private void dispose() {
		plugin.getServer().getScheduler().cancelTask(countdownTimer);

		sealed = false;
		for(int i = 0; i < sealedBids.size(); i++) {
			sealedBids.get(i).cancelBid();
		}
		
		scope.setActiveAuction(null);
	}
	
	/**
	 * Checks all auction parameters and environment factors to determine if the Auction instance can legitimately start. 
	 * 
	 * @return whether the auction can begin
	 */
	public Boolean isValid() {
		if (!isValidOwner()) return false;
		if (!parseHeldItem()) return false;
		if (!parseArgs()) return false;
		if (!isValidAmount()) return false;
		if (!isValidStartingBid()) return false;
		if (!isValidIncrement()) return false;
		if (!isValidTime()) return false;
		if (!isValidBuyNow()) return false;
		return true;
	}
	
	/**
	 * Parses a bid command.
	 * 
	 * @param bidder Player attempting to bid
	 * @param inputArgs parameters entered in chat
	 */
	public void Bid(Player bidder, String[] inputArgs) {

		if (bidder == null) return;
		String playerName = bidder.getName();
		
		if (ArenaManager.isInArena(bidder)) {
			messageManager.sendPlayerMessage(Lists.newArrayList("bid-fail-arena"), playerName, this);
			return;
		}
		
		// BuyNow
		if (AuctionConfig.getBoolean("allow-buynow", scope) && inputArgs.length > 0) {
			if (inputArgs[0].equalsIgnoreCase("buy")) {

				if (buyNow == 0 || (currentBid != null && currentBid.getBidAmount() >= buyNow)) {
					messageManager.sendPlayerMessage(Lists.newArrayList("bid-fail-buynow-expired"), playerName, this);
				} else {
					inputArgs[0] = Double.toString(functions.getUnsafeMoney(buyNow));
					if (inputArgs[0].endsWith(".0")) {
						inputArgs[0] = inputArgs[0].substring(0, inputArgs[0].length() - 2);
					}
					AuctionBid bid = new AuctionBid(this, bidder, inputArgs);
					if (bid.getError() != null) {
						failBid(bid, bid.getError());
						return;
					} else {
						// raisOwnBid does nothing if it's not the current bidder.
						if (currentBid != null) bid.raiseOwnBid(currentBid);
						
						// Let other plugins figure out any reasons why this buy shouldn't happen.
						AuctionBidEvent auctionBidEvent = new AuctionBidEvent(bidder, this, functions.getUnsafeMoney(bid.getBidAmount()), functions.getUnsafeMoney(bid.getMaxBidAmount()), true);
						Bukkit.getServer().getPluginManager().callEvent(auctionBidEvent);
						if (auctionBidEvent.isCancelled()) {
							failBid(bid, "bid-fail-blocked-by-other-plugin");
						} else {
							setNewBid(bid, null);
							end();
						}
					}
				}
				return;
			}
		}
		
		// Normal bid
		AuctionBid bid = new AuctionBid(this, bidder, inputArgs);
		if (bid.getError() != null) {
			failBid(bid, bid.getError());
			return;
		}
		
		if (currentBid == null) {
			if (bid.getBidAmount() < getStartingBid()) {
				failBid(bid, "bid-fail-under-starting-bid");
				return;
			}
			// Let other plugins figure out any reasons why this buy shouldn't happen.
			AuctionBidEvent auctionBidEvent = new AuctionBidEvent(bidder, this, functions.getUnsafeMoney(bid.getBidAmount()), functions.getUnsafeMoney(bid.getMaxBidAmount()), true);
			Bukkit.getServer().getPluginManager().callEvent(auctionBidEvent);
			if (auctionBidEvent.isCancelled()) {
				failBid(bid, "bid-fail-blocked-by-other-plugin");
			} else {
				setNewBid(bid, "bid-success-no-challenger");
			}
			return;
		}
		long previousBidAmount = currentBid.getBidAmount();
		long previousMaxBidAmount = currentBid.getMaxBidAmount();
		if (currentBid.getBidder().equals(bidder.getName())) {
			if (bid.raiseOwnBid(currentBid)) {
				// Let other plugins figure out any reasons why this buy shouldn't happen.
				AuctionBidEvent auctionBidEvent = new AuctionBidEvent(bidder, this, functions.getUnsafeMoney(bid.getBidAmount()), functions.getUnsafeMoney(bid.getMaxBidAmount()), true);
				Bukkit.getServer().getPluginManager().callEvent(auctionBidEvent);
				if (auctionBidEvent.isCancelled()) {
					failBid(bid, "bid-fail-blocked-by-other-plugin");
				} else {
					setNewBid(bid, "bid-success-update-own-bid");
				}
			} else {
				if (previousMaxBidAmount < currentBid.getMaxBidAmount()) {
					failBid(bid, "bid-success-update-own-maxbid");
				} else {
					failBid(bid, "bid-fail-already-current-bidder");
				}
			}
			return;
		}
		AuctionBid winner = null;
		AuctionBid loser = null;
		
		if (AuctionConfig.getBoolean("use-old-bid-logic", scope)) {
			if (bid.getMaxBidAmount() > currentBid.getMaxBidAmount()) {
				winner = bid;
				loser = currentBid;
			} else {
				winner = currentBid;
				loser = bid;
			}
			winner.raiseBid(Math.max(winner.getBidAmount(), Math.min(winner.getMaxBidAmount(), loser.getBidAmount() + minBidIncrement)));
		} else {
			// If you follow what this does, congratulations.  
			long baseBid = 0;
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
				loser = currentBid;
			} else {
				winner = currentBid;
				winner.raiseBid(baseBid + (Math.max(0, newSteps + 1) * minBidIncrement * 2) - minBidIncrement);
				loser = bid;
			}
			
		}

		if (previousBidAmount <= winner.getBidAmount()) {
			// Did the new bid win?
			if (winner.equals(bid)) {
				// Let other plugins figure out any reasons why this buy shouldn't happen.
				AuctionBidEvent auctionBidEvent = new AuctionBidEvent(bidder, this, functions.getUnsafeMoney(bid.getBidAmount()), functions.getUnsafeMoney(bid.getMaxBidAmount()), true);
				Bukkit.getServer().getPluginManager().callEvent(auctionBidEvent);
				if (auctionBidEvent.isCancelled()) {
					failBid(bid, "bid-fail-blocked-by-other-plugin");
				} else {
					setNewBid(bid, "bid-success-outbid");
				}
			} else {
				// Did the old bid have to raise the bid to stay winner?
				if (previousBidAmount < winner.getBidAmount()) {
					if (!this.sealed && !AuctionConfig.getBoolean("broadcast-bid-updates", scope)) {
						messageManager.broadcastAuctionMessage(Lists.newArrayList("bid-auto-outbid"), this);
					}
					failBid(bid, "bid-fail-auto-outbid");
				} else {
					if (!this.sealed) messageManager.sendPlayerMessage(Lists.newArrayList("bid-fail-too-low"), bid.getBidder(), this);
					failBid(bid, null);
				}
			}
		} else {
			// Seriously don't know what could cause this, but might as well take care of it.
			messageManager.sendPlayerMessage(Lists.newArrayList("bid-fail-too-low"), bid.getBidder(), this);
		}
	}
	
	/**
	 * Disposes of a failed bid attempt.
	 * 
	 * @param attemptedBid the attempted bid
	 * @param reason message key to send to looser
	 */
	private void failBid(AuctionBid attemptedBid, String reason) {
		attemptedBid.cancelBid();
		if (this.sealed && (attemptedBid.getError() == null || attemptedBid.getError().isEmpty())) {
			messageManager.sendPlayerMessage(Lists.newArrayList("bid-success-sealed"), attemptedBid.getBidder(), this);
		} else {
			messageManager.sendPlayerMessage(Lists.newArrayList(reason), attemptedBid.getBidder(), this);
		}
	}
	
	/**
	 * Assigns new bid and alerts those in the hosting AuctionScope.
	 * 
	 * @param newBid the new bid
	 * @param reason message key to broadcast
	 */
	private void setNewBid(AuctionBid newBid, String reason) {
		AuctionBid prevBid = currentBid;
		
		if (AuctionConfig.getBoolean("expire-buynow-at-first-bid", scope)) this.buyNow = 0;
		
		if (currentBid != null) {
			currentBid.cancelBid();
		}
		currentBid = newBid;
		if (this.sealed) {
			messageManager.sendPlayerMessage(Lists.newArrayList("bid-success-sealed"), newBid.getBidder(), this);
		} else if (AuctionConfig.getBoolean("broadcast-bid-updates", scope)) {
			messageManager.broadcastAuctionMessage(Lists.newArrayList(reason), this);
		} else {
			messageManager.sendPlayerMessage(Lists.newArrayList(reason), newBid.getBidder(), this);
			if (prevBid != null && newBid.getBidder().equalsIgnoreCase(prevBid.getBidder())) {
				messageManager.sendPlayerMessage(Lists.newArrayList(reason), prevBid.getBidder(), this);
			}
		}
		AuctionParticipant.addParticipant(newBid.getBidder(), scope);
		if (currentBid.getBidAmount() >= buyNow) {
			buyNow = 0;
		}
		
        // see if antisnipe is enabled...
        if (!this.sealed && AuctionConfig.getBoolean("anti-snipe", scope) == true && this.getRemainingTime() <= AuctionConfig.getInt("anti-snipe-prevention-seconds", scope)) {
        	this.addToRemainingTime(AuctionConfig.getInt("anti-snipe-prevention-seconds", scope));
			messageManager.broadcastAuctionMessage(Lists.newArrayList("anti-snipe-time-added"), this);
        }
	}
	
	/**
	 * Checks the item in hand to see if it's valid and allowed.
	 * 
	 * @return acceptability of held item for auctioning
	 */
	private Boolean parseHeldItem() {
		Player owner = Bukkit.getPlayer(ownerName);
		if (lot != null) {
			return true;
		}
		ItemStack heldItem = owner.getItemInHand();
		if (heldItem == null || heldItem.getAmount() == 0) {
			messageManager.sendPlayerMessage(Lists.newArrayList("auction-fail-hand-is-empty"), ownerName, this);
			return false;
		}
		lot = new AuctionLot(heldItem, ownerName);
		
		ItemStack itemType = lot.getTypeStack();
		
		if (
				!AuctionConfig.getBoolean("allow-damaged-items", scope) &&
				itemType.getType().getMaxDurability() > 0 &&
				itemType.getDurability() > 0
		) {
			messageManager.sendPlayerMessage(Lists.newArrayList("auction-fail-damaged-item"), ownerName, this);
			lot = null;
			return false;
		}
		
    	String displayName = items.getDisplayName(itemType);
    	if (displayName == null) displayName = "";
    	
		if (!displayName.isEmpty() && !AuctionConfig.getBoolean("allow-renamed-items", scope)) {
			messageManager.sendPlayerMessage(Lists.newArrayList("auction-fail-renamed-item"), ownerName, this);
			lot = null;
			return false;
		}
		
		// Check lore:
		String[] lore = items.getLore(heldItem);
		List<String> bannedLore = AuctionConfig.getStringList("banned-lore", scope);
		if (lore != null && bannedLore != null) {
			for (int i = 0; i < bannedLore.size(); i++) {
				for (int j = 0; j < lore.length; j++) {
					if (lore[j].toLowerCase().contains(bannedLore.get(i).toLowerCase())) {
						messageManager.sendPlayerMessage(Lists.newArrayList("auction-fail-banned-lore"), ownerName, this);
						lot = null;
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
	private Boolean parseArgs() {
		// (amount) (starting price) (increment) (time) (buynow)
		if (!parseArgAmount()) return false;
		if (!parseArgStartingBid()) return false;
		if (!parseArgIncrement()) return false;
		if (!parseArgTime()) return false;
		if (!parseArgBuyNow()) return false;
		return true;
	}
	
	/**
	 * Checks auction starter ability to start auction.
	 * 
	 * @return acceptability of starter auctioning
	 */
	private Boolean isValidOwner() {
		if (ownerName == null) {
			messageManager.sendPlayerMessage(Lists.newArrayList("auction-fail-invalid-owner"), null, this);
			return false;
		}
		return true;
	}
	
	/**
	 * Checks lot quantity range and availability in starter's inventory.
	 * 
	 * @return acceptability of lot quantity
	 */
	private Boolean isValidAmount() {
		if (quantity <= 0) {
			messageManager.sendPlayerMessage(Lists.newArrayList("auction-fail-quantity-too-low"), ownerName, this);
			return false;
		}
		
		// TODO: Add config setting for max quantity.
		
		if (!items.hasAmount(ownerName, quantity, lot.getTypeStack())) {
			messageManager.sendPlayerMessage(Lists.newArrayList("auction-fail-insufficient-supply"), ownerName, this);
			return false;
		}
		return true;
	}
	
	/**
	 * Checks starting bid.
	 * 
	 * @return if starting bid is ok
	 */
	private Boolean isValidStartingBid() {
		if (startingBid < 0) {
			messageManager.sendPlayerMessage(Lists.newArrayList("auction-fail-starting-bid-too-low"), ownerName, this);
			return false;
		} else if (startingBid > AuctionConfig.getSafeMoneyFromDouble("max-starting-bid", scope)) {
			messageManager.sendPlayerMessage(Lists.newArrayList("auction-fail-starting-bid-too-high"), ownerName, this);
			return false;
		}
		return true;
	}
	
	/**
	 * Checks minimum bid increment.
	 * 
	 * @return if minimum bid increment is okay
	 */
	private Boolean isValidIncrement() {
		if (getMinBidIncrement() < AuctionConfig.getSafeMoneyFromDouble("min-bid-increment", scope)) {
			messageManager.sendPlayerMessage(Lists.newArrayList("auction-fail-increment-too-low"), ownerName, this);
			return false;
		}
		if (getMinBidIncrement() > AuctionConfig.getSafeMoneyFromDouble("max-bid-increment", scope)) {
			messageManager.sendPlayerMessage(Lists.newArrayList("auction-fail-increment-too-high"), ownerName, this);
			return false;
		}
		return true;
	}
	
	/**
	 * Checks BuyNow amount.
	 * 
	 * @return if BuyNow amount is okay
	 */
	private Boolean isValidBuyNow() {
		if (getBuyNow() < 0) {
			messageManager.sendPlayerMessage(Lists.newArrayList("auction-fail-buynow-too-low"), ownerName, this);
			return false;
		}
		if (getBuyNow() > AuctionConfig.getSafeMoneyFromDouble("max-buynow", scope)) {
			messageManager.sendPlayerMessage(Lists.newArrayList("auction-fail-buynow-too-high"), ownerName, this);
			return false;
		}
		return true;
	}
	
	/**
	 * Checks auction time limit.
	 * 
	 * @return if auction time limit is okiedokie
	 */
	private Boolean isValidTime() {
		if (time < AuctionConfig.getInt("min-auction-time", scope)) {
			messageManager.sendPlayerMessage(Lists.newArrayList("auction-fail-time-too-low"), ownerName, this);
			return false;
		}
		if (time > AuctionConfig.getInt("max-auction-time", scope)) {
			messageManager.sendPlayerMessage(Lists.newArrayList("auction-fail-time-too-high"), ownerName, this);
			return false;
		}
		return true;
	}
	
	/**
	 * Parses quantity argument.
	 * 
	 * @return if quantity argument parsed correctly
	 */
	private Boolean parseArgAmount() {
		if (quantity > 0) return true;

		ItemStack lotType = lot.getTypeStack();
		if (args.length > 0) {
			if (args[0].equalsIgnoreCase("this") || args[0].equalsIgnoreCase("hand")) {
				quantity = lotType.getAmount();
			} else if (args[0].equalsIgnoreCase("all")) {
				quantity = items.getAmount(ownerName, lotType);
			} else if (args[0].matches("[0-9]{1,7}")) {
				quantity = Integer.parseInt(args[0]);
			} else {
				messageManager.sendPlayerMessage(Lists.newArrayList("parse-error-invalid-quantity"), ownerName, this);
				return false;
			}
		} else {
			quantity = lotType.getAmount();
		}
		if (quantity < 0) {
			messageManager.sendPlayerMessage(Lists.newArrayList("parse-error-invalid-quantity"), ownerName, this);
			return false;
		}
		return true;
	}
	
	/**
	 * Parses starting bid argument.
	 * 
	 * @return if argument parsed correctly
	 */
	private Boolean parseArgStartingBid() {
		if (startingBid > 0) return true;
		
		if (args.length > 1) {
			if (!args[1].isEmpty() && args[1].matches(floAuction.decimalRegex)) {
				startingBid = functions.getSafeMoney(Double.parseDouble(args[1]));
			} else {
				messageManager.sendPlayerMessage(Lists.newArrayList("parse-error-invalid-starting-bid"), ownerName, this);
				return false;
			}
		} else {
			startingBid = AuctionConfig.getSafeMoneyFromDouble("default-starting-bid", scope);
		}
		if (startingBid < 0) {
			messageManager.sendPlayerMessage(Lists.newArrayList("parse-error-invalid-starting-bid"), ownerName, this);
			return false;
		}
		return true;
	}
	
	/**
	 * Parses minimum bid increment.
	 * 
	 * @return if minimum bid increment parsed correctly
	 */
	private Boolean parseArgIncrement() {
		if (minBidIncrement > 0) return true;

		if (args.length > 2) {
			if (!args[2].isEmpty() && args[2].matches(floAuction.decimalRegex)) {
				minBidIncrement = functions.getSafeMoney(Double.parseDouble(args[2]));
			} else {
				messageManager.sendPlayerMessage(Lists.newArrayList("parse-error-invalid-bid-increment"), ownerName, this);
				return false;
			}
		} else {
			minBidIncrement = AuctionConfig.getSafeMoneyFromDouble("default-bid-increment", scope);
		}
		if (minBidIncrement < 0) {
			messageManager.sendPlayerMessage(Lists.newArrayList("parse-error-invalid-bid-increment"), ownerName, this);
			return false;
		}
		return true;
	}
	
	/**
	 * Parses time argument.
	 * 
	 * @return if time argument parsed correctly
	 */
	private Boolean parseArgTime() {
		if (time > 0) return true;

		if (args.length > 3) {
			if (args[3].matches("[0-9]{1,7}")) {
				time = Integer.parseInt(args[3]);
			} else {
				messageManager.sendPlayerMessage(Lists.newArrayList("parse-error-invalid-time"), ownerName, this);
				return false;
			}
		} else {
			time = AuctionConfig.getInt("default-auction-time", scope);
		}
		if (time < 0) {
			messageManager.sendPlayerMessage(Lists.newArrayList("parse-error-invalid-time"), ownerName, this);
			return false;
		}
		return true;
	}
	
	/**
	 * Parses BuyNow argument.
	 * 
	 * @return if BuyNow argument parsed correctly
	 */
	private Boolean parseArgBuyNow() {
		
		if (this.sealed || !AuctionConfig.getBoolean("allow-buynow", scope)) {
			this.buyNow = 0;
			return true;
		}

		if (getBuyNow() > 0) return true;

		if (args.length > 4) {
			if (!args[4].isEmpty() && args[4].matches(floAuction.decimalRegex)) {
				this.buyNow = functions.getSafeMoney(Double.parseDouble(args[4]));
			} else {
				messageManager.sendPlayerMessage(Lists.newArrayList("parse-error-invalid-buynow"), ownerName, this);
				return false;
			}
		} else {
			this.buyNow = 0;
		}
		if (getBuyNow() < 0) {
			messageManager.sendPlayerMessage(Lists.newArrayList("parse-error-invalid-buynow"), ownerName, this);
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
		return minBidIncrement;
	}
	
	/**
	 * Gets a type stack of the items being auctioned.
	 * 
	 * @return stack of example items
	 */
	public ItemStack getLotType() {
		if (lot == null) {
			return null;
		}
		return lot.getTypeStack();
	}
	
	/**
	 * Gets quantity of the auctioned lot.
	 * 
	 * @return amount being auctioned
	 */
	public int getLotQuantity() {
		if (lot == null) {
			return 0;
		}
		return lot.getQuantity();
	}
	
	/**
	 * Gets the lowest amount first bid can be.
	 * 
	 * @return lowest possible starting bid in floAuction's proprietary "safe money"
	 */
	public long getStartingBid() {
		long effectiveStartingBid = startingBid;
		if (effectiveStartingBid == 0) {
			effectiveStartingBid = minBidIncrement; 
		}
		return effectiveStartingBid;
	}

	/**
	 * Gets the AuctionBid object for the current winning bid.
	 * 
	 * @return AuctionBid object of leader
	 */
	public AuctionBid getCurrentBid() {
		return currentBid;
	}
	
	/**
	 * Gets the name of the player who started and therefore "owns" the auction.
	 * 
	 * @return auction owner name
	 */
	public String getOwner() {
		return ownerName;
	}
	
	/**
	 * Gets the amount of time remaining in the auction.
	 * 
	 * @return number of seconds remaining in auction
	 */
	public int getRemainingTime() {
		return countdown;
	}
	
	/**
	 * Gets the originally specified auction time limit.  This does not take into account time added by anti-snipe being triggered.
	 * 
	 * @return original auction length in seconds
	 */
	public int getTotalTime() {
		return time;
	}

	/**
	 * Adds to the remaining auction countdown.
	 * 
	 * @param secondsToAdd
	 * @return
	 */
    public int addToRemainingTime(int secondsToAdd) {
            countdown += secondsToAdd;
            return countdown;
    }

    /**
     * Gets the amount specified for BuyNow.
     * 
     * @return BuyNow amount in floAuction's proprietary "safe money"
     */
	public long getBuyNow() {
		return buyNow;
	}

	public String getOwnerDisplayName() {
		Player ownerPlayer = Bukkit.getPlayer(ownerName);
		if (ownerPlayer != null) {
			return ownerPlayer.getDisplayName();
		} else {
			return ownerName;
		}
	}
}
