package com.flobi.utility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.milkbowl.vault.item.ItemInfo;
import net.milkbowl.vault.item.Items;
import net.minecraft.server.NBTTagCompound;
import net.minecraft.server.NBTTagList;
import net.minecraft.server.NBTTagString;

import org.bukkit.Material;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.flobi.WhatIsIt.WhatIsIt;
import com.flobi.floAuction.floAuction;


public class items {
	private static Map<Integer, String> enchantmentNames = null;
	private static Map<Integer, String> enchantmentLevels = null;
	
	public static String getHeadOwner(CraftItemStack item) {
		if (item == null) return null;
		if (item.getHandle().getTag() == null) return null;
		if (!item.getHandle().getTag().hasKey("SkullOwner")) return null;
		return item.getHandle().getTag().getString("SkullOwner");
	}
	
	public static void setHeadOwner(CraftItemStack item, String headName) {
		if (item == null) return;
		if (headName == null || headName.isEmpty()) {
			if (item.getHandle().getTag() == null) return;
			item.getHandle().getTag().remove("SkullOwner");
		} else {
			if (item.getHandle().getTag() == null) item.getHandle().setTag(new NBTTagCompound());
			item.getHandle().getTag().setString("SkullOwner", headName);
		}
	}
	
	public static Integer getRepairCost(CraftItemStack item) {
		if (item == null) return null;
		if (item.getHandle().getTag() == null) return null;
		if (!item.getHandle().getTag().hasKey("RepairCost")) return null;
		return (Integer)item.getHandle().getTag().getInt("RepairCost");
	}
	
	public static void setRepairCost(CraftItemStack item, Integer repairCost) {
		if (item == null) return;
		if (repairCost == null) {
			if (item.getHandle().getTag() == null) return;
			item.getHandle().getTag().remove("RepairCost");
		} else {
			if (item.getHandle().getTag() == null) item.getHandle().setTag(new NBTTagCompound());
			item.getHandle().getTag().setInt("RepairCost", repairCost);
		}
	}
	
	public static String getDisplayName(CraftItemStack item) {
		if (item == null) return null;
		if (item.getHandle().getTag() == null) return null;
		if (!item.getHandle().getTag().hasKey("display")) return null;
		if (!item.getHandle().getTag().getCompound("display").hasKey("Name")) return null;
		return item.getHandle().getTag().getCompound("display").getString("Name");
	}
	
	public static void setDisplayName(CraftItemStack item, String name) {
		if (item == null) return;
		if (name == null) {
			if (item.getHandle().getTag() == null) return;
			if (!item.getHandle().getTag().hasKey("display")) return;
			item.getHandle().getTag().getCompound("display").remove("Name");
		} else {
			if (item.getHandle().getTag() == null) item.getHandle().setTag(new NBTTagCompound());
			if (!item.getHandle().getTag().hasKey("display")) item.getHandle().getTag().setCompound("display", new NBTTagCompound());
			item.getHandle().getTag().getCompound("display").setString("Name", name);
		}
	}
	
	public static String getBookAuthor(CraftItemStack book) {
		if (book == null) return "";
		if (!(book.getType() == Material.WRITTEN_BOOK)) return "";
		if (book.getHandle().getTag() == null) return "";
		if (!book.getHandle().getTag().hasKey("author")) return "";
		return book.getHandle().getTag().getString("author");
	}
	
	public static void setBookAuthor(CraftItemStack book, String author) {
		if (book == null) return;
		if (!(book.getType() == Material.WRITTEN_BOOK)) return;
		if (book.getHandle().getTag() == null) book.getHandle().setTag(new NBTTagCompound());
		book.getHandle().getTag().setString("author", author);
	}
	
	public static String getBookTitle(CraftItemStack book) {
		if (book == null) return "";
		if (!(book.getType() == Material.WRITTEN_BOOK)) return "";
		if (book.getHandle().getTag() == null) book.getHandle().setTag(new NBTTagCompound());
		if (!book.getHandle().getTag().hasKey("title")) return "";
		return book.getHandle().getTag().getString("title");
	}
	
	public static void setBookTitle(CraftItemStack book, String title) {
		if (book == null) return;
		if (!(book.getType() == Material.WRITTEN_BOOK)) return;
		if (book.getHandle().getTag() == null) book.getHandle().setTag(new NBTTagCompound());
		book.getHandle().getTag().setString("title", title);
	}
	
	public static String[] getBookPages(CraftItemStack book) {
		if (book == null) return new String[0];
		if (!(book.getType() == Material.WRITTEN_BOOK || book.getType() == Material.BOOK_AND_QUILL)) return null;
		if (book.getHandle().getTag() == null) book.getHandle().setTag(new NBTTagCompound());
		if (!book.getHandle().getTag().hasKey("pages")) return null;
		
		NBTTagList tagList = book.getHandle().getTag().getList("pages");
		
		String[] pages = new String[tagList.size()];
		for(int i = 0; i < tagList.size(); i++){
			pages[i] = ((NBTTagString)tagList.get(i)).data;
		}
		
		return pages;
	}
	
	public static void setBookPages(CraftItemStack book, String[] pages) {
		if (book == null) return;
		if (!(book.getType() == Material.WRITTEN_BOOK || book.getType() == Material.BOOK_AND_QUILL)) return;
		if (pages == null) return;
		
		NBTTagList nPages = new NBTTagList();
		
        for (int i = 0; i < pages.length; i++) {
            nPages.add(new NBTTagString(pages[i],pages[i]));
        }
		if (book.getHandle().getTag() == null) book.getHandle().setTag(new NBTTagCompound());
        book.getHandle().getTag().set("pages", nPages);
    }
	
