package com.flobi.utility;

import java.util.ArrayList;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;


public class items {
	/*
	 * Formats a string such as 17:2 or 17;2
	 */
	
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
	
	public static int getMaxStackSize(ItemStack item) {
		if (item == null)
			return 0;
		
		int maxStackSize = item.getType().getMaxStackSize();
		// If bukkit has any bad stack sized, override them now.
		
		return maxStackSize;
	}

	public static boolean isStackable(int id) {
		if (getMaxStackSize(new ItemStack(id)) > 1)
			return true;
		else
			return false;
	}

	public static int getSpaceForItem(Player player, ItemStack item) {
		int maxstack = getMaxStackSize(item);
		int space = 0;

		ItemStack[] items = player.getInventory().getContents();
		for (ItemStack current : items) {
			if (current == null) {
				space += maxstack;
				continue;
			}
			if (isSameItem(item, current)) {
				space += maxstack - current.getAmount();
			}
		}
		
		return space;
	}
	
	public static boolean hasSpace(Player player, int needed, ItemStack item) {
		return getSpaceForItem(player, item) >= needed;
	}

	public static boolean hasSpace(Player player, ArrayList<ItemStack> lot) {
		int needed = 0;

		for (ItemStack item : lot) {
			if (item != null) {
				needed += item.getAmount();
			}
		}
		return hasSpace(player, needed, lot.get(0));

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
	public static void remove(Player player, int amount, ItemStack compareItem){
		PlayerInventory inventory = player.getInventory();
    	
    	// Remove held item first:
		if (isSameItem(compareItem, player.getItemInHand())) { 
	    	int heldAmount = player.getItemInHand().getAmount();
	    	if (heldAmount <= amount) {
	    		amount -= heldAmount;
	    		inventory.clear(inventory.getHeldItemSlot());
	    	} else {
	    		player.getItemInHand().setAmount(heldAmount - amount);
	    		amount = 0;
	    	}
		}
    	
    	int counter = amount;
    	int leftover = 0;

    	// Remove from other stacks:
    	for (int invIndex = 0; invIndex < inventory.getSize(); invIndex++) {
    		ItemStack current = inventory.getItem(invIndex);

    		if (current == null || current.getAmount() <= 0) 
    			continue;
    		
    		if (!isSameItem(compareItem, current)) 
    			continue;

    		if (current.getAmount() > counter) {
    			leftover = current.getAmount() - counter;
    		}

    		if (leftover != 0) {
    			current.setAmount(leftover);
    			counter = 0;
    			break;
    		} else {
    			counter -= current.getAmount();
    			inventory.clear(invIndex);
    		}
    	}
	}

	public static boolean isEnchantable(ItemStack heldItem) {
		ItemStack item = new ItemStack(heldItem.getType());
		for (Enchantment ench : Enchantment.values()) {
			if (ench.canEnchantItem(item)) {
				return true;
			}
		}
		return false;
	}
	
}
