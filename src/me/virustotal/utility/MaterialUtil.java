package me.virustotal.utility;

import java.util.HashMap;

import org.bukkit.inventory.ItemStack;

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
		if(names.get(id + "," + dura) == null && floAuction.isDamagedAllowed)
		{
			if(names.get(id + "," + 0) != null)
			{
				name = names.get(id + "," + 0);
			}
		}
		else if(names.get(id + "," + dura) != null){
			name = names.get(id + "," + dura);
		}
		else {
			name = names.get("unknown-block");
		}
		return name;
	}
}