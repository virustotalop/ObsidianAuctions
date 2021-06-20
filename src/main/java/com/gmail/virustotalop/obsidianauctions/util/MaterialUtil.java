package com.gmail.virustotalop.obsidianauctions.util;

import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;

public class MaterialUtil {

    private static String getMobEggType(ItemStack item) {
        String type = "";
        try {
            //Should work for 1.9 and above, needs to be tested
            Class<?> craftItemStack = Class.forName("org.bukkit.craftbukkit." + VersionUtil.getVersion() + ".inventory.CraftItemStack");
            Method asCraftCopy = craftItemStack.getMethod("asCraftCopy", ItemStack.class);
            Method asNMSCopy = craftItemStack.getMethod("asNMSCopy", ItemStack.class);
            Object craftCopy = asCraftCopy.invoke(null, item);
            Object itemStack = asNMSCopy.invoke(null, craftCopy);
            Method tagField = itemStack.getClass().getMethod("getTag");
            Object tag = tagField.invoke(itemStack);
            Method getCompound = tag.getClass().getMethod("getCompound", String.class);
            Object compound = getCompound.invoke(tag, "EntityTag");
            type = (String) compound.getClass().getMethod("getString", String.class).invoke(compound, "id");
            type = formatName(type.substring(type.indexOf(":") + 1));
        } catch(Exception e) {
            e.printStackTrace();
        }
        return type;
    }

    private static String getSpawnerType(ItemStack item) {
        String type = "";
        try {
            Class<?> craftItemStack = Class.forName("org.bukkit.craftbukkit." + VersionUtil.getVersion() + ".inventory.CraftItemStack");
            Method asCraftCopy = craftItemStack.getMethod("asCraftCopy", ItemStack.class);
            Method asNMSCopy = craftItemStack.getMethod("asNMSCopy", ItemStack.class);
            Object craftCopy = asCraftCopy.invoke(null, item);
            Object itemStack = asNMSCopy.invoke(null, craftCopy);
            Method tagField = itemStack.getClass().getMethod("getTag");
            Object tag = tagField.invoke(itemStack);
            Method getCompound = tag.getClass().getMethod("getCompound", String.class);
            Object compound = getCompound.invoke(tag, "BlockEntityTag");
            Object spawnData = getCompound.invoke(compound, "SpawnData");
            type = (String) spawnData.getClass().getMethod("getString", String.class).invoke(spawnData, "id");
            type = formatName(type);
        } catch(Exception e) {
            e.printStackTrace();
        }
        return type;
    }

    private static String getItemType(ItemStack item) {
        String name = item.getType().name();
        return formatName(name);
    }

    public static String formatName(String name) {
        char[] chars = name.toCharArray();
        chars[0] = Character.toUpperCase(chars[0]);
        for(int i = 1; i < chars.length; i++) {
            if(chars[i] == '_') {
                chars[i] = ' ';
                if(i + 1 <= chars.length - 1) { //Even though this shouldn't occur it doesn't hurt to check so we don't go out of bounds
                    chars[i + 1] = Character.toUpperCase(chars[i + 1]);
                }
            } else if(chars[i - 1] != ' ') { //Check to make sure the character before is not a space to make upper case to lower case
                chars[i] = Character.toLowerCase(chars[i]);
            }
        }
        return new String(chars);
    }
}