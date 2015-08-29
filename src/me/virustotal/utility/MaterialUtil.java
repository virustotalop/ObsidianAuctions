package me.virustotal.utility;

import java.util.HashMap;

import org.bukkit.block.Skull;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import com.flobi.floAuction.AuctionConfig;
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
		
		if(id == 52)
		{
			if(floAuction.sUtil != null)
			{
				if(AuctionConfig.getBoolean("allow-renamed-mobspawners", null))
				{
					if(item.hasItemMeta())
					{
						if(item.getItemMeta().hasDisplayName())
						{
							short sid = floAuction.sUtil.getDefaultEntityID();
							String creature = floAuction.sUtil.getCreatureName(sid);
							if(floAuction.sUtil.isKnown(creature))
							{
								return item.getItemMeta().getDisplayName();
							}
						}
					}	
				}
			}
		}
		
		if(names.get(id + "," + dura) == null && floAuction.isDamagedAllowed)
		{
			if(names.get(id + "," + 0) != null)
			{
				name = names.get(id + "," + 0);
			}
			else {
				name = id + ":" + dura;
			}
		}
		else if(names.get(id + "," + dura) != null){
			name = names.get(id + "," + dura);
		}
		else {
			name = id + ":" + dura;
		}
		return name;
	}
}