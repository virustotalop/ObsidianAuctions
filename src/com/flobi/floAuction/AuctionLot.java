package com.flobi.floAuction;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.flobi.utility.items;

public class AuctionLot {
	private Player owner;
	private ItemStack lotTypeLock;
	private int quantity = 0;
	
	public AuctionLot(ItemStack lotType, Player lotOwner) {
		// Lots can only have one type of item per lot.
		lotTypeLock = lotType.clone();
		owner = lotOwner;
	}
	public boolean AddItems(int addQuantity, boolean removeFromOwner) {
		if (removeFromOwner) {
			if (!items.hasAmount(owner, addQuantity, lotTypeLock)) {
				return false;
			}
		}
		quantity += addQuantity;
		items.remove(owner, addQuantity, lotTypeLock);
		return true;
	}
	
	public void winLot(Player winner) {
		giveLot(winner);
	}
	public void cancelLot() {
		giveLot(owner);
	}
	
	
	private void giveLot(Player player) {
		owner = player;
		int amountToGive = 0;
		if (items.hasSpace(owner, quantity, lotTypeLock)) {
			amountToGive = quantity;
		} else {
			amountToGive = items.getSpaceForItem(owner, lotTypeLock);
		}
		// Give whatever items space permits at this time.
		if (amountToGive > 0) {
			ItemStack givingItems = lotTypeLock.clone();
			givingItems.setAmount(amountToGive);
			owner.getInventory().addItem(givingItems);
			quantity -= amountToGive;
		}
		if (quantity > 0) {
			if (player.isOnline()) {
				// Drop items at player's feet.
				ItemStack droppingItems = lotTypeLock.clone();
				
				// Move items to drop lot.
				droppingItems.setAmount(quantity);
				quantity = 0;
				
				// Drop lot.
				owner.getWorld().dropItemNaturally(owner.getLocation(), droppingItems);
			} else {
				// Player is offline, queue lot for give on login.
				// Create orphaned lot to try to give when inventory clears up.
				AuctionLot orphanLot = new AuctionLot(lotTypeLock, owner);
				
				// Move items to orphan lot
				orphanLot.AddItems(quantity, false);
				quantity = 0;
				
				// Queue for distribution on space availability.
				floAuction.OrphanLots.add(orphanLot);
			}
		}
	}
	public ItemStack getTypeStack() {
		// Return clone so the caller can't change type.
		return lotTypeLock.clone();
	}
	public Player getOwner() {
		return owner;
	}
	public int getQuantity() {
		return quantity;
	}
}
