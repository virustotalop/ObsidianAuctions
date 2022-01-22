package com.gmail.virustotalop.obsidianauctions.message;

import com.gmail.virustotalop.obsidianauctions.ObsidianAuctions;
import com.gmail.virustotalop.obsidianauctions.auction.Auction;
import com.gmail.virustotalop.obsidianauctions.auction.AuctionManager;
import com.gmail.virustotalop.obsidianauctions.auction.AuctionScope;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@ApiStatus.Internal
public class AuctionMessageManager implements MessageManager {

    private final ActionBarManager actionBar;
    private final BukkitAudiences adventure;
    private final AuctionMessageParser parser;
    private final AuctionManager auctionManager;

    @Inject
    private AuctionMessageManager(ActionBarManager actionBar, BukkitAudiences adventure,
                                  AuctionMessageParser parser, AuctionManager auctionManager) {
        this.actionBar = actionBar;
        this.adventure = adventure;
        this.parser = parser;
        this.auctionManager = auctionManager;
    }

    @Override
    public void sendPlayerMessage(String messageKey, UUID playerUUID, Auction auction) {
        List<String> messageKeys = new ArrayList<>();
        messageKeys.add(messageKey);
        this.sendPlayerMessage(messageKeys, playerUUID, auction);
    }

    @Override
    public void sendPlayerMessage(List<String> messageKeys, UUID playerUUID, Auction auction) {
        CommandSender recipient;
        if(playerUUID == null) {
            recipient = Bukkit.getConsoleSender();
        } else {
            recipient = Bukkit.getPlayer(playerUUID);
        }
        AuctionScope auctionScope = null;
        if(auction != null) {
            auctionScope = auction.getScope();
        }
        if(auctionScope == null && recipient instanceof Player) {
            auctionScope = this.auctionManager.getPlayerScope((Player) recipient);
        }
        this.sendMessage(messageKeys, recipient, auctionScope, false);
    }

    @Override
    public void sendPlayerMessage(String messageKey, UUID playerUUID, AuctionScope auctionScope) {
        List<String> messageKeys = new ArrayList<>();
        messageKeys.add(messageKey);
        this.sendPlayerMessage(messageKeys, playerUUID, auctionScope);
    }

    @Override
    public void sendPlayerMessage(List<String> messageKeys, UUID playerUUID, AuctionScope auctionScope) {
        CommandSender recipient;
        if(playerUUID == null) {
            recipient = Bukkit.getConsoleSender();
        } else {
            recipient = Bukkit.getPlayer(playerUUID);
        }
        if(auctionScope == null && recipient instanceof Player) {
            auctionScope = this.auctionManager.getPlayerScope((Player) recipient);
        }
        this.sendMessage(messageKeys, recipient, auctionScope, false);
    }

    @Override
    public void broadcastAuctionMessage(String messageKey, Auction auction) {
        List<String> messageKeys = new ArrayList<>();
        messageKeys.add(messageKey);
        this.broadcastAuctionMessage(messageKeys, auction);
    }

    @Override
    public void broadcastAuctionMessage(List<String> messageKeys, Auction auction) {
        if(auction == null) {
            return;
        }
        AuctionScope auctionScope = auction.getScope();
        this.sendMessage(messageKeys, null, auctionScope, true);
    }

    @Override
    public void broadcastAuctionScopeMessage(String messageKey, AuctionScope auctionScope) {
        List<String> messageKeys = new ArrayList<>();
        messageKeys.add(messageKey);
        this.broadcastAuctionScopeMessage(messageKeys, auctionScope);
    }

    @Override
    public void broadcastAuctionScopeMessage(List<String> messageKeys, AuctionScope auctionScope) {
        this.sendMessage(messageKeys, null, auctionScope, true);
    }

    /**
     * Sends a message to a player or scope.
     *
     * @param messageKeys   keys to message in language.yml
     * @param sender        focused player
     * @param auctionScope  focused scope
     * @param fullBroadcast whether to broadcast or send to player
     */
    private void sendMessage(List<String> messageKeys, CommandSender sender, AuctionScope auctionScope, boolean fullBroadcast) {
        Auction auction = null;
        Player player = null;

        if(auctionScope != null) {
            auction = auctionScope.getActiveAuction();
        }

        if(sender != null) {
            if(sender instanceof Player) {
                player = (Player) sender;
                if(!fullBroadcast && ObsidianAuctions.get().isVoluntarilyDisabled(player.getUniqueId())) {
                    // Don't send this user any messages.
                    return;
                }
            }
        }

        List<String> messages = this.parser.parseMessages(messageKeys, auctionScope, auction, player, fullBroadcast);

        if(fullBroadcast) {
            broadcastMessage(messages, auctionScope);
        } else if(player != null) {
            for(String message : messages) {
                this.adventure.player(player).sendMessage(MiniMessage.miniMessage().deserialize(message));
                ObsidianAuctions.get().log(player.getName(), this.stripTags(message), auctionScope);
            }
        } else if(sender != null) {
            ConsoleCommandSender console = Bukkit.getConsoleSender();
            for(String message : messages) {
                String stripped = this.stripTags(message);
                console.sendMessage(stripped);
                ObsidianAuctions.get().log("CONSOLE", stripped, auctionScope);
            }
        } else {
            for(String message : messages) {
                ObsidianAuctions.get().log("NO TARGET!", this.stripTags(message), auctionScope);
            }
        }
    }

    /**
     * Broadcast a message to everyone in an auctionScope.
     *
     * @param messages     messages to send
     * @param auctionScope scope to send it to
     */
    private void broadcastMessage(List<String> messages, AuctionScope auctionScope) {
        Collection<? extends Player> onlinePlayers = Bukkit.getServer().getOnlinePlayers();
        for(Player player : onlinePlayers) {
            if(ObsidianAuctions.get().isVoluntarilyDisabled(player.getUniqueId())) {
                continue;
            } else if(auctionScope != null && !auctionScope.equals(this.auctionManager.getPlayerScope(player))) {
                continue;
            }

            Audience audience = this.adventure.player(player);
            for(String message : messages) {
                if(ObsidianAuctions.enableChatMessages) {
                    audience.sendMessage(MiniMessage.miniMessage().deserialize(message));
                }
                if(ObsidianAuctions.enableActionbarMessages) {
                    audience.sendActionBar(MiniMessage.miniMessage().deserialize(message));
                    this.actionBar.addPlayer(player, message);
                }
            }
        }
        for(String message : messages) {
            String stripped = this.stripTags(message);
            Bukkit.getConsoleSender().sendMessage(stripped);
            ObsidianAuctions.get().log("BROADCAST", stripped, auctionScope);
        }
    }

    private String stripTags(String message) {
        return MiniMessage.miniMessage().stripTokens(message);
    }
}