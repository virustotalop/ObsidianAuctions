package com.gmail.virustotalop.obsidianauctions.util;

import org.bukkit.Bukkit;
import org.bukkit.FireworkEffect;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentWrapper;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.FireworkEffectMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Repairable;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class Items {

    private static Map<Integer, String> enchantmentNames = new HashMap<>();
    private static Map<Integer, String> enchantmentLevels = new HashMap<>();

    private static int firstPartial(ItemStack item, ItemStack[] inventory) {
        if(item == null) {
            return -1;
        }
        for(int i = 0; i < inventory.length; i++) {
            ItemStack cItem = inventory[i];
            if(cItem != null && cItem.getAmount() < cItem.getMaxStackSize() && isSameItem(item, cItem)) {
                return i;
            }
        }
        return -1;
    }

    private static int firstEmpty(ItemStack[] inventory) {
        for(int i = 0; i < inventory.length; i++) {
            if(inventory[i] == null) {
                return i;
            }
        }
        return -1;
    }

    // Most of this function was copied from CraftBukkit.  The above functions too.
    public static void saferItemGive(PlayerInventory playerInventory, ItemStack item) {
        // After bukkit is fixed:
        // player.getInventory().addItem(givingItems);

        while(true) {
            // Do we already have a stack of it?
            int firstPartial = firstPartial(item, playerInventory.getStorageContents());

            // Drat! no partial stack
            if(firstPartial == -1) {
                // Find a free spot!
                int firstFree = firstEmpty(playerInventory.getStorageContents());

                if(firstFree == -1) {
                    // No space at all!
                    // Bukkit returns unplaced items, but floAuction only calls this after checking for space.
                    break;
                } else {
                    // Again, floAuction checks for this elsewhere before calling this...technically this would never occur, no reason to code for it.

                    // More than a single stack!
/*                    if (item.getAmount() > getMaxStackSize(item)) {
                        CraftItemStack stack = new CraftItemStack(item.getTypeId(), getMaxStackSize(item), item.getDurability());
                        stack.addUnsafeEnchantments(item.getEnchantments());
                        playerInventory.setItem(firstFree, stack);
                        item.setAmount(item.getAmount() - getMaxStackSize(item));
                    } else {*/
                    // Just store it
                    playerInventory.setItem(firstFree, item);
                    break;
//                    }
                }
            } else {
                // So, apparently it might only partially fit, well lets do just that
                ItemStack partialItem = playerInventory.getItem(firstPartial);

                int amount = item.getAmount();
                int partialAmount = partialItem.getAmount();
                int maxAmount = partialItem.getMaxStackSize();

                // Check if it fully fits
                if(amount + partialAmount <= maxAmount) {
                    partialItem.setAmount(amount + partialAmount);
                    break;
                }

                // It fits partially
                partialItem.setAmount(maxAmount);
                item.setAmount(amount + partialAmount - maxAmount);
            }
        }

    }

    public static String[] getLore(ItemStack item) {
        if(item == null) return null;
        ItemMeta itemMeta = item.getItemMeta();
        if(itemMeta == null) return null;
        List<String> pageList = itemMeta.getLore();
        if(pageList == null) return null;
        String[] pages = new String[pageList.size()];
        for(int i = 0; i < pageList.size(); i++) {
            pages[i] = pageList.get(i);
        }
        return pages;
    }

    public static void setLore(ItemStack item, String[] pages) {
        if(item == null || pages == null) return;
        ItemMeta itemMeta = item.getItemMeta();
        if(itemMeta == null) return;
        List<String> pageList = new ArrayList<String>();
        for(int i = 0; i < pages.length; i++) {
            pageList.add(pages[i]);
        }
        itemMeta.setLore(pageList);
        item.setItemMeta(itemMeta);
    }

    public static Map<Enchantment, Integer> getStoredEnchantments(ItemStack item) {
        if(item == null) {
            return null;
        }
        ItemMeta itemMeta = item.getItemMeta();
        if(itemMeta == null) {
            return null;
        }
        if(itemMeta instanceof EnchantmentStorageMeta) {
            return ((EnchantmentStorageMeta) itemMeta).getStoredEnchants();
        }
        return null;
    }

    public static void addStoredEnchantment(ItemStack item, Integer enchantment, Integer level, boolean ignoreLevelRestriction) {
        if(item == null) {
            return;
        }
        ItemMeta itemMeta = item.getItemMeta();
        if(itemMeta == null) {
            return;
        }
        if(itemMeta instanceof EnchantmentStorageMeta) {
            EnchantmentStorageMeta storageMeta = (EnchantmentStorageMeta) itemMeta;
            storageMeta.addStoredEnchant(new EnchantmentWrapper(enchantment), level, ignoreLevelRestriction);
            item.setItemMeta(storageMeta);
        }
    }

    public static Integer getFireworkPower(ItemStack item) {
        if(item == null) {
            return null;
        }
        ItemMeta itemMeta = item.getItemMeta();
        if(itemMeta == null) {
            return null;
        } else if(itemMeta instanceof FireworkMeta) {
            return ((FireworkMeta) itemMeta).getPower();
        }
        return null;
    }

    public static void setFireworkPower(ItemStack item, Integer power) {
        if(item == null) {
            return;
        }
        ItemMeta itemMeta = item.getItemMeta();
        if(itemMeta == null) {
            return;
        } else if(itemMeta instanceof FireworkMeta) {
            FireworkMeta fireworkMeta = ((FireworkMeta) itemMeta);
            fireworkMeta.setPower(power);
            item.setItemMeta(fireworkMeta);
        }
    }

    public static FireworkEffect[] getFireworkEffects(ItemStack item) {
        if(item == null) {
            return null;
        }
        ItemMeta itemMeta = item.getItemMeta();
        if(itemMeta == null) {
            return null;
        } else if(itemMeta instanceof FireworkMeta) {
            List<FireworkEffect> effectList = ((FireworkMeta) itemMeta).getEffects();
            FireworkEffect[] effects = new FireworkEffect[effectList.size()];
            for(int i = 0; i < effectList.size(); i++) {
                effects[i] = effectList.get(i);
            }
            return effects;
        } else if(itemMeta instanceof FireworkEffectMeta) {
            FireworkEffect[] effects = new FireworkEffect[1];
            effects[0] = ((FireworkEffectMeta) itemMeta).getEffect();
            return effects;
        }
        return null;
    }

    public static void setFireworkEffects(ItemStack item, FireworkEffect[] effects) {
        if(item == null || effects == null) {
            return;
        }
        ItemMeta itemMeta = item.getItemMeta();
        if(itemMeta == null) {
            return;
        } else if(itemMeta instanceof FireworkMeta) {
            FireworkMeta fireworkMeta = ((FireworkMeta) itemMeta);
            fireworkMeta.addEffects(effects);
            item.setItemMeta(fireworkMeta);
        } else if(itemMeta instanceof FireworkEffectMeta) {
            if(effects.length > 0) {
                FireworkEffectMeta fireworkEffectMeta = ((FireworkEffectMeta) itemMeta);
                fireworkEffectMeta.setEffect(effects[0]);
                item.setItemMeta(fireworkEffectMeta);
            }
        }
    }

    public static Object getNbtTag(ItemStack item) {
        Object tag = null;
        try {
            Class<?> craftItemStack = Class.forName("org.bukkit.craftbukkit." + VersionUtil.getVersion() + ".inventory.CraftItemStack");
            Method asCraftCopy = craftItemStack.getMethod("asCraftCopy", ItemStack.class);
            Method asNMSCopy = craftItemStack.getMethod("asNMSCopy", ItemStack.class);
            Object craftCopy = asCraftCopy.invoke(null, item);
            Object itemStack = asNMSCopy.invoke(null, craftCopy);
            Method tagField = itemStack.getClass().getMethod("getTag");
            tag = tagField.invoke(itemStack);
        } catch(ClassNotFoundException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
            e.printStackTrace();
        }
        return tag;
    }

    public static String getHeadOwner(ItemStack item) {
        if(item == null) {
            return null;
        }
        ItemMeta itemMeta = item.getItemMeta();
        if(itemMeta == null) {
            return null;
        }
        if(itemMeta instanceof SkullMeta) {
            return ((SkullMeta) itemMeta).getOwner();
        }
        return null;
    }

    public static void setHeadOwner(ItemStack item, String headName) {
        if(item == null) {
            return;
        }
        ItemMeta itemMeta = item.getItemMeta();
        if(itemMeta == null) {
            return;
        } else if(itemMeta instanceof SkullMeta) {
            SkullMeta skullMeta = ((SkullMeta) itemMeta);
            skullMeta.setOwner(headName);
            item.setItemMeta(skullMeta);
        }
    }

    public static Integer getRepairCost(ItemStack item) {
        if(item == null) {
            return null;
        }
        ItemMeta itemMeta = item.getItemMeta();
        if(itemMeta == null) {
            return null;
        } else if(itemMeta instanceof Repairable) {
            return ((Repairable) itemMeta).getRepairCost();
        }
        return null;
    }

    public static void setRepairCost(ItemStack item, Integer repairCost) {
        if(item == null) return;
        ItemMeta itemMeta = item.getItemMeta();
        if(itemMeta == null) return;
        if(itemMeta instanceof Repairable) {
            Repairable repairable = ((Repairable) itemMeta);
            repairable.setRepairCost(repairCost);
            item.setItemMeta((ItemMeta) repairable);
        }
        return;
    }

    public static String getDisplayName(ItemStack item) {
        if(item == null) return null;
        ItemMeta itemMeta = item.getItemMeta();
        if(itemMeta == null) return null;
        return itemMeta.getDisplayName();
    }

    public static void setDisplayName(ItemStack item, String name) {
        if(item == null) return;
        if(name == null) return;
        ItemMeta itemMeta = item.getItemMeta();
        if(itemMeta == null) return;
        itemMeta.setDisplayName(name);
        item.setItemMeta(itemMeta);
    }

    public static String getBookAuthor(ItemStack book) {
        if(book == null) return null;
        ItemMeta itemMeta = book.getItemMeta();
        if(itemMeta == null) return null;
        if(itemMeta instanceof BookMeta) {
            return ((BookMeta) itemMeta).getAuthor();
        }
        return null;
    }

    public static void setBookAuthor(ItemStack book, String author) {
        if(book == null) return;
        ItemMeta itemMeta = book.getItemMeta();
        if(itemMeta == null) return;
        if(itemMeta instanceof BookMeta) {
            BookMeta bookMeta = ((BookMeta) itemMeta);
            bookMeta.setAuthor(author);
            book.setItemMeta(bookMeta);
        }
        return;
    }

    public static String getBookTitle(ItemStack book) {
        if(book == null) return null;
        ItemMeta itemMeta = book.getItemMeta();
        if(itemMeta == null) return null;
        if(itemMeta instanceof BookMeta) {
            return ((BookMeta) itemMeta).getTitle();
        }
        return null;
    }

    public static void setBookTitle(ItemStack book, String title) {
        if(book == null) return;
        ItemMeta itemMeta = book.getItemMeta();
        if(itemMeta == null) return;
        if(itemMeta instanceof BookMeta) {
            BookMeta bookMeta = ((BookMeta) itemMeta);
            bookMeta.setTitle(title);
            book.setItemMeta(bookMeta);
        }
        return;
    }

    public static String[] getBookPages(ItemStack book) {
        if(book == null) return null;
        ItemMeta itemMeta = book.getItemMeta();
        if(itemMeta == null) return null;
        if(itemMeta instanceof BookMeta) {
            List<String> pageList = ((BookMeta) itemMeta).getPages();
            String[] pages = new String[pageList.size()];
            for(int i = 0; i < pageList.size(); i++) {
                pages[i] = pageList.get(i);
            }
            return pages;
        }
        return null;
    }

    public static void setBookPages(ItemStack book, String[] pages) {
        if(book == null || pages == null) return;
        ItemMeta itemMeta = book.getItemMeta();
        if(itemMeta == null) return;
        if(itemMeta instanceof BookMeta) {
            BookMeta bookMeta = ((BookMeta) itemMeta);
            bookMeta.setPages(pages);
            book.setItemMeta(bookMeta);
        }
    }

    // Some of this was taken from Vault's item classes.
    public static boolean isSameItem(ItemStack item, String searchString) {
        Material mat;
        short damageId = 0;
        if(searchString.contains(",")) {
            String[] split = searchString.split(",");
            mat = Material.valueOf(split[0]);
            damageId = Short.parseShort(split[1]);
        } else {
            mat = Material.valueOf(searchString);
        }
        if(damageId != 0) {
            return isSameItem(item, new ItemStack(mat, 1, damageId));
        }
        return isSameItem(item, new ItemStack(mat, 1));
    }

    public static boolean isSameItem(ItemStack item1, ItemStack item2) {
        return item1.isSimilar(item2);
    }

    private static boolean isSame(String[] strings1, String[] strings2) {
        if(strings1 == null && strings2 == null) return true;
        if(strings1 == null || strings2 == null) return false;
        if(strings1.length != strings2.length) return false;
        for(int i = 0; i < strings1.length; i++) {
            if(!isSame(strings1[i], strings2[i])) return false;
        }
        return true;
    }

    private static boolean isSame(Map<Enchantment, Integer> storedEnchantments1, Map<Enchantment, Integer> storedEnchantments2) {
        if(storedEnchantments1 == null && storedEnchantments2 == null) return true;
        if(storedEnchantments1 == null || storedEnchantments2 == null) return false;
        return storedEnchantments1.equals(storedEnchantments2);
    }

    private static boolean isSame(String str1, String str2) {
        if(str1 == null && str2 == null) return true;
        if(str1 == null || str2 == null) return false;
        return str1.equals(str2);
    }

    private static boolean isSame(Integer int1, Integer int2) {
        if(int1 == null && int2 == null) return true;
        if(int1 == null || int2 == null) return false;
        return int1.equals(int2);
    }

    private static boolean isSame(FireworkEffect[] effects1, FireworkEffect[] effects2) {
        if(effects1 == null && effects2 == null) return true;
        if(effects1 == null || effects2 == null) return false;
        if(effects1.length != effects2.length) return false;
        for(int i = 0; i < effects1.length; i++) {
            if(!isSame(effects1[i].hashCode(), effects2[i].hashCode())) return false;
        }
        return true;
    }

    public static int getMaxStackSize(ItemStack item) {
        if(item == null)
            return 0;

        int maxStackSize = item.getType().getMaxStackSize();
        // If bukkit has any bad stack sized, override them now.

        return maxStackSize;
    }

    public static boolean isStackable(int id) {
        return getMaxStackSize(new ItemStack(id)) > 1;
    }

    public static int getSpaceForItem(Player player, ItemStack item) {
        int maxstack = getMaxStackSize(item);
        int space = 0;

        ItemStack[] items = player.getInventory().getStorageContents();
        for(ItemStack current : items) {
            if(current == null) {
                space += maxstack;
                continue;
            }
            if(isSameItem(item, current)) {
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

        for(ItemStack item : lot) {
            if(item != null) {
                needed += item.getAmount();
            }
        }
        return hasSpace(player, needed, lot.get(0));

    }

    public static boolean hasAmount(String ownerName, int amount, ItemStack compareItem) {
        int has = getAmount(ownerName, compareItem);
        return has >= amount;
    }

    public static int getAmount(String ownerName, ItemStack compareItem) {
        if(Bukkit.getPlayer(ownerName) == null) return 0;
        PlayerInventory inventory = Bukkit.getPlayer(ownerName).getInventory();
        ItemStack[] items = inventory.getStorageContents();
        int has = 0;
        for(ItemStack item : items) {
            if(isSameItem(compareItem, item)) {
                has += item.getAmount();
            }
        }
        return has;
    }

    public static void remove(String playerName, int amount, ItemStack compareItem) {
        Player player = Bukkit.getPlayer(playerName);
        if(player != null) {
            PlayerInventory inventory = player.getInventory();

            // Remove held item first:
            if(isSameItem(compareItem, player.getItemInHand())) {
                int heldAmount = player.getItemInHand().getAmount();
                if(heldAmount <= amount) {
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
            for(int invIndex = 0; invIndex < inventory.getSize(); invIndex++) {
                ItemStack current = inventory.getItem(invIndex);

                if(current == null || current.getAmount() <= 0)
                    continue;

                if(!isSameItem(compareItem, current))
                    continue;

                if(current.getAmount() > counter) {
                    leftover = current.getAmount() - counter;
                }

                if(leftover != 0) {
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
        for(Enchantment ench : Enchantment.values()) {
            if(ench.canEnchantItem(item)) {
                return true;
            }
        }
        return false;
    }

    public static String getItemName(ItemStack itemStack) {
        Class<?> craftItemStack = itemStack.getClass();
        try {
            Field handleField = craftItemStack.getDeclaredField("handle");
            handleField.setAccessible(true);
            Object nmsItemStack = handleField.get(itemStack);
            Method getItem = nmsItemStack.getClass().getMethod("getItem");
            Object item = getItem.invoke(nmsItemStack);
            Class<?> itemClass = Class.forName("net.minecraft.server." + VersionUtil.getVersion() + ".Item");
            Method getName = itemClass.getDeclaredMethod("getName");
            String name = (String) getName.invoke(item);
            if(!name.startsWith("item.minecraft")) { //For legacy support before proper keying
                name += ".name";
            }
            return name;
        } catch(NoSuchFieldException | IllegalAccessException | NoSuchMethodException | InvocationTargetException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getEnchantmentName(Entry<Enchantment, Integer> enchantment) {
        int enchantmentId = enchantment.getKey().getId();
        int enchantmentLevel = enchantment.getValue();
        String enchantmentName = null;
        if(enchantmentNames.size() == 0) {
            enchantmentNames = new HashMap<>();
            enchantmentNames.put(0, "Protection");
            enchantmentNames.put(1, "Fire Protection");
            enchantmentNames.put(2, "Feather Falling");
            enchantmentNames.put(3, "Blast Protection");
            enchantmentNames.put(4, "Projectile Protection");
            enchantmentNames.put(5, "Respiration");
            enchantmentNames.put(6, "Aqua Afinity");
            enchantmentNames.put(8, "Depth Strider");
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
            enchantmentNames.put(61, "Luck of the Sea");
            enchantmentNames.put(62, "Lure");
            enchantmentNames.put(70, "Mending");
        }
        if(enchantmentNames.get(enchantmentId) != null) {
            enchantmentName = enchantmentNames.get(enchantmentId) + " ";
        } else {
            enchantmentName = "UNKNOWN ";
        }
        if(enchantmentLevels.size() == 0) {
            enchantmentLevels = new HashMap<Integer, String>();
            enchantmentLevels.put(0, "");
            enchantmentLevels.put(1, "I");
            enchantmentLevels.put(2, "II");
            enchantmentLevels.put(3, "III");
            enchantmentLevels.put(4, "IV");
            enchantmentLevels.put(5, "V");
        }
        if(enchantmentLevels.get(enchantmentLevel) != null) {
            enchantmentName += enchantmentLevels.get(enchantmentLevel) + " ";
        } else {
            enchantmentName += enchantmentLevel;
        }
        return enchantmentName;
    }
    //}

}
