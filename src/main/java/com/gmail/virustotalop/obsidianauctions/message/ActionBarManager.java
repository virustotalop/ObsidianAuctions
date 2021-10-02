package com.gmail.virustotalop.obsidianauctions.message;

import com.gmail.virustotalop.obsidianauctions.ObsidianAuctions;
import com.google.inject.Inject;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;

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

    public void addPlayer(Player player, String message) {
        int totalTicks = ObsidianAuctions.actionBarTicks;
        totalTicks -= 60;
        if(totalTicks > 0) { //60 is default if less than or equal to we will just ignore
            UUID uuid = player.getUniqueId();
            this.ticksRemaining.put(uuid, totalTicks);
            this.playerMessages.put(uuid, message);
        }
    }

    private void runTask() {
        Bukkit.getScheduler().scheduleSyncRepeatingTask(ObsidianAuctions.get(), () -> {
            Iterator<Map.Entry<UUID, Integer>> it = this.ticksRemaining.entrySet().iterator();
            while(it.hasNext()) {
                Map.Entry<UUID, Integer> next = it.next();
                int ticks = next.getValue();
                ticks -= 1;
                if(ticks % 20 == 0) {
                    UUID uuid = next.getKey();
                    Player player = Bukkit.getServer().getPlayer(uuid);
                    if(player == null) {
                        it.remove();
                        this.playerMessages.remove(uuid);
                    } else {
                        String message = this.playerMessages.get(uuid);
                        this.adventure.player(player).sendActionBar(MiniMessage.miniMessage().parse(message));
                    }
                }
                if(ticks == 0) {
                    it.remove();
                } else {
                    next.setValue(ticks);
                }
            }
        }, 1, 1);
    }
}