	// Some of this was taken from Vault's item classes.
	public static boolean isSameItem(ItemStack item, String searchString) {
		
        if (searchString.matches("\\d+;\\d+")) {
            // Match on integer:short to get typeId and subTypeId

            // Retrieve/parse data
            String[] params = searchString.split(";");
            int typeId = Integer.parseInt(params[0]);
            short subTypeId = Short.parseShort(params[1]);
            
            if (item.getTypeId() == typeId && item.getDurability() == subTypeId) {
            	return true;
            }

        } else if (searchString.matches("\\d+")) {
            // Match an integer only, assume subTypeId = 0

            // Retrieve/parse data
            int typeId = Integer.parseInt(searchString);

            if (item.getTypeId() == typeId) {
            	return true;
            }
        }
		return false;
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
		
		// These were added in 1.4.
		if (!isSame(getHeadOwner((CraftItemStack)item1), getHeadOwner((CraftItemStack)item2))) return false;
		if (!isSame(getRepairCost((CraftItemStack)item1), getRepairCost((CraftItemStack)item2))) return false;
		if (!isSame(getDisplayName((CraftItemStack)item1), getDisplayName((CraftItemStack)item2))) return false;

		// Book author, title and contents must be identical.
		if (!getBookAuthor((CraftItemStack)item1).equals(getBookAuthor((CraftItemStack)item2))) return false;
		if (!getBookTitle((CraftItemStack)item1).equals(getBookTitle((CraftItemStack)item2))) return false;
		String[] pages1 = getBookPages((CraftItemStack)item1);
		String[] pages2 = getBookPages((CraftItemStack)item2);
		if ((pages1 == null) ^ (pages2 == null)) return false;
		if (pages1 != null) {
			if (pages1.length != pages2.length) return false;
			for (int i = 0; i < pages1.length; i++) {
	            if (!pages1[i].equals(pages2[i])) return false;
	        }
		}

		return true;
	}
	private static boolean isSame(String str1, String str2) {
		if (str1 == null && str2 == null) return true;
		if (str1 == null) return false;
		return str1.equals(str2); 
	}
	private static boolean isSame(Integer int1, Integer int2) {
		if (int1 == null && int2 == null) return true;
		if (int1 == null) return false;
		return int1.equals(int2); 
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

	public static boolean hasAmount(String ownerName, int amount, ItemStack compareItem){
		int has = getAmount(ownerName, compareItem);
		if (has >= amount) {
			return true;
		} else {
			return false;
		}
	}
	public static int getAmount(String ownerName, ItemStack compareItem) {
		if (floAuction.server.getPlayer(ownerName) == null) return 0;
		PlayerInventory inventory = floAuction.server.getPlayer(ownerName).getInventory();
		ItemStack[] items = inventory.getContents();
		int has = 0;
		for (ItemStack item : items) {
			if (isSameItem(compareItem, item)) {
				has += item.getAmount();
			}
		}
		return has;
	}
	public static void remove(String playerName, int amount, ItemStack compareItem){
		Player player = floAuction.server.getPlayer(playerName);
		if (player != null) {
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
	
	public static String getItemName(ItemStack typeStack) {
		if (floAuction.useWhatIsIt) {
			return WhatIsIt.itemName(typeStack);
		} else {
			ItemInfo itemInfo = Items.itemByStack(typeStack);
			if (itemInfo == null) {
				return typeStack.getType().name();
			} else {
				return itemInfo.getName();
			}
		}
	}
	
	public static String getEnchantmentName(Entry<Enchantment, Integer> enchantment) {
		if (floAuction.useWhatIsIt) {
			return WhatIsIt.enchantmentName(enchantment);
		} else {
			int enchantmentId = enchantment.getKey().getId();
			int enchantmentLevel = enchantment.getValue();
			String enchantmentName = null;
			if (enchantmentNames == null) {
				enchantmentNames = new HashMap<Integer, String>();
				enchantmentNames.put(0, "Protection");
				enchantmentNames.put(1, "Fire Protection");
				enchantmentNames.put(2, "Feather Falling");
				enchantmentNames.put(3, "Blast Protection");
				enchantmentNames.put(4, "Projectile Protection");
				enchantmentNames.put(5, "Respiration");
				enchantmentNames.put(6, "Aqua Afinity");
				enchantmentNames.put(16, "Sharpness");
				enchantmentNames.put(17, "Smite");
				enchantmentNames.put(18, "Bane of Arthropods");
				enchantmentNames.put(19, "Knockback");
				enchantmentNames.put(20, "Fire Aspect");
				enchantmentNames.put(21, "Looting");
				enchantmentNames.put(32, "Efficiency");
				enchantmentNames.put(33, "Silk Touch");
				enchantmentNames.put(34, "Unbreaking");
				enchantmentNames.put(35, "Fortune");
				enchantmentNames.put(48, "Power");
				enchantmentNames.put(49, "Punch");
				enchantmentNames.put(50, "Flame");
				enchantmentNames.put(51, "Infinity");
			}
			if (enchantmentNames.get(enchantmentId) != null) {
				enchantmentName = enchantmentNames.get(enchantmentId) + " ";
			} else {
				enchantmentName = "UNKNOWN ";
			}
			if (enchantmentLevels == null) {
				enchantmentLevels = new HashMap<Integer, String>();
				enchantmentLevels.put(0, "");
				enchantmentLevels.put(1, "I");
				enchantmentLevels.put(2, "II");
				enchantmentLevels.put(3, "III");
				enchantmentLevels.put(4, "IV");
				enchantmentLevels.put(5, "V");
			}
			if (enchantmentLevels.get(enchantmentLevel) != null) {
				enchantmentName = enchantmentLevels.get(enchantmentLevel) + " ";
			} else {
				enchantmentName += enchantmentLevel;
			}
			return enchantmentName;
		}
	}
	
}
