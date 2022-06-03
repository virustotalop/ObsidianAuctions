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

package com.gmail.virustotalop.obsidianauctions.command;

import cloud.commandframework.exceptions.NoPermissionException;
import com.gmail.virustotalop.obsidianauctions.Key;
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
            this.sendMessage(sender, Key.AUCTION_USE_PERMISSIONS);
        } else if (missing.equals(Permission.AUCTION_TOGGLE)) {
            this.sendMessage(sender, Key.AUCTION_TOGGLE_PERMISSIONS);
        } else if (missing.equals(Permission.AUCTION_START)) {
            this.sendMessage(sender, Key.AUCTION_FAIL_PERMISSIONS);
        } else if (missing.equals(Permission.AUCTION_END)) {
            this.sendMessage(sender, Key.AUCTION_END_PERMISSIONS);
        } else if (missing.equals(Permission.AUCTION_CANCEL)) {
            this.sendMessage(sender, Key.AUCTION_CANCEL_PERMISSIONS);
        } else if (missing.equals(Permission.AUCTION_QUEUE)) {
            this.sendMessage(sender, Key.AUCTION_QUEUE_PERMISSIONS);
        } else if (missing.equals(Permission.AUCTION_INFO)) {
            this.sendMessage(sender, Key.AUCTION_INFO_PERMISSIONS);
        } else if (missing.equals(Permission.AUCTION_ADMIN_RELOAD)) {
            this.sendMessage(sender, Key.PLUGIN_RELOAD_FAIL_PERMISSIONS);
        } else if (missing.equals(Permission.AUCTION_ADMIN_SUSPEND)) {
            this.sendMessage(sender, Key.SUSPENSION_FAIL_PERMISSIONS);
        } else if (missing.equals(Permission.AUCTION_ADMIN_RESUME)) {
            this.sendMessage(sender, Key.UNSUSPENSION_FAIL_PERMISSIONS);
        } else if (missing.equals(Permission.AUCTION_ADMIN_CONFISCATE)) {
            this.sendMessage(sender, Key.CONFISCATE_FAIL_PERMISSIONS);
        } else if (missing.equals(Permission.AUCTION_BID)) { //Handle bid
            this.sendMessage(sender, Key.BID_FAIL_PERMISSIONS);
        }
    }

    private void sendMessage(CommandSender sender, Key key) {
        UUID uuid = null;
        if (sender instanceof Player) {
            uuid = ((Player) sender).getUniqueId();
        }
        this.manager.sendPlayerMessage(key, uuid, (Auction) null);
    }
}
