package com.gmail.virustotalop.obsidianauctions.listener;

import com.gmail.virustotalop.obsidianauctions.AuctionConfig;
import com.gmail.virustotalop.obsidianauctions.ObsidianAuctions;
import com.gmail.virustotalop.obsidianauctions.auction.AuctionLocationManager;
import com.gmail.virustotalop.obsidianauctions.auction.AuctionScope;
import com.gmail.virustotalop.obsidianauctions.auction.AuctionManager;
import com.google.inject.Inject;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.UUID;

public class ArenaListener implements Listener {

    private final AuctionLocationManager areaManager;
    private final AuctionManager scope;

    @Inject
    private ArenaListener(AuctionLocationManager areaManager, AuctionManager scope) {
        this.areaManager = areaManager;
        this.scope = scope;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onTeleport(PlayerTeleportEvent event) {
        if(this.checkArena(event)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onMove(PlayerMoveEvent event) {
        if(this.checkArena(event)) {
            event.setCancelled(true);
        }
    }

    private boolean checkArena(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();
        // Having arena check first is most optimal since we are just doing math here
        // and a world check
        if(this.areaManager.isInArena(event.getTo()) && this.canNotJoinArenas(player) && this.participating(playerUUID)) {
            ObsidianAuctions.get().getMessageManager().sendPlayerMessage("arena-warning", playerUUID, (AuctionScope) null);
            return true;
        }
        return false;
    }

    private boolean canNotJoinArenas(Player player) {
        return !AuctionConfig.getBoolean("allow-arenas", this.scope.getPlayerScope(player));
    }

    private boolean participating(UUID playerUUID) {
        return ObsidianAuctions.get().getAuctionScopeManager().isParticipant(playerUUID);
    }
}
