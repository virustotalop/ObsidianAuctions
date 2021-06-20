package com.gmail.virustotalop.obsidianauctions.listener;

import com.garbagemule.MobArena.events.ArenaPlayerJoinEvent;
import com.gmail.virustotalop.obsidianauctions.AuctionConfig;
import com.gmail.virustotalop.obsidianauctions.ObsidianAuctions;
import com.gmail.virustotalop.obsidianauctions.auction.AuctionParticipant;
import com.gmail.virustotalop.obsidianauctions.auction.AuctionScope;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.UUID;

public class MobArenaListener implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onArenaJoin(ArenaPlayerJoinEvent event) {
        Player player = event.getPlayer();
        if(player == null) {
            return;
        }
        UUID playerUUID = player.getUniqueId();
        if(!AuctionConfig.getBoolean("allow-arenas", AuctionScope.getPlayerScope(player)) && AuctionParticipant.isParticipating(playerUUID)) {
            ObsidianAuctions.get().getMessageManager().sendPlayerMessage("arena-warning", playerUUID, (AuctionScope) null);
            event.setCancelled(true);
        }
    }
}