package com.flobi.floauction.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import com.flobi.floauction.AuctionConfig;
import com.flobi.floauction.FloAuction;

public class MaterialUtil {

	public static String getName(ItemStack item)
	{
		if(item == null) //Even though it shouldn't happen
			return "Air";
		
		HashMap<String,String> names = FloAuction.plugin.names;
		int id = item.getTypeId(); //Code needs to be updated eventually, waiting for dura to be completely removed
		short dura = item.getDurability();
		String name = "";

		if(id == 397)
		{
			if(dura == 3)
			{
				SkullMeta skullMeta = (SkullMeta) item.getItemMeta();
				if(skullMeta.hasOwner())
				{
					if(skullMeta.getOwner() != null && !(skullMeta.equals("")))
						return skullMeta.getOwner() + "\'s" + " Head";
				}
			}	
		}
		else if(id == 383) //mob eggs
		{
			return MaterialUtil.getMobEggType(item) + " Spawn Egg";
		}
		
		if(id == 52 && AuctionConfig.getBoolean("allow-mobspawners", null))
		{
			return MaterialUtil.getSpawnerType(item) + " Spawner";
		}
		else if(AuctionConfig.getBoolean("renamed-items-override", null) && Items.getDisplayName(item) != null && id != 52)
		{
			return Items.getDisplayName(item);
		}
		else if(names.get(id + "," + dura) == null && FloAuction.isDamagedAllowed)
		{
			if(names.get(id + "," + 0) != null)
			{
				name = names.get(id + "," + 0);
			}
			else 
			{
				name = MaterialUtil.getItemType(item) + ":" + dura;
			}
		}
		else if(names.get(id + "," + dura) != null)
		{
			name = names.get(id + "," + dura);
		}
		else 
		{
			name = MaterialUtil.getItemType(item);
		}
		return name;
	}
	
	private static String getMobEggType(ItemStack item)
	{
		String type = "";
		try 
		{
			if(VersionUtil.getVersion().contains("1_8"))
			{
				String entityType = EntityType.fromId(item.getDurability()).getName();
				type = (Character.toUpperCase(entityType.charAt(0)) + entityType.toLowerCase().substring(1)).replace("_", "");
			}
			else //Should work for 1.9 and above, needs to be tested
			{
				Class<?> craftItemStack = Class.forName("org.bukkit.craftbukkit." + VersionUtil.getVersion() + ".inventory.CraftItemStack");
				Method asCraftCopy = craftItemStack.getMethod("asCraftCopy", new Class[] {ItemStack.class});
				Method asNMSCopy = craftItemStack.getMethod("asNMSCopy", new Class[] {ItemStack.class});
				Object craftCopy = asCraftCopy.invoke(null, item);
				Object itemStack = asNMSCopy.invoke(null, (ItemStack)craftCopy);
				Method tagField = itemStack.getClass().getMethod("getTag");
				Object tag  = tagField.invoke(itemStack);
				Method getCompound = tag.getClass().getMethod("getCompound", String.class);
				Object compound = getCompound.invoke(tag, "EntityTag");
				type = (String) compound.getClass().getMethod("getString", String.class).invoke(compound, "id");
			}


		} 
		catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) 
		{
			e.printStackTrace();
		}
		return type;
	}
	
	private static String getSpawnerType(ItemStack item)
	{
		String type = ""; //Support is dropped for 1.7
		try 
		{
			Class<?> craftItemStack = Class.forName("org.bukkit.craftbukkit." + VersionUtil.getVersion() + ".inventory.CraftItemStack");
			Method asCraftCopy = craftItemStack.getMethod("asCraftCopy", new Class[] {ItemStack.class});
			Method asNMSCopy = craftItemStack.getMethod("asNMSCopy", new Class[] {ItemStack.class});
			Object craftCopy = asCraftCopy.invoke(null, item);
			Object itemStack = asNMSCopy.invoke(null, (ItemStack)craftCopy);
			Method tagField = itemStack.getClass().getMethod("getTag");
			Object tag  = tagField.invoke(itemStack);
			Method getCompound = tag.getClass().getMethod("getCompound", String.class);
			Object compound = getCompound.invoke(tag, "BlockEntityTag");
			if(VersionUtil.getVersion().contains("1_8"))
			{
				type = (String) compound.getClass().getMethod("getString", String.class).invoke(compound, "EntityId");	
			}
			else if(VersionUtil.getVersion().contains("1_9"))
			{
				Object spawnData = getCompound.invoke(compound, "SpawnData");
				type = (String) spawnData.getClass().getMethod("getString", String.class).invoke(spawnData, "id");
			}
			else //Should work for 1.10 and above, needs to be tested
			{
				Object spawnData = getCompound.invoke(compound, "SpawnData");
				type = (String) spawnData.getClass().getMethod("getString", String.class).invoke(spawnData, "id");
			}


		} 
		catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) 
		{
			e.printStackTrace();
		}
		return type;
	}
	
	private  static String getItemType(ItemStack item)
	{
		char[] chars = item.getType().name().toCharArray();
		chars[0] = Character.toUpperCase(chars[0]);
		
		for(int i = 1; i < chars.length; i++)
		{
			
			if(chars[i] == '_')
			{
				chars[i] = ' ';
				if(i + 1 <= chars.length - 1) //Even though this shouldn't occur it doesn't hurt to check so we don't go out of bounds
				{
					chars[i + 1] = Character.toUpperCase(chars[i + 1]);
				}
			}
			else if(chars[i - 1] != ' ') //Check to make sure the character before is not a space to make upper case to lower case
			{
				chars[i] = Character.toLowerCase(chars[i]);
			}
		}
		return new String(chars);
	}
}