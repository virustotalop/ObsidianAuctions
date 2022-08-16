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

package com.gmail.virustotalop.obsidianauctions.message;

import com.gmail.virustotalop.obsidianauctions.AuctionConfig;
import com.gmail.virustotalop.obsidianauctions.Key;
import com.gmail.virustotalop.obsidianauctions.ObsidianAuctions;
import com.gmail.virustotalop.obsidianauctions.auction.AuctionScope;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;

import javax.inject.Inject;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@ApiStatus.Internal
public class ActionBarManager {

    private final BukkitAudiences adventure;
    private final Map<UUID, String> playerMessages = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> ticksRemaining = new ConcurrentHashMap<>();

    @Inject
    private ActionBarManager(BukkitAudiences adventure) {
        this.adventure = adventure;
        this.runTask();
    }

    public void addPlayer(Player player, String message, AuctionScope auctionScope) {
        int totalTicks = AuctionConfig.getInt(Key.ACTION_BAR_TICKS, auctionScope);
        totalTicks -= 60;
        if (totalTicks > 0) { //60 is default if less than or equal to we will just ignore
            UUID uuid = player.getUniqueId();
            this.ticksRemaining.put(uuid, totalTicks);
            this.playerMessages.put(uuid, message);
        }
    }

    private void runTask() {
        Bukkit.getScheduler().scheduleSyncRepeatingTask(ObsidianAuctions.get(), () -> {
            Iterator<Map.Entry<UUID, Integer>> it = this.ticksRemaining.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<UUID, Integer> next = it.next();
                int ticks = next.getValue();
                ticks -= 1;
                if (ticks % 20 == 0) {
                    UUID uuid = next.getKey();
                    Player player = Bukkit.getServer().getPlayer(uuid);
                    if (player == null) {
                        it.remove();
                        this.playerMessages.remove(uuid);
                    } else {
                        String message = this.playerMessages.get(uuid);
                        this.adventure.player(player).sendActionBar(MiniMessage.miniMessage().deserialize(message));
                    }
                }
                if (ticks == 0) {
                    it.remove();
                } else {
                    next.setValue(ticks);
                }
            }
        }, 1, 1);
    }
}