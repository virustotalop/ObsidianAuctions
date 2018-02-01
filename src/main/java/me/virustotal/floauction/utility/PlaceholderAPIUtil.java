package me.virustotal.floauction.utility;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.bukkit.entity.Player;


/*
 * Work around for PlaceHolderAPI's license
 * Since PlaceHolderAPI does not want versions distributed and
 * my reluctance to use 3rd party spigot plugin repo's this reflection
 * should accomplish the same thing as calling the api directly.
*/
public class PlaceholderAPIUtil {

	private static Class<?> placeholderAPI;
	private static Method setPlaceHolders;
	
	public static String setPlaceHolders(Player player, String message)
	{
		try 
		{
			if(placeholderAPI == null)
			{
				placeholderAPI = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
			}
			if(setPlaceHolders == null)
			{
				setPlaceHolders = placeholderAPI.getDeclaredMethod("setPlaceHolders", new Class[] {Player.class, String.class});
				setPlaceHolders.setAccessible(true); //Should make the reflection marginally faster, even though the method is public
			}
			return (String) setPlaceHolders.invoke(null, player, message);
		} 
		catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) 
		{
			e.printStackTrace();
		}
		return null;
	}
}