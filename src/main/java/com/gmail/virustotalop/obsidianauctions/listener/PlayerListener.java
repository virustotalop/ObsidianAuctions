package com.gmail.virustotalop.obsidianauctions.listener;

import com.gmail.virustotalop.obsidianauctions.AuctionConfig;
import com.gmail.virustotalop.obsidianauctions.ObsidianAuctions;
import com.gmail.virustotalop.obsidianauctions.auction.Auction;
import com.gmail.virustotalop.obsidianauctions.auction.AuctionLocationManager;
import com.gmail.virustotalop.obsidianauctions.auction.AuctionManager;
import com.gmail.virustotalop.obsidianauctions.auction.AuctionScope;
import com.gmail.virustotalop.obsidianauctions.message.MessageManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import javax.inject.Inject;
import java.util.List;
import java.util.UUID;

public class PlayerListener implements Listener {

    private final MessageManager message;
    private final AuctionManager auctionManager;
    private final AuctionLocationManager locationManager;
    private final ObsidianAuctions plugin;

    @Inject
    private PlayerListener(MessageManager message, AuctionManager auctionManager,
                           AuctionLocationManager locationManager, ObsidianAuctions plugin) {
        this.message = message;
        this.auctionManager = auctionManager;
        this.locationManager = locationManager;
        this.plugin = plugin;
    }

    @EventHandler
    public void playerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        this.plugin.killOrphan(player);
        this.auctionManager.sendWelcomeMessage(player, true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        // Hopefully the teleport and portal things I just added will make this obsolete, but I figure I'll keep it just to make sure.
        this.locationManager.forceLocation(event.getPlayer().getUniqueId(), null);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerChangedGameMode(PlayerGameModeChangeEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();
        AuctionScope playerScope = this.auctionManager.getPlayerScope(player);
        Auction playerAuction = ObsidianAuctions.get().getPlayerAuction(player);
        if (AuctionConfig.getBoolean("allow-gamemode-change", playerScope) || playerAuction == null) {
            return;
        }

        if (ObsidianAuctions.get().getAuctionManager().isParticipant(playerUUID)) {
            event.setCancelled(true);
            this.message.sendPlayerMessage("gamemodechange-fail-participating", playerUUID, (AuctionScope) null);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerPreprocessCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }
        UUID playerUUID = player.getUniqueId();
        String message = event.getMessage();
        if (message == null || message.isEmpty()) {
            return;
        }

        AuctionScope playerScope = this.auctionManager.getPlayerScope(player);

        // Check inscope disabled commands, doesn't matter if participating:
        List<String> disabledCommands = AuctionConfig.getStringList("disabled-commands-inscope", playerScope);
        for (String disabledCommand : disabledCommands) {
            if (disabledCommand.isEmpty()) continue;
            if (message.toLowerCase().startsWith(disabledCommand.toLowerCase())) {
                event.setCancelled(true);
                this.message.sendPlayerMessage("disabled-command-inscope", playerUUID, (AuctionScope) null);
                return;
            }
        }

        // Check participating disabled commands
        if (playerScope == null) {
            return;
        }
        if (!ObsidianAuctions.get().getAuctionManager().isParticipant(playerUUID)) {
            return;
        }

        disabledCommands = AuctionConfig.getStringList("disabled-commands-participating", playerScope);
        for (String disabledCommand : disabledCommands) {
            if (disabledCommand.isEmpty()) {
                continue;
            }
            if (message.toLowerCase().startsWith(disabledCommand.toLowerCase())) {
                event.setCancelled(true);
                this.message.sendPlayerMessage("disabled-command-participating", playerUUID, (AuctionScope) null);
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        this.locationManager.forceLocation(event.getPlayer().getUniqueId(), event.getTo());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (!this.locationManager.checkTeleportLocation(event.getPlayer().getUniqueId(), event.getTo()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerPortalEvent(PlayerPortalEvent event) {
        if (!this.locationManager.checkTeleportLocation(event.getPlayer().getUniqueId(), event.getTo()))
            event.setCancelled(true);
    }
}