package com.flobi.floAuction;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.FireworkEffect;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentWrapper;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.flobi.utility.items;

public class AuctionLot implements java.io.Serializable {
	private static final long serialVersionUID = -1764290458703647129L;
	private String ownerName;
	private int quantity = 0;
	private int lotTypeId;
	private short lotDurability;
	private Map<Integer, Integer> lotEnchantments;
	private Map<Integer, Integer> storedEnchantments;
	private int sourceStackQuantity = 0;
	private String displayName = "";
	private String bookAuthor = "";
	private String bookTitle = "";
	private String[] bookPages = null;
	private Integer repairCost = null;
	private String headOwner = null;
	private Integer power = 0;
	private FireworkEffect[] effects = null;
	private String[] lore = null;
//	private Map<String, Object> itemSerialized = null;
	private String itemSerialized = null;
	
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
	@SuppressWarnings("deprecation")
	public ItemStack getTypeStack() {
		ItemStack lotTypeLock = null;
		if (this.itemSerialized != null) {
//			lotTypeLock = ItemStack.deserialize(this.itemSerialized);
			FileConfiguration tmpconfig = new YamlConfiguration();
			try {
				tmpconfig.loadFromString(this.itemSerialized);
				if (tmpconfig.isItemStack("itemstack")) {
					return tmpconfig.getItemStack("itemstack");
				}
			} catch (InvalidConfigurationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		// The rest of this remains for backward compatibility.
		lotTypeLock = new ItemStack(lotTypeId, 1, lotDurability);
		
		for (Entry<Integer, Integer> enchantment : lotEnchantments.entrySet()) {
			lotTypeLock.addUnsafeEnchantment(new EnchantmentWrapper(enchantment.getKey()), enchantment.getValue());
		}
		for (Entry<Integer, Integer> enchantment : storedEnchantments.entrySet()) {
			items.addStoredEnchantment(lotTypeLock, enchantment.getKey(), enchantment.getValue(), true);
		}
		lotTypeLock.setAmount(sourceStackQuantity);
		items.setDisplayName(lotTypeLock, displayName);
		items.setBookAuthor(lotTypeLock, bookAuthor);
		items.setBookTitle(lotTypeLock, bookTitle);
		items.setBookPages(lotTypeLock, bookPages);
		items.setRepairCost(lotTypeLock, repairCost);
		items.setHeadOwner(lotTypeLock, headOwner);
		items.setFireworkPower(lotTypeLock, power);
		items.setFireworkEffects(lotTypeLock, effects);
		items.setLore(lotTypeLock, lore);
		return lotTypeLock;
	}
	@SuppressWarnings("deprecation")
	private void setLotType(ItemStack lotType) {
//		this.itemSerialized = lotType.serialize();
		FileConfiguration tmpconfig = new YamlConfiguration();
		tmpconfig.set("itemstack", lotType);
		itemSerialized = tmpconfig.saveToString();

		// The rest of this remains for backward compatibility.
		lotTypeId = lotType.getTypeId();
		lotDurability = lotType.getDurability();
		sourceStackQuantity = lotType.getAmount();
		lotEnchantments = new HashMap<Integer, Integer>();
		storedEnchantments = new HashMap<Integer, Integer>();
		Map<Enchantment, Integer> enchantmentList = lotType.getEnchantments();
		for (Entry<Enchantment, Integer> enchantment : enchantmentList.entrySet()) {
			lotEnchantments.put(enchantment.getKey().getId(), enchantment.getValue());
		}
		enchantmentList = items.getStoredEnchantments(lotType);
		if (enchantmentList != null) for (Entry<Enchantment, Integer> enchantment : enchantmentList.entrySet()) {
			storedEnchantments.put(enchantment.getKey().getId(), enchantment.getValue());
		}
		displayName = items.getDisplayName(lotType);
		bookAuthor = items.getBookAuthor(lotType);
		bookTitle = items.getBookTitle(lotType);
		bookPages = items.getBookPages(lotType);
		repairCost = items.getRepairCost(lotType);
		headOwner = items.getHeadOwner(lotType);
		power = items.getFireworkPower(lotType);
		effects = items.getFireworkEffects(lotType);
		lore = items.getLore(lotType);
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
