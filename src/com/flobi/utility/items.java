package com.flobi.utility;

import java.util.ArrayList;
import org.bukkit.DyeColor;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.material.MaterialData;


public class items {
	/*
	 * Formats a string such as 17:2 or 17;2
	 */
	
	public static MaterialData formatItem(String string) {
		Integer[] id = new Integer[1];
		if (string.contains(":")) {
			String[] split = string.split(":");
			id[0] = Integer.valueOf(split[0]);
			id[1] = Integer.valueOf(split[1]);
			return getMatfromID(id[0], Byte.valueOf(id[1].toString()));
		} else if (string.contains(";")) {
			String[] split = string.split(";");
			id[0] = Integer.valueOf(split[0]);
			id[1] = Integer.valueOf(split[1]);
			return getMatfromID(id[0], Byte.valueOf(id[1].toString()));
		} else {
			id[0] = Integer.valueOf(string);
			return getMatfromID(id[0]);
		}

	}

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
	
    public static void repair(ItemStack heldItem)
    {
        if (isDamageable(heldItem.getTypeId())) {
        	heldItem.setDurability((short) 0);
        }
    }	
	public static MaterialData getMatfromID(int id, Byte data) {
		MaterialData dat = new MaterialData(id, data);
		return dat;

	}

	public static MaterialData getMatfromID(int id) {
		MaterialData dat = new MaterialData(id);
		return dat;
	}
	
	public static boolean isDamageable(ItemStack heldItem) {
		return isDamageable(heldItem.getTypeId());
	}
	
