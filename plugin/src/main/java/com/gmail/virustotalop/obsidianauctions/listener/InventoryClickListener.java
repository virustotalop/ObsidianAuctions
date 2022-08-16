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

package com.gmail.virustotalop.obsidianauctions.listener;

import com.gmail.virustotalop.obsidianauctions.auction.AuctionManager;
import com.gmail.virustotalop.obsidianauctions.inventory.QueueInventoryHolder;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryView;

import javax.inject.Inject;

public class InventoryClickListener implements Listener {

    private final AuctionManager auctionManager;

    @Inject
    private InventoryClickListener(AuctionManager auctionManager) {
        this.auctionManager = auctionManager;
    }

    @EventHandler
    public void inventoryClick(InventoryClickEvent e) {
        if (e.getInventory() != null) {
            if (e.getClick() != null) {
                if (e.getWhoClicked().getOpenInventory() != null) {
                    InventoryView view = e.getWhoClicked().getOpenInventory();
                    if (isAuctionHolder(view)) {
                        e.setCancelled(true);
                    }
                }
            }
        }
    }

    private boolean isAuctionHolder(InventoryView view) {
        return view.getTopInventory().getHolder() instanceof QueueInventoryHolder
        || view.getBottomInventory().getHolder() instanceof QueueInventoryHolder;
    }
}