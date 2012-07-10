package com.flobi.utility;

import java.text.DecimalFormat;

import net.milkbowl.vault.economy.EconomyResponse;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.flobi.floAuction.floAuction;

public class functions {

	// Item functions.
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
		return has >= amount;
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
	
	// Money functions.
	public static String formatAmount(long safeMoney) {
		return formatAmount(getUnsafeMoney(safeMoney));
	}
	
	public static String formatAmount(double unsafeMoney) {
		if (floAuction.econ == null) return "-";
		return floAuction.econ.format(unsafeMoney);
	}
	
	public static boolean withdrawPlayer(String playerName, long safeMoney) {
		return withdrawPlayer(playerName, getUnsafeMoney(safeMoney));
	}
	
	public static boolean withdrawPlayer(String playerName, double unsafeMoney) {
		EconomyResponse receipt = floAuction.econ.withdrawPlayer(playerName, unsafeMoney);
		return receipt.transactionSuccess();
	}
	
	public static boolean depositPlayer(String playerName, double unsafeMoney) {
		EconomyResponse receipt = floAuction.econ.depositPlayer(playerName, unsafeMoney);
		return receipt.transactionSuccess();
	}
	
	public static long getSafeMoney(Double money) {
        DecimalFormat twoDForm = new DecimalFormat("#");
        return Long.valueOf(twoDForm.format(money * Math.pow(10, floAuction.decimalPlaces)));
	}
	
	public static double getUnsafeMoney(long money) {
		return (double)money / Math.pow(10, floAuction.decimalPlaces);
	}

}
















