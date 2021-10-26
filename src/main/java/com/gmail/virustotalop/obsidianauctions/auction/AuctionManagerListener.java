package com.gmail.virustotalop.obsidianauctions.auction;

import com.google.inject.Inject;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class AuctionManagerListener implements Listener {

    private final AuctionManager auctionManager;

    @Inject
    private AuctionManagerListener(AuctionManager auctionManager) {
        this.auctionManager = auctionManager;
    }

    @EventHandler
    public void onPlayerQuitEvent(PlayerQuitEvent event) {
        this.auctionManager.clearPlayerScope(event.getPlayer());
    }
}
