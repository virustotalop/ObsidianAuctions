package me.virustotal.utility;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import com.flobi.floAuction.floAuction;

public class MaterialUtil {

	private floAuction plugin;
	public MaterialUtil(floAuction plugin)
	{
		this.plugin = plugin;
	}

	public String getName(ItemStack item)
	{
		HashMap<String,String> names = plugin.names;
		int id = item.getTypeId();
		short dura = item.getDurability();
		String name = "";

		if(id == 397)
		{
			if(dura == 3)
			{
				SkullMeta skullMeta = (SkullMeta) item.getItemMeta();
				if(skullMeta.hasOwner())
				{
					return skullMeta.getOwner() + "\'s" + " Head";
				}
			}	
		}
		else if(id == 52)
		{
			return MaterialUtil.getSpawnerType(item) + " Spawner";
		}
		else if(names.get(id + "," + dura) == null && floAuction.isDamagedAllowed)
		{
			if(names.get(id + "," + 0) != null)
			{
				name = names.get(id + "," + 0);
			}
			else 
			{
				name = id + ":" + dura;
			}
		}
		else if(names.get(id + "," + dura) != null)
		{
			name = names.get(id + "," + dura);
		}
		else 
		{
			name = id + ":" + dura;
		}
		return name;
	}
	
	private static String getSpawnerType(ItemStack item)
	{
		String type = "";
		if(MaterialUtil.getVersion().contains("1_7"))
		{
			short dura = item.getDurability();
			return EntityType.fromId(dura).getName();
		}
		else
		{
			try {
				Class<?> craftItemStack = Class.forName("org.bukkit.craftbukkit." + getVersion() + ".inventory.CraftItemStack");
				Method asCraftCopy = craftItemStack.getMethod("asCraftCopy", new Class[] {ItemStack.class});
				Method asNMSCopy = craftItemStack.getMethod("asNMSCopy", new Class[] {ItemStack.class});
				Object craftCopy = asCraftCopy.invoke(null, item);
				Object itemStack = asNMSCopy.invoke(null, (ItemStack)craftCopy);
				Method tagField = itemStack.getClass().getMethod("getTag");
				Object tag  = tagField.invoke(itemStack);
				Method getCompound = tag.getClass().getMethod("getCompound", String.class);
				Object compound = getCompound.invoke(tag, "BlockEntityTag");
				type = (String) compound.getClass().getMethod("getString", String.class).invoke(compound, "EntityId");	
			} catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				e.printStackTrace();
			}
		}
		return type;
	}

	private synchronized static String getVersion() 
	{
		String version = "";
		if(Bukkit.getServer() == null)
		{
			return null;
		}
		String name = Bukkit.getServer().getClass().getPackage().getName();
		version = name.substring(name.lastIndexOf('.') + 1);
		return version;
	}
}