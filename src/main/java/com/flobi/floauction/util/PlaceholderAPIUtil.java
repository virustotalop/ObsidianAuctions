package com.flobi.floauction.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.bukkit.entity.Player;

public class PlaceholderAPIUtil {

	private static Class<?> placeholderAPI;
	private static Method setPlaceHolders;
	
	public static String setPlaceHolders(Player player, String message) {
		try {
			if(placeholderAPI == null) {
				placeholderAPI = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
			}
			if(setPlaceHolders == null) {
				setPlaceHolders = placeholderAPI.getDeclaredMethod("setPlaceHolders", new Class[] {Player.class, String.class});
				setPlaceHolders.setAccessible(true);
			}
			return (String) setPlaceHolders.invoke(null, player, message);
		} catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
		}
		return null;
	}
}