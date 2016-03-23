package com.flobi.floauction.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.flobi.floauction.Auction;

public class AuctionStartEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    
    private boolean cancelled;
    private Player player;
    private Auction auction;
    
    public AuctionStartEvent(Player player, Auction auction) 
    {
    	this.player = player;
    	this.auction = auction;
    	this.cancelled = false;
    }
    
	public HandlerList getHandlers() 
	{
		return handlers;
	}

	public boolean isCancelled() 
	{
		return this.cancelled;
	}

	public void setCancelled(boolean cancelled) 
	{
		this.cancelled = cancelled;
	}
	
	public Player getPlayer() 
	{
		return this.player;
	}
	
	public Auction getAuction() 
	{
		return this.auction;
	}
}
