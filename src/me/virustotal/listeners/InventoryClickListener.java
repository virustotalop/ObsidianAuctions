package me.virustotal.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import com.flobi.floAuction.floAuction;

public class InventoryClickListener implements Listener {

	@EventHandler
	public void invClick(InventoryClickEvent e)
	{
		if(e.getClickedInventory() != null)
		{
			if(e.getClick() != null)
			{
				if(e.getClickedInventory().getTitle().equals(floAuction.guiQueueName))
				{
					e.setCancelled(true);
				}
			}
		}
	}
}
