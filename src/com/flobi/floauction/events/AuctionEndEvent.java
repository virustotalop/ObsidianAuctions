package com.flobi.floauction.events;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.flobi.floauction.Auction;

public class AuctionEndEvent extends Event implements Cancellable {
	
    private static final HandlerList handlers = new HandlerList();
    
    private boolean cancelled;
    private Auction auction;
    
    public AuctionEndEvent(Auction auction, boolean cancelled) 
    {
    	this.auction = auction;
    	this.cancelled = cancelled;
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
	
	public Auction getAuction() 
	{
		return this.auction;
	}
	
	public static HandlerList getHandlerList()
	{
		return AuctionEndEvent.handlers;
	}
}
