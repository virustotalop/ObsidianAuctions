package com.gmail.virustotalop.obsidianauctions.command;

import cloud.commandframework.exceptions.NoPermissionException;
import com.gmail.virustotalop.obsidianauctions.Permission;
import com.gmail.virustotalop.obsidianauctions.auction.Auction;
import com.gmail.virustotalop.obsidianauctions.message.AuctionMessageManager;
import com.gmail.virustotalop.obsidianauctions.message.MessageManager;
import com.google.inject.Inject;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.function.BiConsumer;

public class CommandPermissionHandler implements BiConsumer<CommandSender, NoPermissionException> {

    private final MessageManager manager;

    @Inject
    private CommandPermissionHandler(MessageManager manager) {
        this.manager = manager;
    }

    @Override
    public void accept(CommandSender sender, NoPermissionException ex) {
        String missing = ex.getMissingPermission()
                .replace("(", "")
                .replace(")", "");
        System.out.println("missing: " + missing);
        if(missing.equals(Permission.AUCTION_BID)) {
            this.sendMessage(sender, "bid-fail-permissions");
        } else if(missing.equals(Permission.AUCTION_USE)) {
            
        }
    }

    private void sendMessage(CommandSender sender, String key) {
        UUID uuid = null;
        if(sender instanceof Player) {
            uuid = ((Player) sender).getUniqueId();
        }
        System.out.println("Sending player message: " + key);
        this.manager.sendPlayerMessage(key, uuid, (Auction) null);
    }
}
