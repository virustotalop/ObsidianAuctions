package com.flobi.floAuction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.FireworkEffect;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.flobi.utility.functions;
import com.flobi.utility.items;

public class Auction {
	protected floAuction plugin;
	private String[] args;
	private String ownerName;
	private String scope;
	
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
	
	public long nextTickTime = 0;
	
	// Scheduled timers:
	private int countdown = 0;
	private int countdownTimer = 0;
	
	public String getScope() {
		return scope;
	}
	
	public Auction(floAuction plugin, Player auctionOwner, String[] inputArgs, String scope, boolean sealed) {
		ownerName = auctionOwner.getName();
		args = functions.mergeInputArgs(auctionOwner.getName(), inputArgs, false);
		this.plugin = plugin; 
		this.scope = scope;
		this.sealed = sealed;
	}
	
	public Boolean start() {
		
		ItemStack typeStack = lot.getTypeStack();
		double preAuctionTax = floAuction.taxPerAuction;
		
		// Check banned items:
		for (int i = 0; i < floAuction.bannedItems.size(); i++) {
			if (items.isSameItem(typeStack, floAuction.bannedItems.get(i))) {
				floAuction.sendMessage("auction-fail-banned", ownerName, this);
				return false;
			}
		}
		
		for (Map.Entry<String, String> entry : floAuction.taxedItems.entrySet()) {
			if (items.isSameItem(typeStack, entry.getKey())) {
				String itemTax = entry.getValue();
				
				if (itemTax.endsWith("a")) {
					try {
						preAuctionTax = Double.valueOf(itemTax.substring(0, itemTax.length() - 1));
					} catch (Exception e) {
						// Clearly this isn't a valid number, just forget about it.
						preAuctionTax = floAuction.taxPerAuction;
					}
				} else if (!itemTax.endsWith("%")) {
					try {
						preAuctionTax = Double.valueOf(itemTax);
						preAuctionTax *= quantity;
					} catch (Exception e) {
						// Clearly this isn't a valid number, just forget about it.
						preAuctionTax = floAuction.taxPerAuction;
					}
				}
				break;
			}
		}		
		
		if (preAuctionTax > 0D) {
			if (!floAuction.econ.has(ownerName, preAuctionTax)) {
				floAuction.sendMessage("auction-fail-start-tax", ownerName, this);
				return false;
			}
		}
		
		if (!lot.AddItems(quantity, true)) {
			floAuction.sendMessage("auction-fail-insufficient-supply", ownerName, this);
			return false;
		}

		if (preAuctionTax > 0D) {
			if (floAuction.econ.has(ownerName, preAuctionTax)) {
				floAuction.econ.withdrawPlayer(ownerName, preAuctionTax);
				extractedPreTax = preAuctionTax;
				floAuction.sendMessage("auction-start-tax", getOwner(), this);
				if (!floAuction.taxDestinationUser.isEmpty()) floAuction.econ.depositPlayer(floAuction.taxDestinationUser, preAuctionTax);
			}
		}

		active = true;
		floAuction.currentAuctionOwnerLocation = floAuction.server.getPlayer(ownerName).getLocation().clone();
		floAuction.currentAuctionOwnerGamemode = floAuction.server.getPlayer(ownerName).getGameMode();
		floAuction.sendMessage("auction-start", (CommandSender) null, this, true);
		
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
		    	if (!floAuction.suppressCountdown){
			    	if (thisAuction.countdown < 4) {
				    	floAuction.sendMessage("timer-countdown-notification", (CommandSender) null, thisAuction, true);
				    	return;
			    	}
			    	if (thisAuction.time >= 20) {
			    		if (thisAuction.countdown == (int) (thisAuction.time / 2)) {
					    	floAuction.sendMessage("timer-countdown-notification", (CommandSender) null, thisAuction, true);
			    		}
			    	}
		    	}
		    }
		}, 1L, 1L);
		nextTickTime = System.currentTimeMillis() + 1000;

		info(null, true);
		return true;
	}
	public void info(CommandSender sender, boolean fullBroadcast) {
		List<String> messageKeys = new ArrayList<String>();
		
		ItemStack itemType = this.getLotType();
		short maxDurability = itemType.getType().getMaxDurability();
		short currentDurability = itemType.getDurability();
		Map<Enchantment, Integer> enchantments = itemType.getEnchantments();
		if (enchantments == null || enchantments.size() == 0) enchantments = items.getStoredEnchantments(itemType);
		if (!active) {
			floAuction.sendMessage("auction-info-no-auction", sender, this, fullBroadcast);
			return;
		} else if (fullBroadcast && floAuction.suppressAuctionStartInfo) {
			messageKeys.add("auction-info-suppressed-alt");
			if (floAuction.allowBuyNow && getBuyNow() > 0) messageKeys.add("auction-info-buynow");
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
			if (floAuction.allowBuyNow && getBuyNow() > 0) messageKeys.add("auction-info-buynow");
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
			if (floAuction.allowBuyNow && getBuyNow() > 0) messageKeys.add("auction-info-buynow");
		}
		floAuction.sendMessage(messageKeys, sender, this, fullBroadcast);
	}
	public void cancel() {
		floAuction.sendMessage("auction-cancel", (CommandSender) null, this, true);
		if (lot != null) lot.cancelLot();
		if (currentBid != null) currentBid.cancelBid();
		dispose();
	}
	public void confiscate(Player authority) {
		ownerName = authority.getName();
		floAuction.sendMessage("auction-confiscated", (CommandSender) null, this, true);
		if (lot != null) {
			lot.setOwner(authority.getName());
			lot.cancelLot();
		}
		if (currentBid != null) currentBid.cancelBid();
		dispose();
	}
	public void end() {
		if (currentBid == null || lot == null) {
			floAuction.sendMessage("auction-end-nobids", (CommandSender) null, this, true);
			if (lot != null) lot.cancelLot();
			if (currentBid != null) currentBid.cancelBid();
		} else {
			floAuction.sendMessage("auction-end", (CommandSender) null, this, true);
			lot.winLot(currentBid.getBidder());
			currentBid.winBid();
		}
		dispose();
	}
	private void dispose() {
		plugin.getServer().getScheduler().cancelTask(countdownTimer);

		sealed = false;
		for(int i = 0; i < sealedBids.size(); i++) {
			sealedBids.get(i).cancelBid();
		}
		
		plugin.detachAuction(this);
	}
	public Boolean isValid() {
		if (!isValidOwner()) return false;
		if (!isValidParticipant()) return false;
		if (!parseHeldItem()) return false;
		if (!parseArgs()) return false;
		if (!isValidAmount()) return false;
		if (!isValidStartingBid()) return false;
		if (!isValidIncrement()) return false;
		if (!isValidTime()) return false;
		if (!isValidBuyNow()) return false;
		return true;
	}
	public void Bid(Player bidder, String[] inputArgs) {

		// BuyNow
		if (floAuction.allowBuyNow && inputArgs.length > 0) {
			if (inputArgs[0].equalsIgnoreCase("buy")) {

				if (buyNow == 0 || (currentBid != null && currentBid.getBidAmount() >= buyNow)) {
					floAuction.sendMessage("bid-fail-buynow-expired", bidder, this, false);
				} else {
					inputArgs[0] = Long.toString(buyNow);
					AuctionBid bid = new AuctionBid(this, bidder, inputArgs);
					if (bid.getError() != null) {
						failBid(bid, bid.getError());
						return;
					} else {
						// raisOwnBid does nothing if it's not the current bidder.
						bid.raiseOwnBid(currentBid);
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
		
		if (floAuction.useOldBidLogic) {
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
					if (!this.sealed && !floAuction.broadCastBidUpdates) floAuction.sendMessage("bid-auto-outbid", (CommandSender) null, this, true);
					failBid(bid, "bid-fail-auto-outbid");
				} else {
					if (!this.sealed) floAuction.sendMessage("bid-fail-too-low", bid.getBidder(), this);
					failBid(bid, null);
				}
			}
		} else {
			// Seriously don't know what could cause this, but might as well take care of it.
			floAuction.sendMessage("bid-fail-too-low", bid.getBidder(), this);
		}
		
		
		
	}
	private void failBid(AuctionBid newBid, String reason) {
		newBid.cancelBid();
		if (this.sealed && (newBid.getError() == null || newBid.getError().isEmpty())) {
			floAuction.sendMessage("bid-success-sealed", newBid.getBidder(), this);
		} else {
			floAuction.sendMessage(reason, newBid.getBidder(), this);
		}
	}
	private void setNewBid(AuctionBid newBid, String reason) {
		AuctionBid prevBid = currentBid;
		
		if (floAuction.expireBuyNowOnFirstBid) setBuyNow(0);
		
		if (currentBid != null) {
			currentBid.cancelBid();
		}
		currentBid = newBid;
		floAuction.currentBidPlayerLocation = floAuction.server.getPlayer(newBid.getBidder()).getLocation().clone();
		floAuction.currentBidPlayerGamemode = floAuction.server.getPlayer(newBid.getBidder()).getGameMode();
		if (this.sealed) {
			floAuction.sendMessage("bid-success-sealed", newBid.getBidder(), this);
		} else if (floAuction.broadCastBidUpdates) {
			floAuction.sendMessage(reason, (CommandSender) null, this, true);
		} else {
			floAuction.sendMessage(reason, newBid.getBidder(), this);
			if (prevBid != null && newBid.getBidder().equalsIgnoreCase(prevBid.getBidder())) {
				floAuction.sendMessage(reason, prevBid.getBidder(), this);
			}
		}
		Participant.addParticipant(newBid.getBidder());
		
        // see if antisnipe is enabled...
        if (!this.sealed && floAuction.antiSnipe == true && this.getRemainingTime() <= floAuction.antiSnipePreventionSeconds) {
        	this.addToRemainingTime((floAuction.antiSnipeExtensionSeconds));
	        floAuction.sendMessage("anti-snipe-time-added", null, this, true);
        }
	}
	private Boolean parseHeldItem() {
		Player owner = floAuction.server.getPlayer(ownerName);
		if (lot != null) {
			return true;
		}
		ItemStack heldItem = owner.getItemInHand();
		if (heldItem == null || heldItem.getAmount() == 0) {
			floAuction.sendMessage("auction-fail-hand-is-empty", owner, this, false);
			return false;
		}
		lot = new AuctionLot(heldItem, ownerName);
		
		ItemStack itemType = lot.getTypeStack();
		
		if (
				!floAuction.allowDamagedItems &&
				itemType.getType().getMaxDurability() > 0 &&
				itemType.getDurability() > 0
		) {
			floAuction.sendMessage("auction-fail-damaged-item", owner, this, false);
			lot = null;
			return false;
		}
		
    	String displayName = items.getDisplayName(itemType);
    	if (displayName == null) displayName = "";
    	
		if (!displayName.isEmpty() && !floAuction.allowRenamedItems) {
			floAuction.sendMessage("auction-fail-renamed-item", owner, this, false);
			lot = null;
			return false;
		}
		
		// Check lore:
		String[] lore = items.getLore(heldItem);
		if (lore != null && floAuction.bannedLore != null) {
			for (int i = 0; i < floAuction.bannedLore.size(); i++) {
				for (int j = 0; j < lore.length; j++) {
					if (lore[j].toLowerCase().contains(floAuction.bannedLore.get(i).toLowerCase())) {
						floAuction.sendMessage("auction-fail-banned-lore", owner, this, false);
						lot = null;
						return false;
					}
				}
			}
		}
		
		return true;
	}
	private Boolean parseArgs() {
		// (amount) (starting price) (increment) (time) (buynow)
		if (!parseArgAmount()) return false;
		if (!parseArgStartingBid()) return false;
		if (!parseArgIncrement()) return false;
		if (!parseArgTime()) return false;
		if (!parseArgBuyNow()) return false;
		return true;
	}
	private Boolean isValidOwner() {
		if (ownerName == null) {
			floAuction.sendMessage("auction-fail-invalid-owner", (Player) plugin.getServer().getConsoleSender(), this, false);
			return false;
		}
		return true;
	}
	
	private Boolean isValidParticipant() {
		if (Participant.checkLocation(ownerName)) {
			return true;
		}
		floAuction.sendMessage("auction-fail-outside-auctionhouse", ownerName, this);
		return false;
	}
	
	private Boolean isValidAmount() {
		if (quantity <= 0) {
			floAuction.sendMessage("auction-fail-quantity-too-low", ownerName, this);
			return false;
		}
		if (!items.hasAmount(ownerName, quantity, lot.getTypeStack())) {
			floAuction.sendMessage("auction-fail-insufficient-supply", ownerName, this);
			return false;
		}
		return true;
	}
	private Boolean isValidStartingBid() {
		if (startingBid < 0) {
			floAuction.sendMessage("auction-fail-starting-bid-too-low", ownerName, this);
			return false;
		} else if (startingBid > floAuction.maxStartingBid) {
			floAuction.sendMessage("auction-fail-starting-bid-too-high", ownerName, this);
			return false;
		}
		return true;
	}
	private Boolean isValidIncrement() {
		if (getMinBidIncrement() < floAuction.minIncrement) {
			floAuction.sendMessage("auction-fail-increment-too-low", ownerName, this);
			return false;
		}
		if (getMinBidIncrement() > floAuction.maxIncrement) {
			floAuction.sendMessage("auction-fail-increment-too-high", ownerName, this);
			return false;
		}
		return true;
	}
	private Boolean isValidBuyNow() {
		if (getBuyNow() < 0) {
			floAuction.sendMessage("auction-fail-buynow-too-low", ownerName, this);
			return false;
		}
		if (getBuyNow() > floAuction.maxBuyNow) {
			floAuction.sendMessage("auction-fail-buynow-too-high", ownerName, this);
			return false;
		}
		return true;
	}
	private Boolean isValidTime() {
		if (time < floAuction.minTime) {
			floAuction.sendMessage("auction-fail-time-too-low", ownerName, this);
			return false;
		}
		if (time > floAuction.maxTime) {
			floAuction.sendMessage("auction-fail-time-too-high", ownerName, this);
			return false;
		}
		return true;
	}
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
				floAuction.sendMessage("parse-error-invalid-quantity", ownerName, this);
				return false;
			}
		} else {
			quantity = lotType.getAmount();
		}
		if (quantity < 0) {
			floAuction.sendMessage("parse-error-invalid-quantity", ownerName, this);
			return false;
		}
		return true;
	}
	private Boolean parseArgStartingBid() {
		if (startingBid > 0) return true;
		
		if (args.length > 1) {
			if (!args[1].isEmpty() && args[1].matches(floAuction.decimalRegex)) {
				startingBid = functions.getSafeMoney(Double.parseDouble(args[1]));
			} else {
				floAuction.sendMessage("parse-error-invalid-starting-bid", ownerName, this);
				return false;
			}
		} else {
			startingBid = floAuction.defaultStartingBid;
		}
		if (startingBid < 0) {
			floAuction.sendMessage("parse-error-invalid-starting-bid", ownerName, this);
			return false;
		}
		return true;
	}
	private Boolean parseArgIncrement() {
		if (minBidIncrement > 0) return true;

		if (args.length > 2) {
			if (!args[2].isEmpty() && args[2].matches(floAuction.decimalRegex)) {
				minBidIncrement = functions.getSafeMoney(Double.parseDouble(args[2]));
			} else {
				floAuction.sendMessage("parse-error-invalid-bid-increment", ownerName, this);
				return false;
			}
		} else {
			minBidIncrement = floAuction.defaultBidIncrement;
		}
		if (minBidIncrement < 0) {
			floAuction.sendMessage("parse-error-invalid-bid-increment", ownerName, this);
			return false;
		}
		return true;
	}
	private Boolean parseArgTime() {
		if (time > 0) return true;

		if (args.length > 3) {
			if (args[3].matches("[0-9]{1,7}")) {
				time = Integer.parseInt(args[3]);
			} else {
				floAuction.sendMessage("parse-error-invalid-time", ownerName, this);
				return false;
			}
		} else {
			time = floAuction.defaultAuctionTime;
		}
		if (time < 0) {
			floAuction.sendMessage("parse-error-invalid-time", ownerName, this);
			return false;
		}
		return true;
	}
	private Boolean parseArgBuyNow() {
		
		if (this.sealed || !floAuction.allowBuyNow) {
			setBuyNow(0);
			return true;
		}

		if (getBuyNow() > 0) return true;

		if (args.length > 4) {
			if (!args[4].isEmpty() && args[4].matches(floAuction.decimalRegex)) {
				setBuyNow(functions.getSafeMoney(Double.parseDouble(args[4])));
			} else {
				floAuction.sendMessage("parse-error-invalid-buynow", ownerName, this);
				return false;
			}
		} else {
			setBuyNow(0);
		}
		if (getBuyNow() < 0) {
			floAuction.sendMessage("parse-error-invalid-buynow", ownerName, this);
			return false;
		}
		return true;
	}
	public long getMinBidIncrement() {
		return minBidIncrement;
	}
	
	public ItemStack getLotType() {
		if (lot == null) {
			return null;
		}
		return lot.getTypeStack();
	}
	
	public int getLotQuantity() {
		if (lot == null) {
			return 0;
		}
		return lot.getQuantity();
	}
	public long getStartingBid() {
		long effectiveStartingBid = startingBid;
		if (effectiveStartingBid == 0) {
			effectiveStartingBid = minBidIncrement; 
		}
		return effectiveStartingBid;
	}
	public AuctionBid getCurrentBid() {
		return currentBid;
	}
	public String getOwner() {
		return ownerName;
	}
	public int getRemainingTime() {
		return countdown;
	}
	public int getTotalTime() {
		return time;
	}

    public int addToRemainingTime(int i) {
            countdown += i;
            return countdown;
    }

	public long getBuyNow() {
		return buyNow;
	}

	public void setBuyNow(long buyNow) {
		this.buyNow = buyNow;
	}
}
