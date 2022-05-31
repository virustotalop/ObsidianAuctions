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

package com.gmail.virustotalop.obsidianauctions.inventory;

import com.gmail.virustotalop.obsidianauctions.AuctionConfig;
import com.gmail.virustotalop.obsidianauctions.Key;
import com.gmail.virustotalop.obsidianauctions.auction.AuctionScope;
import com.gmail.virustotalop.obsidianauctions.util.AdventureUtil;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class QueueInventoryHolder implements InventoryHolder {

    private final Inventory inventory;

    public QueueInventoryHolder(AuctionScope auctionScope) {
        this.inventory = this.createInventory(auctionScope);
    }

    private Inventory createInventory(AuctionScope auctionScope) {
        String guiTitle = AuctionConfig.getLanguageString(Key.QUEUE_GUI_TITLE, auctionScope);
        String colorizedTitle = AdventureUtil.miniToLegacy(guiTitle);
        return Bukkit.getServer().createInventory(this, 18, colorizedTitle);
    }

    @Override
    public Inventory getInventory() {
        return this.inventory;
    }
}
