package com.gmail.virustotalop.obsidianauctions.listener;

import com.gmail.virustotalop.obsidianauctions.AuctionConfig;
import com.gmail.virustotalop.obsidianauctions.ObsidianAuctions;
import com.gmail.virustotalop.obsidianauctions.auction.AuctionManager;
import com.gmail.virustotalop.obsidianauctions.auction.AuctionScope;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import javax.inject.Inject;

public class InventoryClickListener implements Listener {

    private final AuctionManager auctionManager;

    @Inject
    private InventoryClickListener(AuctionManager auctionManager) {
        this.auctionManager = auctionManager;
    }

    @EventHandler
    public void inventoryClick(InventoryClickEvent e) {
        if (e.getInventory() != null) {
            if (e.getClick() != null) {
                if (e.getWhoClicked().getOpenInventory() != null) {
                    String title = e.getWhoClicked().getOpenInventory().getTitle();
                    for(AuctionScope scope : this.auctionManager.getAuctionScopes()) {
                        if (title.equals(AuctionConfig.getString("queue-gui-name", scope))) {
                            e.setCancelled(true);
                            break;
                        }
                    }
                }
            }
        }
    }
}