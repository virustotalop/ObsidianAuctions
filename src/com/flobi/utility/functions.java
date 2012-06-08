package com.flobi.utility;

import java.text.DecimalFormat;

import net.milkbowl.vault.economy.EconomyResponse;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.flobi.floAuction.AuctionLot;
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
		if (!floAuction.useGoldStandard) {
			return floAuction.econ.format(unsafeMoney);
		}
		String amountText = "";
		if (unsafeMoney == 1) {
			amountText = floAuction.textConfig.getString("gold-standard-name-singular"); 
		} else {
			amountText = floAuction.textConfig.getString("gold-standard-name-plural"); 
		}
		long nuggets = getSafeMoney(unsafeMoney);
		return amountText.replaceAll("%g", Long.toString(nuggets));
	}
	
	public static boolean withdrawPlayer(String playerName, long safeMoney) {
		return withdrawPlayer(playerName, getUnsafeMoney(safeMoney));
	}
	
	public static boolean withdrawPlayer(String playerName, double unsafeMoney) {
		if (!floAuction.useGoldStandard) {
			EconomyResponse receipt = floAuction.econ.withdrawPlayer(playerName, unsafeMoney);
			return receipt.transactionSuccess();
		}
		return updateGold(playerName, 0 - getSafeMoney(unsafeMoney));
	}
	
	public static void depositPlayer(String playerName, double unsafeMoney) {
		if (!floAuction.useGoldStandard) {
			floAuction.econ.depositPlayer(playerName, unsafeMoney);
			return;
		}
		updateGold(playerName, getSafeMoney(unsafeMoney));
		
	}
	
	public static long getSafeMoney(Double money) {
        DecimalFormat twoDForm = new DecimalFormat("#");
        return Long.valueOf(twoDForm.format(money * Math.pow(10, floAuction.decimalPlaces)));
	}
	
	public static double getUnsafeMoney(long money) {
		return (double)money / Math.pow(10, floAuction.decimalPlaces);
	}
	
	private static boolean updateGold(String playerName, long nuggetAdjustment) {
		Player player = floAuction.server.getPlayer(playerName);
		if (nuggetAdjustment < 0) {
			if (player == null || !player.isOnline()) return false;
		}
		if (player == null || !player.isOnline()) {
			// Add as cancelled lots if adding and player isn't online.
			long nuggets = nuggetAdjustment;
			long ingots = (long) Math.floor((double) nuggets / 9);
			long blocks = (long) Math.floor((double) ingots / 9);
			nuggets -= (ingots * 9);
			ingots -= (blocks * 9);

			ItemStack nugget = new ItemStack(371);
			ItemStack ingot = new ItemStack(266);
			ItemStack block = new ItemStack(41);

			AuctionLot nuggetLot = new AuctionLot(nugget, playerName);
			nuggetLot.AddItems((int) nuggets, false);
			nuggetLot.cancelLot();
			AuctionLot ingotLot = new AuctionLot(ingot, playerName);
			ingotLot.AddItems((int) ingots, false);
			ingotLot.cancelLot();
			AuctionLot blockLot = new AuctionLot(block, playerName);
			blockLot.AddItems((int) blocks, false);
			blockLot.cancelLot();
			return true;
		}
		long nuggets = nuggetAdjustment;
		ItemStack nugget = new ItemStack(371);
		ItemStack ingot = new ItemStack(266);
		ItemStack block = new ItemStack(41);
		
		ItemStack[] items = player.getInventory().getContents();
		for (ItemStack current : items) {
			if (current == null) {
				continue;
			}
			if (isSameItem(nugget, current)) {
				nuggets += current.getAmount();
			}
			if (isSameItem(ingot, current)) {
				nuggets += current.getAmount() * 9;
			}
			if (isSameItem(block, current)) {
				nuggets += current.getAmount() * 81;
			}
		}
		return setGoldAmount(player, nuggets);
	}
	
	private static boolean setGoldAmount(Player player, long nuggets) {
		if (nuggets < 0) return false;

		ItemStack nugget = new ItemStack(371);
		ItemStack ingot = new ItemStack(266);
		ItemStack block = new ItemStack(41);

		// Clear all gold in the inventory first.
		ItemStack[] inventoryItems = player.getInventory().getContents();
		for (ItemStack current : inventoryItems) {
			if (current == null) {
				continue;
			}
			if (
					isSameItem(nugget, current) ||
					isSameItem(ingot, current) ||
					isSameItem(block, current)
			) {
				player.getInventory().remove(current);
			}
		}

		long ingots = (long) Math.floor((double) nuggets / 9);
		long blocks = (long) Math.floor((double) ingots / 9);
		nuggets -= (ingots * 9);
		ingots -= (blocks * 9);
		
		AuctionLot nuggetLot = new AuctionLot(nugget, player.getName());
		nuggetLot.AddItems((int) nuggets, false);
		nuggetLot.cancelLot();
		AuctionLot ingotLot = new AuctionLot(ingot, player.getName());
		ingotLot.AddItems((int) ingots, false);
		ingotLot.cancelLot();
		AuctionLot blockLot = new AuctionLot(block, player.getName());
		blockLot.AddItems((int) blocks, false);
		blockLot.cancelLot();
		
		return true;
	}

}
















