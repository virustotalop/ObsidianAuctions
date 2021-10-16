package com.gmail.virustotalop.obsidianauctions.util;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

@ApiStatus.Internal
public class PlaceholderAPIUtil {
    
    private static MethodHandle setPlaceHolders;

    public static String setPlaceHolders(Player player, String message) {
        try {
            if(setPlaceHolders == null) {
                Class<?> placeholderAPI = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
                Method reflect = placeholderAPI.getDeclaredMethod("setPlaceholders",
                        OfflinePlayer.class, String.class);
                setPlaceHolders = MethodHandles.lookup().unreflect(reflect);
            }
            return (String) setPlaceHolders.invoke(player, message);
        }
        catch(Throwable e) {
            e.printStackTrace();
        }
        return null;
    }
}