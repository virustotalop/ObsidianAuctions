package com.flobi.floAuction;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.flobi.utility.items;

public class AuctionLot {
	private String ownerName;
	private ItemStack lotTypeLock;
	private int quantity = 0;
	
	public AuctionLot(ItemStack lotType, String lotOwner) {
		// Lots can only have one type of item per lot.
		lotTypeLock = lotType.clone();
		ownerName = lotOwner;
	}
	public boolean AddItems(int addQuantity, boolean removeFromOwner) {
		if (removeFromOwner) {
			if (!items.hasAmount(ownerName, addQuantity, lotTypeLock)) {
				return false;
			}
			items.remove(ownerName, addQuantity, lotTypeLock);
		}
		quantity += addQuantity;
		return true;
	}
	
	public void winLot(String winnerName) {
		giveLot(winnerName);
	}
	public void cancelLot() {
		giveLot(ownerName);
	}
	
	
	private void giveLot(String playerName) {
		ownerName = playerName;
		Player player = floAuction.server.getPlayer(playerName);
		int maxStackSize = lotTypeLock.getType().getMaxStackSize();
		if (player != null && player.isOnline()) {
			int amountToGive = 0;
			if (items.hasSpace(player, quantity, lotTypeLock)) {
				amountToGive = quantity;
			} else {
				amountToGive = items.getSpaceForItem(player, lotTypeLock);
			}
			// Give whatever items space permits at this time.
			ItemStack typeStack = getTypeStack();
			if (amountToGive > 0) {
				if (
						!floAuction.useGoldStandard || 
						(
								!items.isSameItem(typeStack, new ItemStack(371)) &&
								!items.isSameItem(typeStack, new ItemStack(266)) &&
								!items.isSameItem(typeStack, new ItemStack(41))
						)
				) {
					floAuction.sendMessage("lot-give", player, null);
				}
			}
			while (amountToGive > 0) {
				ItemStack givingItems = lotTypeLock.clone();
				givingItems.setAmount(Math.min(maxStackSize, amountToGive));
				quantity -= givingItems.getAmount();
				player.getInventory().addItem(givingItems);
				amountToGive -= maxStackSize;
			}
			if (quantity > 0) {
				// Drop items at player's feet.
				
				// Move items to drop lot.
				typeStack.setAmount(quantity);
				quantity = 0;
				
				// Drop lot.
				player.getWorld().dropItemNaturally(player.getLocation(), typeStack);
				floAuction.sendMessage("lot-drop", player, null);
			}
		} else {
			// Player is offline, queue lot for give on login.
			// Create orphaned lot to try to give when inventory clears up.
			final AuctionLot orphanLot = new AuctionLot(lotTypeLock, playerName);
			
			// Move items to orphan lot
			orphanLot.AddItems(quantity, false);
			quantity = 0;
			
			// Queue for distribution on space availability.
			floAuction.OrphanLots.add(orphanLot);
		}
	}
	public ItemStack getTypeStack() {
		// Return clone so the caller can't change type.
		return lotTypeLock.clone();
	}
	public String getOwner() {
		return ownerName;
	}
	public void setOwner(String ownerName) {
		this.ownerName = ownerName;
	}
	public int getQuantity() {
		return quantity;
	}
}
