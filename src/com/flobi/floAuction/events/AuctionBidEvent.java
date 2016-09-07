package com.flobi.floauction.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.flobi.floauction.Auction;

public class AuctionBidEvent extends Event implements Cancellable {
	
    private static final HandlerList handlers = new HandlerList();
    
    private boolean cancelled;
    private Player player;
    private Auction auction;
    private double bidAmount;
    private double hiddenMaxBid;
    private boolean isBuy;
    
    public AuctionBidEvent(Player player, Auction auction, double bidAmount, double hiddenMaxBid, boolean isBuy) 
    {
    	this.player = player;
    	this.auction = auction;
    	this.bidAmount = bidAmount;
    	this.hiddenMaxBid = hiddenMaxBid;
    	this.isBuy = isBuy;
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
	
	public double getBidAmount() 
	{
		return this.bidAmount;
	}
	
	public double getHiddenMaxBid() 
	{
		return this.hiddenMaxBid;
	}
	
	public boolean getIsBuy() 
	{
		return this.isBuy;
	}
	
	public static HandlerList getHandlerList()
	{
		return AuctionBidEvent.handlers;
	}
}
