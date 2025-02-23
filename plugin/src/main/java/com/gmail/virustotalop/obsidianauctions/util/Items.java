/*
 *     ObsidianAuctions
 *     Copyright (C) 2012-2022 flobi and contributors
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.gmail.virustotalop.obsidianauctions.util;

import org.bukkit.Bukkit;
import org.bukkit.FireworkEffect;
import org.bukkit.Material;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.FireworkEffectMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Repairable;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

@ApiStatus.Internal
@ApiStatus.NonExtendable
public final class Items {

    private static final String ITEM_STACK_PATH = "itemstack";
    private static final Map<Enchantment, String> enchantmentNames = new HashMap<>();
    private static final Map<Integer, String> enchantmentLevels = new HashMap<>();

    static {
        registerEnchantment("PROTECTION_ENVIRONMENTAL", "Protection");
        registerEnchantment("PROTECTION_FIRE", "Fire Protection");
        registerEnchantment("PROTECTION_FALL", "Feather Falling");
        registerEnchantment("PROTECTION_EXPLOSIONS", "Blast Protection");
        registerEnchantment("PROTECTION_PROJECTILE", "Projectile Protection");
        registerEnchantment("OXYGEN", "Respiration");
        registerEnchantment("WATER_WORKER", "Aqua Afinity");
        registerEnchantment("DEPTH_STRIDER", "Depth Strider");
        registerEnchantment("BINDING_CURSE", "Curse of Bindings");
        registerEnchantment("DAMAGE_ALL", "Sharpness");
        registerEnchantment("DAMAGE_UNDEAD", "Smite");
        registerEnchantment("DAMAGE_ARTHROPODS", "Bane of Arthropods");
        registerEnchantment("LOOT_BONUS_MOBS", "Looting");
        registerEnchantment("DIG_SPEED", "Efficiency");
        registerEnchantment("DURABILITY", "Unbreaking");
        registerEnchantment("LOOT_BONUS_BLOCKS", "Fortune");
        registerEnchantment("ARROW_DAMAGE", "Power");
        registerEnchantment("ARROW_KNOCKBACK", "Punch");
        registerEnchantment("ARROW_FIRE", "Flame");
        registerEnchantment("ARROW_INFINITE", "Infinity");
        registerEnchantment("LUCK", "Luck of the Sea");
        registerEnchantment("VANISHING_CURSE", "Curse of Vanishing");

        enchantmentLevels.put(0, "");
        enchantmentLevels.put(1, "I");
        enchantmentLevels.put(2, "II");
        enchantmentLevels.put(3, "III");
        enchantmentLevels.put(4, "IV");
        enchantmentLevels.put(5, "V");
    }

    private static void registerEnchantment(String enchantmentName, String alias) {
        Enchantment enchantment = Enchantment.getByName(enchantmentName);
        if (enchantment == null) {
            return;
        }
        enchantmentNames.put(enchantment, alias);
    }

    private static int firstPartial(ItemStack item, ItemStack[] inventory) {
        if (item == null) {
            return -1;
        }
        for (int i = 0; i < inventory.length; i++) {
            ItemStack cItem = inventory[i];
            if (cItem != null && cItem.getAmount() < cItem.getMaxStackSize() && isSameItem(item, cItem)) {
                return i;
            }
        }
        return -1;
    }

    private static int firstEmpty(ItemStack[] inventory) {
        for (int i = 0; i < inventory.length; i++) {
            if (inventory[i] == null) {
                return i;
            }
        }
        return -1;
    }

    public static String serializeItem(ItemStack itemStack) {
        FileConfiguration temp = new YamlConfiguration();
        temp.set(ITEM_STACK_PATH, itemStack);
        return temp.saveToString();
    }

    public static ItemStack deserializeItemString(String configContents) {
        FileConfiguration temp = new YamlConfiguration();
        try {
            temp.loadFromString(configContents);
            return temp.getItemStack(ITEM_STACK_PATH);
        } catch (InvalidConfigurationException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Most of this function was copied from CraftBukkit.  The above functions too.

    public static void saferItemGive(PlayerInventory playerInventory, ItemStack item) {
        // After bukkit is fixed:
        // player.getInventory().addItem(givingItems);

        while (true) {
            // Do we already have a stack of it?
            int firstPartial = firstPartial(item, playerInventory.getStorageContents());

            // Drat! no partial stack
            if (firstPartial == -1) {
                // Find a free spot!
                int firstFree = firstEmpty(playerInventory.getStorageContents());

                if (firstFree == -1) {
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
                if (amount + partialAmount <= maxAmount) {
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
        if (item == null) {
            return null;
        }
        ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta == null) {
            return null;
        }
        List<String> pageList = itemMeta.getLore();
        if (pageList == null) {
            return null;
        }
        String[] pages = new String[pageList.size()];
        for (int i = 0; i < pageList.size(); i++) {
            pages[i] = AdventureUtil.legacyToMini(pageList.get(i));
        }
        return pages;
    }


    public static void setLore(ItemStack item, String[] pages) {
        if (item == null || pages == null) {
            return;
        }
        ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta != null) {
            List<String> pageList = new ArrayList<>();
            Collections.addAll(pageList, pages);
            itemMeta.setLore(pageList);
            item.setItemMeta(itemMeta);
        }
    }


    public static Map<Enchantment, Integer> getStoredEnchantments(ItemStack item) {
        if (item == null) {
            return null;
        }
        ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta instanceof EnchantmentStorageMeta) {
            return ((EnchantmentStorageMeta) itemMeta).getStoredEnchants();
        }
        return null;
    }


    public static void addStoredEnchantment(ItemStack item, Enchantment enchantment, Integer level, boolean ignoreLevelRestriction) {
        if (item == null) {
            return;
        }
        ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta == null) {
            return;
        }
        if (itemMeta instanceof EnchantmentStorageMeta) {
            EnchantmentStorageMeta storageMeta = (EnchantmentStorageMeta) itemMeta;
            storageMeta.addStoredEnchant(enchantment, level, ignoreLevelRestriction);
            item.setItemMeta(storageMeta);
        }
    }


    public static Integer getFireworkPower(ItemStack item) {
        if (item == null) {
            return null;
        }
        ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta instanceof FireworkMeta) {
            FireworkMeta fireworkMeta = ((FireworkMeta) itemMeta);
            return fireworkMeta.getPower();
        }
        return null;
    }


    public static void setFireworkPower(ItemStack item, Integer power) {
        if (item == null) {
            return;
        }
        ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta instanceof FireworkMeta) {
            FireworkMeta fireworkMeta = ((FireworkMeta) itemMeta);
            fireworkMeta.setPower(power);
            item.setItemMeta(fireworkMeta);
        }
    }


    public static FireworkEffect[] getFireworkEffects(ItemStack item) {
        if (item == null) {
            return null;
        }
        ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta instanceof FireworkMeta) {
            List<FireworkEffect> effectList = ((FireworkMeta) itemMeta).getEffects();
            FireworkEffect[] effects = new FireworkEffect[effectList.size()];
            for (int i = 0; i < effectList.size(); i++) {
                effects[i] = effectList.get(i);
            }
            return effects;
        } else if (itemMeta instanceof FireworkEffectMeta) {
            FireworkEffect[] effects = new FireworkEffect[1];
            effects[0] = ((FireworkEffectMeta) itemMeta).getEffect();
            return effects;
        }
        return null;
    }


    public static void setFireworkEffects(ItemStack item, FireworkEffect[] effects) {
        if (item == null || effects == null) {
            return;
        }
        ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta instanceof FireworkMeta) {
            FireworkMeta fireworkMeta = ((FireworkMeta) itemMeta);
            fireworkMeta.addEffects(effects);
            item.setItemMeta(fireworkMeta);
        } else if (itemMeta instanceof FireworkEffectMeta) {
            if (effects.length > 0) {
                FireworkEffectMeta fireworkEffectMeta = ((FireworkEffectMeta) itemMeta);
                fireworkEffectMeta.setEffect(effects[0]);
                item.setItemMeta(fireworkEffectMeta);
            }
        }
    }


    public static Integer getRepairCost(ItemStack item) {
        if (item == null) {
            return null;
        }
        ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta instanceof Repairable) {
            return ((Repairable) itemMeta).getRepairCost();
        }
        return null;
    }


    public static void setRepairCost(ItemStack item, Integer repairCost) {
        if (item == null) {
            return;
        }
        ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta instanceof Repairable) {
            Repairable repairable = ((Repairable) itemMeta);
            repairable.setRepairCost(repairCost);
            item.setItemMeta((ItemMeta) repairable);
        }
    }


    public static String getDisplayName(ItemStack item) {
        if (item == null) {
            return null;
        }
        ItemMeta itemMeta = item.getItemMeta();
        return itemMeta == null ? null : AdventureUtil.legacyToMini(itemMeta.getDisplayName());
    }


    public static void setDisplayName(ItemStack item, String name) {
        if (item == null || name == null) {
            return;
        }
        ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta != null) {
            itemMeta.setDisplayName(name);
            item.setItemMeta(itemMeta);
        }
    }


    public static String getBookAuthor(ItemStack book) {
        if (book == null) {
            return null;
        }
        ItemMeta itemMeta = book.getItemMeta();
        if (itemMeta instanceof BookMeta) {
            return ((BookMeta) itemMeta).getAuthor();
        }
        return null;
    }


    public static void setBookAuthor(ItemStack book, String author) {
        if (book == null) {
            return;
        }
        ItemMeta itemMeta = book.getItemMeta();
        if (itemMeta instanceof BookMeta) {
            BookMeta bookMeta = ((BookMeta) itemMeta);
            bookMeta.setAuthor(author);
            book.setItemMeta(bookMeta);
        }
    }


    public static String getBookTitle(ItemStack book) {
        if (book == null) {
            return null;
        }
        ItemMeta itemMeta = book.getItemMeta();
        if (itemMeta instanceof BookMeta) {
            return ((BookMeta) itemMeta).getTitle();
        }
        return null;
    }


    public static void setBookTitle(ItemStack book, String title) {
        if (book == null) {
            return;
        }
        ItemMeta itemMeta = book.getItemMeta();
        if (itemMeta instanceof BookMeta) {
            BookMeta bookMeta = ((BookMeta) itemMeta);
            bookMeta.setTitle(title);
            book.setItemMeta(bookMeta);
        }
    }


    public static String[] getBookPages(ItemStack book) {
        if (book == null) {
            return null;
        }
        ItemMeta itemMeta = book.getItemMeta();
        if (itemMeta instanceof BookMeta) {
            List<String> pageList = ((BookMeta) itemMeta).getPages();
            String[] pages = new String[pageList.size()];
            for (int i = 0; i < pageList.size(); i++) {
                pages[i] = pageList.get(i);
            }
            return pages;
        }
        return null;
    }


    public static void setBookPages(ItemStack book, String[] pages) {
        if (book == null || pages == null) return;
        ItemMeta itemMeta = book.getItemMeta();
        if (itemMeta instanceof BookMeta) {
            BookMeta bookMeta = ((BookMeta) itemMeta);
            bookMeta.setPages(pages);
            book.setItemMeta(bookMeta);
        }
    }

    public static boolean isSameItem(ItemStack item, String searchString) {
        Material mat;
        short damageId = 0;
        if (searchString.contains(",")) {
            String[] split = searchString.split(",");
            mat = Material.getMaterial(split[0]);
            damageId = Short.parseShort(split[1]);
        } else {
            mat = Material.getMaterial(searchString);
        }
        if (mat == null) {
            return false;
        }
        if (damageId != 0) {
            return isSameItem(item, new ItemStack(mat, 1, damageId));
        }
        return isSameItem(item, new ItemStack(mat, 1));
    }

    //Short for serializeAndDeserialize
    //Helper method to get around serialization
    //issue due to inconsistencies. This really
    //is just a hack, but it is really all we can
    //do besides rewriting the BukkitObjectOutputStream
    //or something similar
    private static ItemStack sad(ItemStack itemStack) {
        Inventory inventory = Bukkit.createInventory(null, 9);
        inventory.setItem(0, deserializeItemString(serializeItem(itemStack)));
        return inventory.getItem(0);
    }

    //We cannot compare null stacks but shouldn't even happen
    public static boolean isSameItem(ItemStack item1, ItemStack item2) {
        if (item1 != null && item2 != null && item1.getType() == item2.getType()) {
            return sad(item1).isSimilar(sad(item2));
        }
        return false;
    }

    public static int getMaxStackSize(ItemStack item) {
        return item == null ? 0 : item.getType().getMaxStackSize();
    }

    public static int getSpaceForItem(Player player, ItemStack item) {
        int maxstack = getMaxStackSize(item);
        int space = 0;
        ItemStack[] items = player.getInventory().getStorageContents();
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


    public static boolean hasAmount(String ownerName, int amount, ItemStack compareItem) {
        int has = getAmount(ownerName, compareItem);
        return has >= amount;
    }


    public static int getAmount(String ownerName, ItemStack compareItem) {
        if (Bukkit.getPlayer(ownerName) == null) return 0;
        PlayerInventory inventory = Bukkit.getPlayer(ownerName).getInventory();
        ItemStack[] items = inventory.getStorageContents();
        int has = 0;
        for (ItemStack item : items) {
            if (isSameItem(compareItem, item)) {
                has += item.getAmount();
            }
        }
        return has;
    }


    public static void remove(String playerName, int amount, ItemStack compareItem) {
        Player player = Bukkit.getPlayer(playerName);
        if (player != null) {
            PlayerInventory inventory = player.getInventory();

            // Remove held item first:
            ItemStack hand = LegacyUtil.getItemInMainHand(player);
            if (isSameItem(compareItem, hand)) {
                int heldAmount = hand.getAmount();
                if (heldAmount <= amount) {
                    amount -= heldAmount;
                    inventory.clear(inventory.getHeldItemSlot());
                } else {
                    hand.setAmount(heldAmount - amount);
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


    public static String getEnchantmentName(Entry<Enchantment, Integer> enchantment) {
        Enchantment enchantmentType = enchantment.getKey();
        int enchantmentLevel = enchantment.getValue();
        String enchantmentName;
        if (enchantmentNames.get(enchantmentType) != null) {
            enchantmentName = enchantmentNames.get(enchantmentType) + " ";
        } else {
            enchantmentName = EnumUtil.formatName(enchantmentType.getName()) + " ";
        }
        if (enchantmentLevels.get(enchantmentLevel) != null) {
            enchantmentName += enchantmentLevels.get(enchantmentLevel) + " ";
        } else {
            enchantmentName += enchantmentLevel + " ";
        }
        return enchantmentName;
    }


    public static Boolean isPlayerHead(ItemStack lot) {
        if (lot == null) {
            return false;
        } else if (lot.getItemMeta() == null) {
            return false;
        }
        String lotType = lot.getType().name();
        return lotType.equals("PLAYER_HEAD") || (lotType.equals("SKULL_ITEM") && LegacyUtil.getDurability(lot) == 3);
    }


    public static String getPlayerHeadOwner(ItemStack lot) {
        if (!isPlayerHead(lot)) {
            return null;
        }
        SkullMeta itemMeta = (SkullMeta) lot.getItemMeta();
        if (!itemMeta.hasOwner()) {
            return null;
        }
        return itemMeta.getOwningPlayer().getName();
    }


    public static void setPlayerHeadOwner(ItemStack item, String headName) {
        if (item == null) {
            return;
        }
        ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta instanceof SkullMeta) {
            SkullMeta skullMeta = ((SkullMeta) itemMeta);
            skullMeta.setOwner(headName);
            item.setItemMeta(skullMeta);
        }
    }

    private Items() {
    }
}