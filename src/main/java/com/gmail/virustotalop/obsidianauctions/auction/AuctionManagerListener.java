package com.gmail.virustotalop.obsidianauctions.auction;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import javax.inject.Inject;
import java.util.UUID;

public class AuctionManagerListener implements Listener {

    private final AuctionManager auctionManager;

    @Inject
    private AuctionManagerListener(AuctionManager auctionManager) {
        this.auctionManager = auctionManager;
    }

    @EventHandler
    public void onPlayerQuitEvent(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        this.auctionManager.clearPlayerScope(player);
        AuctionParticipant participant = this.auctionManager.getParticipant(uuid);
        if(participant != null) {
            this.auctionManager.removeParticipant(participant);
        }
    }
}
