package com.gmail.virustotalop.obsidianauctions.listener;

import com.gmail.virustotalop.obsidianauctions.AuctionConfig;
import com.gmail.virustotalop.obsidianauctions.ObsidianAuctions;
import com.gmail.virustotalop.obsidianauctions.auction.AuctionParticipant;
import com.gmail.virustotalop.obsidianauctions.auction.AuctionScope;
import com.garbagemule.MobArena.events.ArenaPlayerJoinEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class MobArenaListener implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onArenaJoin(ArenaPlayerJoinEvent event) {
        Player player = event.getPlayer();
        if(player == null) {
            return;
        }
        String playerName = player.getName();
        if(!AuctionConfig.getBoolean("allow-arenas", AuctionScope.getPlayerScope(player)) && AuctionParticipant.isParticipating(playerName)) {
            ObsidianAuctions.getMessageManager().sendPlayerMessage("arena-warning", playerName, (AuctionScope) null);
            event.setCancelled(true);
        }
    }
}