/*
 *     ObsidianAuctions
 *     Copyright (C) 2012-2022 flobi and contributors
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.gmail.virustotalop.obsidianauctions.event;

import com.gmail.virustotalop.obsidianauctions.auction.Auction;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class AuctionBidEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private boolean cancelled;
    private final Player player;
    private final Auction auction;
    private final double bidAmount;
    private final double hiddenMaxBid;
    private final boolean isBuy;

    public AuctionBidEvent(Player player, Auction auction, double bidAmount, double hiddenMaxBid, boolean isBuy) {
        this.player = player;
        this.auction = auction;
        this.bidAmount = bidAmount;
        this.hiddenMaxBid = hiddenMaxBid;
        this.isBuy = isBuy;
        this.cancelled = false;
    }

    public Player getPlayer() {
        return this.player;
    }

    public Auction getAuction() {
        return this.auction;
    }

    public double getBidAmount() {
        return this.bidAmount;
    }

    public double getHiddenMaxBid() {
        return this.hiddenMaxBid;
    }

    public boolean getIsBuy() {
        return this.isBuy;
    }

    public boolean isCancelled() {
        return this.cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return AuctionBidEvent.handlers;
    }
}
