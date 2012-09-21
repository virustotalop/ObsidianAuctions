package com.flobi.floAuction;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentWrapper;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.flobi.utility.items;

public class AuctionLot implements java.io.Serializable {
	private static final long serialVersionUID = -1764290458703647129L;
	private String ownerName;
	private int quantity = 0;
	private int lotTypeId;
	@SuppressWarnings("unused") // Will be necessary later when Bukkit gets fixed.
	private byte lotDataData;
	private short lotDurability;
	private Map<Integer, Integer> lotEnchantments;
	private int sourceStackQuantity = 0;
	private String bookAuthor = "";
	private String bookTitle = "";
	private String[] bookPages = null;
	
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
				floAuction.sendMessage("lot-give", player, null);
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
			floAuction.orphanLots.add(orphanLot);
			floAuction.saveObject(floAuction.orphanLots, "orphanLots.ser");
		}
	}
	public ItemStack getTypeStack() {
		CraftItemStack lotTypeLock = new CraftItemStack(lotTypeId, 1, lotDurability);
		for (Entry<Integer, Integer> enchantment : lotEnchantments.entrySet()) {
			lotTypeLock.addUnsafeEnchantment(new EnchantmentWrapper(enchantment.getKey()), enchantment.getValue());
		}
		lotTypeLock.setAmount(sourceStackQuantity);
		items.setBookAuthor(lotTypeLock, bookAuthor);
		items.setBookTitle(lotTypeLock, bookTitle);
		items.setBookPages(lotTypeLock, bookPages);
		return lotTypeLock;
	}
	private void setLotType(ItemStack lotType) {
		lotTypeId = lotType.getTypeId();
		lotDataData = lotType.getData().getData();
		lotDurability = lotType.getDurability();
		sourceStackQuantity = lotType.getAmount();
		lotEnchantments = new HashMap<Integer, Integer>();
		Map<Enchantment, Integer> enchantmentList = lotType.getEnchantments();
		for (Entry<Enchantment, Integer> enchantment : enchantmentList.entrySet()) {
			lotEnchantments.put(enchantment.getKey().getId(), enchantment.getValue());
		}
		bookAuthor = items.getBookAuthor((CraftItemStack)lotType);
		bookTitle = items.getBookTitle((CraftItemStack)lotType);
		bookPages = items.getBookPages((CraftItemStack)lotType);
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
