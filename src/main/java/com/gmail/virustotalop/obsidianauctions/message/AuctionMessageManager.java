package com.gmail.virustotalop.obsidianauctions.message;

import com.gmail.virustotalop.obsidianauctions.AuctionConfig;
import com.gmail.virustotalop.obsidianauctions.ObsidianAuctions;
import com.gmail.virustotalop.obsidianauctions.auction.Auction;
import com.gmail.virustotalop.obsidianauctions.auction.AuctionBid;
import com.gmail.virustotalop.obsidianauctions.auction.AuctionScope;
import com.gmail.virustotalop.obsidianauctions.language.TranslationFactory;
import com.gmail.virustotalop.obsidianauctions.util.Functions;
import com.gmail.virustotalop.obsidianauctions.util.Items;
import com.gmail.virustotalop.obsidianauctions.util.PlaceholderAPIUtil;
import com.google.inject.Inject;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

public class AuctionMessageManager extends MessageManager {

    private final BukkitAudiences adventure;
    private final AuctionMessageParser parser;

    @Inject
    private AuctionMessageManager(BukkitAudiences adventure, AuctionMessageParser parser) {
        this.adventure = adventure;
        this.parser = parser;
    }

    @Override
    public void sendPlayerMessage(String messageKey, UUID playerUUID, Auction auction) {
        List<String> messageKeys = new ArrayList<>();
        messageKeys.add(messageKey);
        this.sendPlayerMessage(messageKeys, playerUUID, auction);
    }

    @Override
    public void sendPlayerMessage(List<String> messageKeys, UUID playerUUID, Auction auction) {
        CommandSender recipient = null;
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
            auctionScope = AuctionScope.getPlayerScope((Player) recipient);
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
        CommandSender recipient = null;
        if(playerUUID == null) {
            recipient = Bukkit.getConsoleSender();
        } else {
            recipient = Bukkit.getPlayer(playerUUID);
        }
        if(auctionScope == null && recipient instanceof Player) {
            auctionScope = AuctionScope.getPlayerScope((Player) recipient);
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
                this.adventure.player(player).sendMessage(MiniMessage.get().parse(message));
                ObsidianAuctions.get().log(player.getName(), message, auctionScope);
            }
        } else if(sender != null) {
            ConsoleCommandSender console = Bukkit.getConsoleSender();
            for(String message : messages) {
                console.sendMessage(ChatColor.stripColor(GsonComponentSerializer
                        .gson()
                        .serialize(MiniMessage.get().parse(message))));
                ObsidianAuctions.get().log("CONSOLE", message, auctionScope);
            }
        } else {
            for(String message : messages) {
                ObsidianAuctions.get().log("NO TARGET!", message, auctionScope);
            }
        }
    }

    /**
     * Broadcast a message to everyone in an auctionscope.
     *
     * @param messages     messages to send
     * @param auctionScope scope to send it to
     */
    private void broadcastMessage(List<String> messages, AuctionScope auctionScope) {
        Collection<? extends Player> onlinePlayers = Bukkit.getServer().getOnlinePlayers();
        for(Player player : onlinePlayers) {
            if(ObsidianAuctions.get().isVoluntarilyDisabled(player.getUniqueId())) {
                continue;
            } else if(auctionScope != null && !auctionScope.equals(AuctionScope.getPlayerScope(player))) {
                continue;
            }

            for(String message : messages) {
                if(ObsidianAuctions.enableChatMessages) {
                    this.adventure.player(player).sendMessage(MiniMessage.get().parse(message));
                }
                if(ObsidianAuctions.enableActionbarMessages) {
                    this.adventure.player(player).sendActionBar(MiniMessage.get().parse(message));
                }
            }
        }
        for(String message : messages) {
            message = MiniMessage.get().stripTokens(message);
            Bukkit.getConsoleSender().sendMessage(message);
            ObsidianAuctions.get().log("BROADCAST", message, auctionScope);
        }
    }
}