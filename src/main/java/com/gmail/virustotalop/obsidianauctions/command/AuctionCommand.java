package com.gmail.virustotalop.obsidianauctions.command;

import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import com.gmail.virustotalop.obsidianauctions.Permission;
import org.bukkit.command.CommandSender;

public class AuctionCommand {

    @CommandMethod("auction|auc")
    @CommandPermission(Permission.AUCTION_USE)
    public void auction(CommandSender sender) {

    }

    @CommandMethod("auction start [quantity] [price] [increment] [buynow]")
    @CommandPermission(Permission.AUCTION_START)
    public void start(CommandSender sender, Integer quantity, Long price, Long increment, Long buyNow) {

    }

    @CommandMethod("auction end")
    @CommandPermission(Permission.AUCTION_END)
    public void end(CommandSender sender) {

    }
}