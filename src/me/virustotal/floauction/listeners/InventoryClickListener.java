package me.virustotal.floauction.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import com.flobi.floauction.FloAuction;

public class InventoryClickListener implements Listener {

	@EventHandler
	public void invClick(InventoryClickEvent e)
	{
		if(e.getInventory() != null)
		{
			if(e.getClick() != null)
			{
				if(e.getWhoClicked().getOpenInventory() != null)
				{
					if(e.getWhoClicked().getOpenInventory().getTitle().equals(FloAuction.guiQueueName))
						e.setCancelled(true);
				}
			}
		}
	}
}