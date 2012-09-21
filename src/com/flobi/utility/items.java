package com.flobi.utility;

import java.util.ArrayList;

import net.minecraft.server.NBTTagCompound;
import net.minecraft.server.NBTTagList;
import net.minecraft.server.NBTTagString;

import org.bukkit.Material;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.flobi.floAuction.floAuction;


public class items {
	
	public static String getBookAuthor(CraftItemStack book) {
		if (!(book.getType() == Material.WRITTEN_BOOK)) return "";
		if (book.getHandle().getTag() == null) book.getHandle().setTag(new NBTTagCompound());
		if (!book.getHandle().getTag().hasKey("author")) return "";
		return book.getHandle().getTag().getString("author");
	}
	
	public static void setBookAuthor(CraftItemStack book, String author) {
		if (!(book.getType() == Material.WRITTEN_BOOK)) return;
		if (book.getHandle().getTag() == null) book.getHandle().setTag(new NBTTagCompound());
		book.getHandle().getTag().setString("author", author);
	}
	
	public static String getBookTitle(CraftItemStack book) {
		if (!(book.getType() == Material.WRITTEN_BOOK)) return "";
		if (book.getHandle().getTag() == null) book.getHandle().setTag(new NBTTagCompound());
		if (!book.getHandle().getTag().hasKey("title")) return "";
		return book.getHandle().getTag().getString("title");
	}
	
	public static void setBookTitle(CraftItemStack book, String title) {
		if (!(book.getType() == Material.WRITTEN_BOOK)) return;
		if (book.getHandle().getTag() == null) book.getHandle().setTag(new NBTTagCompound());
		book.getHandle().getTag().setString("title", title);
	}
	
	public static String[] getBookPages(CraftItemStack book) {
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
	
}
