package com.flobi.floauction.listener;

import com.flobi.floauction.FloAuction;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class InventoryClickListener implements Listener {

    @EventHandler
    public void inventoryClick(InventoryClickEvent e) {
        if(e.getInventory() != null) {
            if(e.getClick() != null) {
                if(e.getWhoClicked().getOpenInventory() != null) {
                    if(e.getWhoClicked().getOpenInventory().getTitle().equals(FloAuction.guiQueueName)) {
                        e.setCancelled(true);
                    }
                }
            }
        }
    }
}