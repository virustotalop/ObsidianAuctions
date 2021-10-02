package com.gmail.virustotalop.obsidianauctions.util;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;

import java.lang.reflect.Method;

@ApiStatus.Internal
public class PlaceholderAPIUtil {

    private static Method setPlaceHolders;

    public static String setPlaceHolders(Player player, String message) {
        try {
            if(setPlaceHolders == null) {
                Class<?> placeholderAPI = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
                setPlaceHolders = placeholderAPI.getDeclaredMethod("setPlaceholders", OfflinePlayer.class, String.class);
                setPlaceHolders.setAccessible(true);
            }
            return (String) setPlaceHolders.invoke(null, player, message);
        } catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}