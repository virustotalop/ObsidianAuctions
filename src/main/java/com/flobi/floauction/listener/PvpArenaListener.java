package com.flobi.floauction.listener;

//import net.slipcor.pvparena.events.PAJoinEvent;

import org.bukkit.event.Listener;

public class PvpArenaListener implements Listener {
	
	/*@EventHandler
	public void onPAPlayerJoin(PAJoinEvent event) 
	{
		if (event.isCancelled()) 
		{
			return;
		}
		Player player = event.getPlayer();
		if (player == null) 
		{
			return;
		}
		String playerName = player.getName();
		if (!AuctionConfig.getBoolean("allow-arenas", AuctionScope.getPlayerScope(player)) && AuctionParticipant.isParticipating(playerName)) 
		{
			FloAuction.getMessageManager().sendPlayerMessage(new CArrayList<String>(new String[] {"arena-warning"}), playerName, (AuctionScope) null);
			event.setCancelled(true);
		}
	}*/
}