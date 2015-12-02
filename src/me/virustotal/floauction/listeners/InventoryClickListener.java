package me.virustotal.floauction.listeners;

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
				if(e.getWhoClicked().getOpenInventory() != null)
				{
					/*Shift clicking items into the gui was likely possible, this should fix that bug
					 */
					if(e.getWhoClicked().getOpenInventory().getTitle().equals(floAuction.guiQueueName))
						e.setCancelled(true);
				}
			}
		}
	}
}
