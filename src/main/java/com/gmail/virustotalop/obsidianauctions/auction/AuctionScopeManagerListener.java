package com.gmail.virustotalop.obsidianauctions.auction;

import com.google.inject.Inject;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class AuctionScopeManagerListener implements Listener {

    private final AuctionScopeManager scope;

    @Inject
    private AuctionScopeManagerListener(AuctionScopeManager scope) {
        this.scope = scope;
    }

    @EventHandler
    public void onPlayerQuitEvent(PlayerQuitEvent event) {
        this.scope.clearPlayerScope(event.getPlayer());
    }
}
