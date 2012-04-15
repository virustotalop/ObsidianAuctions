package com.flobi.utility;

import java.text.DecimalFormat;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

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

	public static boolean hasAmount(Player player, int amount, ItemStack compareItem){
		int has = getAmount(player, compareItem);
		if (has >= amount) {
			return true;
		} else {
			return false;
		}
	}

	public static int getAmount(Player player, ItemStack compareItem) {
		PlayerInventory inventory = player.getInventory();
		ItemStack[] items = inventory.getContents();
		int has = 0;
		for (ItemStack item : items) {
			if (isSameItem(compareItem, item)) {
				has += item.getAmount();
			}
		}
		return has;
	}
	
	public static int safeMoney(Double money) {
        DecimalFormat twoDForm = new DecimalFormat("#");
        return Integer.valueOf(twoDForm.format(money * 100));
	}
	public static double unsafeMoney(int money) {
		return (double)money / 100;
	}

}
