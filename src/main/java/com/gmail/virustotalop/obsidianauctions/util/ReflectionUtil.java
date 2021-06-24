package com.gmail.virustotalop.obsidianauctions.util;

import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;

public class ReflectionUtil {

    private static boolean durabilityExists = false;

    static {
        for(Method method : ItemStack.class.getDeclaredMethods()) {
            if(method.getName().equals("getDurability")) {
                durabilityExists = true;
            }
        }
    }

    public static short getDurability(ItemStack itemStack) {
        if(durabilityExists) {
            return itemStack.getDurability();
        }
        return 0;
    }
}