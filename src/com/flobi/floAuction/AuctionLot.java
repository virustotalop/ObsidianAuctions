package com.flobi.floAuction;

import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.flobi.utility.items;

public class AuctionLot implements java.io.Serializable {
	private static final long serialVersionUID = -1764290458703647129L;
	private String ownerName;
	private int quantity = 0;
	private ItemStack typeStack;
	
	public AuctionLot(ItemStack lotType, String lotOwner) {
		// Lots can only have one type of item per lot.
		ownerName = lotOwner;
		setLotType(lotType);
	}
	public boolean AddItems(int addQuantity, boolean removeFromOwner) {
		if (removeFromOwner) {
			if (!items.hasAmount(ownerName, addQuantity, getTypeStack())) {
				return false;
			}
			items.remove(ownerName, addQuantity, getTypeStack());
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
		if (quantity == 0) return;
		ItemStack lotTypeLock = getTypeStack();
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
				floAuction.sendMessage("lot-give", player, null, false);
			}
			while (amountToGive > 0) {
				ItemStack givingItems = lotTypeLock.clone();
				givingItems.setAmount(Math.min(maxStackSize, amountToGive));
				quantity -= givingItems.getAmount();
				
//				player.getInventory().addItem();
				items.saferItemGive(player.getInventory(), givingItems);
				
				amountToGive -= maxStackSize;
			}
			if (quantity > 0) {
				// Drop items at player's feet.
				
				// Move items to drop lot.
				while (quantity > 0) {
					ItemStack cloneStack = typeStack.clone();
					cloneStack.setAmount(Math.min(quantity, items.getMaxStackSize(typeStack)));
					quantity -= cloneStack.getAmount();
					
					// Drop lot.
					Item drop = player.getWorld().dropItemNaturally(player.getLocation(), cloneStack);
					drop.setItemStack(cloneStack);
				}
				floAuction.sendMessage("lot-drop", player, null, false);
			}
		} else {
			// Player is offline, queue lot for give on login.
			// Create orphaned lot to try to give when inventory clears up.
			final AuctionLot orphanLot = new AuctionLot(lotTypeLock, playerName);
			
			// Move items to orphan lot
			orphanLot.AddItems(quantity, false);
			quantity = 0;
			
			// Queue for distribution on space availability.
			floAuction.orphanLots.add(orphanLot);
			floAuction.saveObject(floAuction.orphanLots, "orphanLots.ser");
		}
	}
	public ItemStack getTypeStack() {
		return typeStack.clone();
	}
	private void setLotType(ItemStack lotType) {
		typeStack = lotType.clone();
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
