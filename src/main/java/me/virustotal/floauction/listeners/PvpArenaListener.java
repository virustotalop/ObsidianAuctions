package me.virustotal.floauction.listeners;

import me.virustotal.floauction.utility.CArrayList;
//import net.slipcor.pvparena.events.PAJoinEvent;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import com.flobi.floauction.AuctionConfig;
import com.flobi.floauction.AuctionParticipant;
import com.flobi.floauction.AuctionScope;
import com.flobi.floauction.FloAuction;

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