package com.flobi.floAuction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.FireworkEffect;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.flobi.utility.functions;
import com.flobi.utility.items;

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
	private AuctionBid currentBid;
	public ArrayList<AuctionBid> sealedBids = new ArrayList<AuctionBid>(); 
	
	public boolean sealed = false;
	public boolean web = false;
	
	public long nextTickTime = 0;
	
	// Scheduled timers:
	private int countdown = 0;
	private int countdownTimer = 0;
	
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
	public Auction(floAuction plugin, Player auctionOwner, String[] inputArgs, AuctionScope scope, boolean sealed, boolean web) {
		ownerName = auctionOwner.getName();
		args = functions.mergeInputArgs(auctionOwner.getName(), inputArgs, false);
		this.plugin = plugin; 
		this.scope = scope;
		this.sealed = sealed;
		this.web = false;
	}
	
	/**
	 * Attempts to start this auction instance.  Returns success.
	 * 
	 * @return whether or not the auction start succeeded
	 */
	public Boolean start() {
		
		if (ArenaManager.isInArena(Bukkit.getPlayer(ownerName))) {
			floAuction.sendMessage("arena-warning", ownerName, null);
			return false;
		}
		
		ItemStack typeStack = lot.getTypeStack();
		double preAuctionTax = AuctionConfig.getDouble("auction-start-tax", scope);
		
		// Check banned items:
		List<String> bannedItems = AuctionConfig.getStringList("banned-items", scope);
		for (int i = 0; i < bannedItems.size(); i++) {
			if (items.isSameItem(typeStack, bannedItems.get(i))) {
				floAuction.sendMessage("auction-fail-banned", ownerName, scope);
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
				floAuction.sendMessage("auction-fail-start-tax", ownerName, scope);
				return false;
			}
		}
		
		if (!lot.addItems(quantity, true)) {
			floAuction.sendMessage("auction-fail-insufficient-supply", ownerName, scope);
			return false;
		}

		if (preAuctionTax > 0D) {
			if (floAuction.econ.has(ownerName, preAuctionTax)) {
				floAuction.econ.withdrawPlayer(ownerName, preAuctionTax);
				extractedPreTax = preAuctionTax;
				floAuction.sendMessage("auction-start-tax", getOwner(), scope);
				String taxDestinationUser = AuctionConfig.getString("deposit-tax-to-user", scope);
				if (!taxDestinationUser.isEmpty()) floAuction.econ.depositPlayer(taxDestinationUser, preAuctionTax);
			}
		}

		active = true;
		floAuction.sendMessage("auction-start", (CommandSender) null, scope, true);
		
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
				    	floAuction.sendMessage("timer-countdown-notification", (CommandSender) null, scope, true);
				    	return;
			    	}
			    	if (thisAuction.time >= 20) {
			    		if (thisAuction.countdown == (int) (thisAuction.time / 2)) {
					    	floAuction.sendMessage("timer-countdown-notification", (CommandSender) null, scope, true);
			    		}
			    	}
		    	}
		    }
		}, 1L, 1L);
		nextTickTime = System.currentTimeMillis() + 1000;

		info(null, true);
		return true;
	}
	
	/**
	 * Sends auction info to chat.
	 * 
	 * @param sender the CommandSender initiating the request
	 * @param fullBroadcast whether to send the message to everyone in the hosting AuctionScope
	 */
	public void info(CommandSender sender, boolean fullBroadcast) {
		List<String> messageKeys = new ArrayList<String>();
		
		ItemStack itemType = this.getLotType();
		short maxDurability = itemType.getType().getMaxDurability();
		short currentDurability = itemType.getDurability();
		Map<Enchantment, Integer> enchantments = itemType.getEnchantments();
		if (enchantments == null || enchantments.size() == 0) enchantments = items.getStoredEnchantments(itemType);
		if (!active) {
			floAuction.sendMessage("auction-info-no-auction", sender, scope, fullBroadcast);
			return;
		} else if (fullBroadcast && AuctionConfig.getBoolean("suppress-auction-start-info", scope)) {
			messageKeys.add("auction-info-suppressed-alt");
			if (AuctionConfig.getBoolean("allow-buynow", scope) && getBuyNow() > 0) messageKeys.add("auction-info-buynow");
		} else if (sealed) {
			messageKeys.add("auction-info-header-sealed");
			if (items.getDisplayName(itemType) != null && !items.getDisplayName(itemType).isEmpty()) messageKeys.add("auction-info-display-name");
			if (items.getBookTitle(itemType) != null && !items.getBookTitle(itemType).isEmpty()) messageKeys.add("auction-info-book-title");
			if (items.getBookAuthor(itemType) != null && !items.getBookAuthor(itemType).isEmpty()) messageKeys.add("auction-info-book-author");
			if (enchantments != null && enchantments.size() > 0) messageKeys.add("auction-info-enchantment");
			if (maxDurability > 0 && currentDurability > 0) messageKeys.add("auction-info-damage");

			// Firework data
			FireworkEffect[] payload = items.getFireworkEffects(itemType);
			if (payload != null && payload.length > 0) {
				messageKeys.add("auction-info-payload");
			}
			if (itemType.getType() == Material.FIREWORK) {
				messageKeys.add("auction-info-payload-power");
			}
			String[] lore = items.getLore(itemType);
			if (lore != null && lore.length > 0) {
				messageKeys.add("auction-info-lore-header");
				messageKeys.add("auction-info-lore-detail");
				messageKeys.add("auction-info-lore-footer");
			}

			messageKeys.add("auction-info-footer-sealed");
		} else if (currentBid == null) {
			messageKeys.add("auction-info-header-nobids");
			if (items.getDisplayName(itemType) != null && !items.getDisplayName(itemType).isEmpty()) messageKeys.add("auction-info-display-name");
			if (items.getBookTitle(itemType) != null && !items.getBookTitle(itemType).isEmpty()) messageKeys.add("auction-info-book-title");
			if (items.getBookAuthor(itemType) != null && !items.getBookAuthor(itemType).isEmpty()) messageKeys.add("auction-info-book-author");
			if (enchantments != null && enchantments.size() > 0) messageKeys.add("auction-info-enchantment");
			if (maxDurability > 0 && currentDurability > 0) messageKeys.add("auction-info-damage");

			// Firework data
			FireworkEffect[] payload = items.getFireworkEffects(itemType);
			if (payload != null && payload.length > 0) {
				messageKeys.add("auction-info-payload");
			}
			if (itemType.getType() == Material.FIREWORK) {
				messageKeys.add("auction-info-payload-power");
			}
			String[] lore = items.getLore(itemType);
			if (lore != null && lore.length > 0) {
				messageKeys.add("auction-info-lore-header");
				messageKeys.add("auction-info-lore-detail");
				messageKeys.add("auction-info-lore-footer");
			}
			
			messageKeys.add("auction-info-footer-nobids");
			if (AuctionConfig.getBoolean("allow-buynow", scope) && getBuyNow() > 0) messageKeys.add("auction-info-buynow");
		} else {
			messageKeys.add("auction-info-header");
			if (items.getDisplayName(itemType) != null && !items.getDisplayName(itemType).isEmpty()) messageKeys.add("auction-info-display-name");
			if (items.getBookTitle(itemType) != null && !items.getBookTitle(itemType).isEmpty()) messageKeys.add("auction-info-book-title");
			if (items.getBookAuthor(itemType) != null && !items.getBookAuthor(itemType).isEmpty()) messageKeys.add("auction-info-book-author");
			if (enchantments != null && enchantments.size() > 0) messageKeys.add("auction-info-enchantment");
			if (maxDurability > 0 && currentDurability > 0) messageKeys.add("auction-info-damage");

			// Firework data
			FireworkEffect[] payload = items.getFireworkEffects(itemType);
			if (payload != null && payload.length > 0) {
				messageKeys.add("auction-info-payload");
			}
			if (itemType.getType() == Material.FIREWORK) {
				messageKeys.add("auction-info-payload-power");
			}
			String[] lore = items.getLore(itemType);
			if (lore != null && lore.length > 0) {
				messageKeys.add("auction-info-lore-header");
				messageKeys.add("auction-info-lore-detail");
				messageKeys.add("auction-info-lore-footer");
			}
			
			messageKeys.add("auction-info-footer");
			if (AuctionConfig.getBoolean("allow-buynow", scope) && getBuyNow() > 0) messageKeys.add("auction-info-buynow");
		}
		floAuction.sendMessage(messageKeys, sender, scope, fullBroadcast);
	}
	
	/**
	 * Cancels the Auction instance and disposes of it normally.
	 */
	public void cancel() {
		floAuction.sendMessage("auction-cancel", (CommandSender) null, scope, true);
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
		ownerName = authority.getName();
		floAuction.sendMessage("auction-confiscated", (CommandSender) null, scope, true);
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
		if (currentBid == null || lot == null) {
			floAuction.sendMessage("auction-end-nobids", (CommandSender) null, scope, true);
			if (lot != null) lot.cancelLot();
			if (currentBid != null) currentBid.cancelBid();
		} else {
			floAuction.sendMessage("auction-end", (CommandSender) null, scope, true);
			lot.winLot(currentBid.getBidder());
			currentBid.winBid();
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

		if (ArenaManager.isInArena(bidder)) {
			floAuction.sendMessage("arena-warning", bidder.getName(), null);
			return;
		}
		
		// BuyNow
		if (AuctionConfig.getBoolean("allow-buynow", scope) && inputArgs.length > 0) {
			if (inputArgs[0].equalsIgnoreCase("buy")) {

				if (buyNow == 0 || (currentBid != null && currentBid.getBidAmount() >= buyNow)) {
					floAuction.sendMessage("bid-fail-buynow-expired", bidder, scope, false);
				} else {
					inputArgs[0] = Double.toString(functions.getUnsafeMoney(buyNow));
					AuctionBid bid = new AuctionBid(this, bidder, inputArgs);
					if (bid.getError() != null) {
						failBid(bid, bid.getError());
						return;
					} else {
						// raisOwnBid does nothing if it's not the current bidder.
						if (currentBid != null) bid.raiseOwnBid(currentBid);
						setNewBid(bid, null);
						end();
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
			setNewBid(bid, "bid-success-no-challenger");
			return;
		}
		long previousBidAmount = currentBid.getBidAmount();
		long previousMaxBidAmount = currentBid.getMaxBidAmount();
		if (currentBid.getBidder().equals(bidder.getName())) {
			if (bid.raiseOwnBid(currentBid)) {
				setNewBid(bid, "bid-success-update-own-bid");
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
				setNewBid(bid, "bid-success-outbid");
			} else {
				// Did the old bid have to raise the bid to stay winner?
				if (previousBidAmount < winner.getBidAmount()) {
					if (!this.sealed && !AuctionConfig.getBoolean("broadcast-bid-updates", scope)) floAuction.sendMessage("bid-auto-outbid", (CommandSender) null, scope, true);
					failBid(bid, "bid-fail-auto-outbid");
				} else {
					if (!this.sealed) floAuction.sendMessage("bid-fail-too-low", bid.getBidder(), scope);
					failBid(bid, null);
				}
			}
		} else {
			// Seriously don't know what could cause this, but might as well take care of it.
			floAuction.sendMessage("bid-fail-too-low", bid.getBidder(), scope);
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
			floAuction.sendMessage("bid-success-sealed", attemptedBid.getBidder(), scope);
		} else {
			floAuction.sendMessage(reason, attemptedBid.getBidder(), scope);
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
			floAuction.sendMessage("bid-success-sealed", newBid.getBidder(), scope);
		} else if (AuctionConfig.getBoolean("broadcast-bid-updates", scope)) {
			floAuction.sendMessage(reason, (CommandSender) null, scope, true);
		} else {
			floAuction.sendMessage(reason, newBid.getBidder(), scope);
			if (prevBid != null && newBid.getBidder().equalsIgnoreCase(prevBid.getBidder())) {
				floAuction.sendMessage(reason, prevBid.getBidder(), scope);
			}
		}
		AuctionParticipant.addParticipant(newBid.getBidder(), scope);
		
        // see if antisnipe is enabled...
        if (!this.sealed && AuctionConfig.getBoolean("anti-snipe", scope) == true && this.getRemainingTime() <= AuctionConfig.getInt("anti-snipe-prevention-seconds", scope)) {
        	this.addToRemainingTime(AuctionConfig.getInt("anti-snipe-prevention-seconds", scope));
	        floAuction.sendMessage("anti-snipe-time-added", null, scope, true);
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
			floAuction.sendMessage("auction-fail-hand-is-empty", owner, scope, false);
			return false;
		}
		lot = new AuctionLot(heldItem, ownerName);
		
		ItemStack itemType = lot.getTypeStack();
		
		if (
				!AuctionConfig.getBoolean("allow-damaged-items", scope) &&
				itemType.getType().getMaxDurability() > 0 &&
				itemType.getDurability() > 0
		) {
			floAuction.sendMessage("auction-fail-damaged-item", owner, scope, false);
			lot = null;
			return false;
		}
		
    	String displayName = items.getDisplayName(itemType);
    	if (displayName == null) displayName = "";
    	
		if (!displayName.isEmpty() && !AuctionConfig.getBoolean("allow-renamed-items", scope)) {
			floAuction.sendMessage("auction-fail-renamed-item", owner, scope, false);
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
						floAuction.sendMessage("auction-fail-banned-lore", owner, scope, false);
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
			floAuction.sendMessage("auction-fail-invalid-owner", (Player) plugin.getServer().getConsoleSender(), scope, false);
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
			floAuction.sendMessage("auction-fail-quantity-too-low", ownerName, scope);
			return false;
		}
		
		// TODO: Add config setting for max quantity.
		
		if (!items.hasAmount(ownerName, quantity, lot.getTypeStack())) {
			floAuction.sendMessage("auction-fail-insufficient-supply", ownerName, scope);
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
			floAuction.sendMessage("auction-fail-starting-bid-too-low", ownerName, scope);
			return false;
		} else if (startingBid > AuctionConfig.getSafeMoneyFromDouble("max-starting-bid", scope)) {
			floAuction.sendMessage("auction-fail-starting-bid-too-high", ownerName, scope);
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
			floAuction.sendMessage("auction-fail-increment-too-low", ownerName, scope);
			return false;
		}
		if (getMinBidIncrement() > AuctionConfig.getSafeMoneyFromDouble("max-bid-increment", scope)) {
			floAuction.sendMessage("auction-fail-increment-too-high", ownerName, scope);
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
			floAuction.sendMessage("auction-fail-buynow-too-low", ownerName, scope);
			return false;
		}
		if (getBuyNow() > AuctionConfig.getSafeMoneyFromDouble("max-buynow", scope)) {
			floAuction.sendMessage("auction-fail-buynow-too-high", ownerName, scope);
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
			floAuction.sendMessage("auction-fail-time-too-low", ownerName, scope);
			return false;
		}
		if (time > AuctionConfig.getInt("max-auction-time", scope)) {
			floAuction.sendMessage("auction-fail-time-too-high", ownerName, scope);
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
				floAuction.sendMessage("parse-error-invalid-quantity", ownerName, scope);
				return false;
			}
		} else {
			quantity = lotType.getAmount();
		}
		if (quantity < 0) {
			floAuction.sendMessage("parse-error-invalid-quantity", ownerName, scope);
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
				floAuction.sendMessage("parse-error-invalid-starting-bid", ownerName, scope);
				return false;
			}
		} else {
			startingBid = AuctionConfig.getSafeMoneyFromDouble("default-starting-bid", scope);
		}
		if (startingBid < 0) {
			floAuction.sendMessage("parse-error-invalid-starting-bid", ownerName, scope);
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
				floAuction.sendMessage("parse-error-invalid-bid-increment", ownerName, scope);
				return false;
			}
		} else {
			minBidIncrement = AuctionConfig.getSafeMoneyFromDouble("default-bid-increment", scope);
		}
		if (minBidIncrement < 0) {
			floAuction.sendMessage("parse-error-invalid-bid-increment", ownerName, scope);
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
				floAuction.sendMessage("parse-error-invalid-time", ownerName, scope);
				return false;
			}
		} else {
			time = AuctionConfig.getInt("default-auction-time", scope);
		}
		if (time < 0) {
			floAuction.sendMessage("parse-error-invalid-time", ownerName, scope);
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
				floAuction.sendMessage("parse-error-invalid-buynow", ownerName, scope);
				return false;
			}
		} else {
			this.buyNow = 0;
		}
		if (getBuyNow() < 0) {
			floAuction.sendMessage("parse-error-invalid-buynow", ownerName, scope);
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
}
