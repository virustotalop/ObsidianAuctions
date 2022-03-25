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

import com.gmail.virustotalop.obsidianauctions.AuctionConfig;
import com.gmail.virustotalop.obsidianauctions.ObsidianAuctions;
import com.gmail.virustotalop.obsidianauctions.auction.AuctionLocationManager;
import com.gmail.virustotalop.obsidianauctions.auction.AuctionManager;
import com.gmail.virustotalop.obsidianauctions.auction.AuctionScope;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import javax.inject.Inject;
import java.util.UUID;

public class ArenaListener implements Listener {

    private final AuctionLocationManager areaManager;
    private final AuctionManager scope;

    @Inject
    private ArenaListener(AuctionLocationManager areaManager, AuctionManager scope) {
        this.areaManager = areaManager;
        this.scope = scope;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onTeleport(PlayerTeleportEvent event) {
        if (this.checkArena(event)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onMove(PlayerMoveEvent event) {
        if (this.checkArena(event)) {
            event.setCancelled(true);
        }
    }

    private boolean checkArena(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();
        // Having arena check first is most optimal since we are just doing math here
        // and a world check
        if (this.areaManager.isInArena(event.getTo()) && this.canNotJoinArenas(player) && this.participating(playerUUID)) {
            ObsidianAuctions.get().getMessageManager().sendPlayerMessage("arena-warning", playerUUID, (AuctionScope) null);
            return true;
        }
        return false;
    }

    private boolean canNotJoinArenas(Player player) {
        return !AuctionConfig.getBoolean("allow-arenas", this.scope.getPlayerScope(player));
    }

    private boolean participating(UUID playerUUID) {
        return ObsidianAuctions.get().getAuctionManager().isParticipant(playerUUID);
    }
}
