package com.flobi.floauction.listener;

import com.flobi.floauction.util.CArrayList;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import com.flobi.floauction.AuctionConfig;
import com.flobi.floauction.AuctionParticipant;
import com.flobi.floauction.AuctionScope;
import com.flobi.floauction.FloAuction;
import com.garbagemule.MobArena.events.ArenaPlayerJoinEvent;

public class MobArenaListener implements Listener {
	
	@EventHandler(ignoreCancelled = true)
	public void onArenaJoin(ArenaPlayerJoinEvent event) {
		Player player = event.getPlayer();
		if (player == null) {
			return;
		}
		String playerName = player.getName();
		if (!AuctionConfig.getBoolean("allow-arenas", AuctionScope.getPlayerScope(player)) && AuctionParticipant.isParticipating(playerName)) {
			FloAuction.getMessageManager().sendPlayerMessage(new CArrayList<String>(new String[] {"arena-warning"}), playerName, (AuctionScope) null);
			event.setCancelled(true);
		}
	}
}