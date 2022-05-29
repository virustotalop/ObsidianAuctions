package com.gmail.virustotalop.obsidianauctions.inventory;

import com.gmail.virustotalop.obsidianauctions.AuctionConfig;
import com.gmail.virustotalop.obsidianauctions.auction.AuctionScope;
import com.gmail.virustotalop.obsidianauctions.util.AdventureUtil;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class QueueInventoryHolder implements InventoryHolder {

    private final Inventory inventory;

    public QueueInventoryHolder(AuctionScope auctionScope) {
        this.inventory = this.createInventory(auctionScope);
    }

    private Inventory createInventory(AuctionScope auctionScope) {
        String guiTitle = AuctionConfig.getLanguageString("queue-gui-title", auctionScope);
        String colorizedTitle = AdventureUtil.miniToLegacy(guiTitle);
        return Bukkit.getServer().createInventory(this, 18, colorizedTitle);
    }

    @Override
    public Inventory getInventory() {
        return this.inventory;
    }
}
