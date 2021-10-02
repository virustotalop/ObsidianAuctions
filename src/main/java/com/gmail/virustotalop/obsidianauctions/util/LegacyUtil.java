package com.gmail.virustotalop.obsidianauctions.util;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.ApiStatus;

import java.lang.reflect.Method;

@ApiStatus.Internal
public final class LegacyUtil {

    private static final boolean durabilityExists = methodExists(ItemStack.class, "getDurability");
    private static final boolean mainHandExists = methodExists(PlayerInventory.class, "getItemInMainHand");

    public static short getDurability(ItemStack itemStack) {
        if(durabilityExists) {
            return itemStack.getDurability();
        }
        return 0;
    }


    public static ItemStack getItemInMainHand(Player player) {
        if(mainHandExists) {
            return player.getInventory().getItemInMainHand();
        }
        return player.getItemInHand();
    }

    private static boolean methodExists(Class<?> clazz, String methodName) {
        for(Method method : clazz.getDeclaredMethods()) {
            if(method.getName().equals(methodName)) {
                return true;
            }
        }
        return false;
    }
}