	public static boolean isDamageable(int id) {
		Short durability = getMatfromID(id).getItemType().getMaxDurability();
		// If bukkit has any bad durabilities, correct now.
		
		if (durability > 0) {
			return true;
		}
		return false;
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

	private static String getName(ItemStack item) {
		return getName(item.getTypeId(), item.getData().getData(), item.getDurability());
	}

	public static String getName(int id, byte data, short damage) {
		ItemStack item = new ItemStack(id, 1, (short) 0 , data);
		String itemPrefix = "";
		String itemName = item.getType().toString();
		
		switch (id) {
		case 5: // Plank
			// no "break;" because we want to continue into 
		case 17: // Log
		case 6: // Sapling
		case 18: // Leaves
			// The built-in names for these suck.
			if (id == 5) {
				itemName = "plank";
			} else if (id == 17) {
				itemName = "log";
			}
			
			// Don't want to change the actual data, just get the name.
			byte displayData = data;
			while (displayData > 3) displayData -= 4;
			switch ((short) displayData) {
				case 0:
					itemPrefix = "oak ";
					break;
				case 1:
					itemPrefix = "pine ";
					break;
				case 2:
					itemPrefix = "birch ";
					break;
				case 3:
					itemPrefix = "jungle ";
					break;
			}
			break;
		case 24: // Sandstone
			switch ((short) data) {
				case 0:
					itemPrefix = "";
					break;
				case 1:
					itemPrefix = "hieroglyphic ";
					break;
				case 2:
					itemPrefix = "smooth ";
					break;
			}
			break;
		case 35: // Wool
			itemPrefix = DyeColor.values()[data].toString() + " ";
			break;
		case 43: // Doubleslab
		case 44: // Slab
			itemPrefix = SlabMaterial.values()[data].toString() + " ";
			break;
		case 98:
			itemPrefix = StoneBrickType.values()[data].toString() + " ";
			break;
		case 263: // Coal
			if (data > 0) {
				itemPrefix = "char";
			}
			break;
		case 351: // Ink
			// Data changes whole item, so don't use colorPrefix.
			return InkName.values()[data].toString().toLowerCase().replace('_', ' ');
		case 373: // Potion
			if (damage >= 16384) {
				itemPrefix = "splash ";
				damage -= 8192;
			}
			switch(damage) {
				case 0:
					return itemPrefix + "water bottle";
				case 16:
					return itemPrefix + "awkward potion";
				case 32:
					return itemPrefix + "thick potion";
				case 64:
					return itemPrefix + "mundane potion (extended)";
				case 8192:
					return itemPrefix + "mundane potion";
				case 8193:
					return itemPrefix + "potion of regeneration (short)";
				case 8257:
					return itemPrefix + "potion of regeneration (long)";
				case 8225:
					return itemPrefix + "potion of regeneration ii";
				case 8194:
					return itemPrefix + "potion of swiftness (short)";
				case 8258:
					return itemPrefix + "potion of swiftness (long)";
				case 8226:
					return itemPrefix + "potion of swiftness ii";
				case 8195:
					return itemPrefix + "potion of fire resistance (short)";
				case 8259:
					return itemPrefix + "potion of fire resistance (long)";
				case 8197:
					return itemPrefix + "potion of healing";
				case 8229:
					return itemPrefix + "potion of healing ii";
				case 8201:
					return itemPrefix + "potion of strength (short)";
				case 8265:
					return itemPrefix + "potion of strength (long)";
				case 8233:
					return itemPrefix + "potion of strength ii";
				case 8296:
					return itemPrefix + "potion of poison (short)";
				case 8260:
					return itemPrefix + "potion of poison (long)";
				case 8228:
					return itemPrefix + "potion of poison ii";
				case 8200:
					return itemPrefix + "potion of weakness (short)";
				case 8264:
					return itemPrefix + "potion of weakness (long)";
				case 8202:
					return itemPrefix + "potion of slowness (short)";
				case 8266:
					return itemPrefix + "potion of slowness (long)";
				case 8204:
					return itemPrefix + "potion of harming";
				case 8236:
					return itemPrefix + "potion of harming ii";
				case 8289:
					return itemPrefix + "potion of regeneration ii";
				case 8290:
					return itemPrefix + "potion of swiftness ii";
				case 8297:
					return itemPrefix + "potion of strength ii";
				case 8292:
					return itemPrefix + "potion of poison ii";
			}
			return "unknown potion (dv:" + Short.toString(damage) + ")";
		case 383:
			switch(data) {
			case 50:
				itemPrefix = "creeper ";
				break;
			case 51:
				itemPrefix = "skeleton ";
				break;
			case 52:
				itemPrefix = "spider ";
				break;
			case 53:
				itemPrefix = "giant ";
				break;
			case 54:
				itemPrefix = "zombie ";
				break;
			case 55:
				itemPrefix = "slime ";
				break;
			case 56:
				itemPrefix = "ghast ";
				break;
			case 57:
				itemPrefix = "zombie pigman ";
				break;
			case 58:
				itemPrefix = "enderman ";
				break;
			case 59:
				itemPrefix = "cave spider ";
				break;
			case 60:
				itemPrefix = "silverfish ";
				break;
			case 61:
				itemPrefix = "blaze ";
				break;
			case 62:
				itemPrefix = "magma cube ";
				break;
			case 63:
				itemPrefix = "enderdragon ";
				break;
			case 90:
				itemPrefix = "pig ";
				break;
			case 91:
				itemPrefix = "sheep ";
				break;
			case 92:
				itemPrefix = "cow ";
				break;
			case 93:
				itemPrefix = "chicken ";
				break;
			case 94:
				itemPrefix = "squid ";
				break;
			case 95:
				itemPrefix = "wolf ";
				break;
			case 96:
				itemPrefix = "mooshroom ";
				break;
			case 97:
				itemPrefix = "snow golem ";
				break;
			case 120:
				itemPrefix = "villager ";
				break;
			case 98:
				itemPrefix = "ocelot ";
				break;
			default:
				itemPrefix = "unknown ";
			}
			itemName = "spawn egg";
			break;
		
		// 1.2 Items, should only be temporary, but just in case bukkit doesn't update quite as quick
		case 385:
			itemName = "fire charge";
			break;
		case 384:
			itemName = "bottle o' enchanting";
			break;

		default:
//			ItemStack stack = new ItemStack(id, 1,
//					Short.parseShort("0"),data);
//			return stack.getType().toString().toLowerCase().replace('_', ' ');
		}
		return (itemPrefix + itemName).toLowerCase().replace('_', ' ');
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
