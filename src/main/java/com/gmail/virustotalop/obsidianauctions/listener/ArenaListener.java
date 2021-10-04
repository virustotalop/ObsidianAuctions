package com.gmail.virustotalop.obsidianauctions.listener;

import com.gmail.virustotalop.obsidianauctions.AuctionConfig;
import com.gmail.virustotalop.obsidianauctions.ObsidianAuctions;
import com.gmail.virustotalop.obsidianauctions.arena.ArenaManager;
import com.gmail.virustotalop.obsidianauctions.auction.AuctionParticipant;
import com.gmail.virustotalop.obsidianauctions.auction.AuctionScope;
import com.google.inject.Inject;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.UUID;

public class ArenaListener {

    private final ArenaManager areaManager;

    @Inject
    private ArenaListener(ArenaManager areaManager) {
        this.areaManager = areaManager;
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
        return !AuctionConfig.getBoolean("allow-arenas", AuctionScope.getPlayerScope(player));
    }

    private boolean participating(UUID playerUUID) {
        return AuctionParticipant.isParticipating(playerUUID);
    }
}
