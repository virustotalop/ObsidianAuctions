package com.gmail.virustotalop.obsidianauctions.command;

import cloud.commandframework.exceptions.NoPermissionException;
import com.gmail.virustotalop.obsidianauctions.Permission;
import com.gmail.virustotalop.obsidianauctions.auction.Auction;
import com.gmail.virustotalop.obsidianauctions.message.MessageManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import javax.inject.Inject;
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
        if (missing.contains("|") || missing.equals(Permission.AUCTION_USE)) {
            this.sendMessage(sender, "auction-use-permissions");
        } else if (missing.equals(Permission.AUCTION_TOGGLE)) {
            this.sendMessage(sender, "auction-toggle-permissions");
        } else if (missing.equals(Permission.AUCTION_START)) {
            this.sendMessage(sender, "auction-fail-permissions");
        } else if (missing.equals(Permission.AUCTION_END)) {
            this.sendMessage(sender, "auction-end-permissions");
        } else if (missing.equals(Permission.AUCTION_CANCEL)) {
            this.sendMessage(sender, "auction-cancel-permissions");
        } else if (missing.equals(Permission.AUCTION_QUEUE)) {
            this.sendMessage(sender, "auction-queue-permissions");
        } else if (missing.equals(Permission.AUCTION_INFO)) {
            this.sendMessage(sender, "auction-info-permissions");
        } else if (missing.equals(Permission.AUCTION_ADMIN_RELOAD)) {
            this.sendMessage(sender, "plugin-reload-fail-permissions");
        } else if (missing.equals(Permission.AUCTION_ADMIN_SUSPEND)) {
            this.sendMessage(sender, "suspension-fail-permissions");
        } else if (missing.equals(Permission.AUCTION_ADMIN_RESUME)) {
            this.sendMessage(sender, "unsuspension-fail-permissions");
        } else if (missing.equals(Permission.AUCTION_ADMIN_CONFISCATE)) {
            this.sendMessage(sender, "confiscate-fail-permissions");
        } else if (missing.equals(Permission.AUCTION_BID)) { //Handle bid
            this.sendMessage(sender, "bid-fail-permissions");
        }
    }

    private void sendMessage(CommandSender sender, String key) {
        UUID uuid = null;
        if (sender instanceof Player) {
            uuid = ((Player) sender).getUniqueId();
        }
        this.manager.sendPlayerMessage(key, uuid, (Auction) null);
    }
}
