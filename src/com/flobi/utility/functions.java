package com.flobi.utility;

import java.text.DecimalFormat;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.flobi.floAuction.floAuction;

public class functions {

	public static boolean isSameItem(ItemStack item1, ItemStack item2) {
		// Is it even an item?
		if (item1 == null) return false;
		if (item2 == null) return false;

		// Type must be the same:
		if (item1.getTypeId() != item2.getTypeId()) return false;
		
		// Data must be the same:
		if (item1.getData().getData() != item2.getData().getData()) return false;
		
		// Damage must be the same:
		if (item1.getDurability() != item2.getDurability()) return false;
		
		// Enchantments must be the same:
		if (!item1.getEnchantments().equals(item2.getEnchantments())) return false;
		
		return true;
	}

	public static boolean hasAmount(String playerName, int amount, ItemStack compareItem){
		int has = getAmount(playerName, compareItem);
		if (has >= amount) {
			return true;
		} else {
			return false;
		}
	}

	public static int getAmount(String playerName, ItemStack compareItem) {
		PlayerInventory inventory = floAuction.server.getPlayer(playerName).getInventory();
		ItemStack[] items = inventory.getContents();
		int has = 0;
		for (ItemStack item : items) {
			if (isSameItem(compareItem, item)) {
				has += item.getAmount();
			}
		}
		return has;
	}
	
	public static long safeMoney(Double money) {
        DecimalFormat twoDForm = new DecimalFormat("#");
        return Long.valueOf(twoDForm.format(money * 100));
	}
	public static double unsafeMoney(long amountToReserve) {
		return (double)amountToReserve / 100;
	}

}
