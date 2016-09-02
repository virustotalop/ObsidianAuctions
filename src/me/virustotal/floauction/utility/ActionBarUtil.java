package me.virustotal.floauction.utility;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class ActionBarUtil {

	private static Class<?> chatPacketClass;
	private static Class<?> iChatClass;
	private static Class<?> packetClass;
	private static Method iChatMethod;
	private static Method getHandle;
	private static Constructor<?> packetConstructor;
	
	public static void sendMessage(Player player, String message) throws ClassNotFoundException, NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, InstantiationException, NoSuchFieldException
	{
		if(ActionBarUtil.chatPacketClass == null)
		{
			ActionBarUtil.chatPacketClass = Class.forName("net.minecraft.server." + VersionUtil.getVersion() + ".PacketPlayOutChat");
		}
		
		if(ActionBarUtil.iChatClass == null)
		{
			ActionBarUtil.iChatClass = Class.forName("net.minecraft.server." + VersionUtil.getVersion() + ".IChatBaseComponent");
		}
		
		if(ActionBarUtil.packetClass == null)
		{
			ActionBarUtil.packetClass = Class.forName("net.minecraft.server." + VersionUtil.getVersion() + ".Packet");
		}
		
		if(ActionBarUtil.iChatMethod == null)
		{
			ActionBarUtil.iChatMethod = iChatClass.getMethod("a", String.class);
		}
		
		if(ActionBarUtil.getHandle == null)
		{
			ActionBarUtil.getHandle = player.getClass().getMethod("getHandle");
		}
		
		if(ActionBarUtil.packetConstructor == null)
		{
			ActionBarUtil.packetConstructor = ActionBarUtil.chatPacketClass.getConstructor(ActionBarUtil.iChatClass, byte.class);
		}
		
		
		String json = "{\"text\":\"" + ChatColor.translateAlternateColorCodes('&', message) + "\"}";
		Object chatObject = ActionBarUtil.iChatMethod.invoke(null, json);
		Object chatPacketObject = ActionBarUtil.packetConstructor.newInstance(chatObject, (byte)1);
		Object handleObject = ActionBarUtil.getHandle.invoke(player);
		Object playerConnectionObject = handleObject.getClass().getField("playerConnection").get(handleObject);
		playerConnectionObject.getClass().getMethod("sendPacket", ActionBarUtil.packetClass).invoke(playerConnectionObject, chatPacketObject);
		
	}	
}