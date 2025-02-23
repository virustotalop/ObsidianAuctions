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

import com.gmail.virustotalop.obsidianauctions.AuctionConfig;
import com.gmail.virustotalop.obsidianauctions.Key;
import com.gmail.virustotalop.obsidianauctions.ObsidianAuctions;
import com.gmail.virustotalop.obsidianauctions.Permissions;
import com.gmail.virustotalop.obsidianauctions.auction.Auction;
import com.gmail.virustotalop.obsidianauctions.auction.AuctionManager;
import com.gmail.virustotalop.obsidianauctions.auction.AuctionScope;
import com.gmail.virustotalop.obsidianauctions.inventory.QueueInventoryHolder;
import com.gmail.virustotalop.obsidianauctions.message.MessageManager;
import com.gmail.virustotalop.obsidianauctions.util.LegacyUtil;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.annotations.exception.ExceptionHandler;
import org.incendo.cloud.exception.NoPermissionException;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AuctionCommands {

    private final ObsidianAuctions plugin;
    private final MessageManager message;
    private final AuctionManager auctionManager;

    @Inject
    private AuctionCommands(ObsidianAuctions plugin, MessageManager messageManager, AuctionManager auctionManager) {
        this.plugin = plugin;
        this.message = messageManager;
        this.auctionManager = auctionManager;
    }

    @Command("auction|auc")
    @Permission(Permissions.AUCTION_USE)
    public void auction(CommandSender sender) {
        UUID uuid = this.uuidFromSender(sender);
        this.message.sendPlayerMessage(Key.AUCTION_HELP, uuid, (AuctionScope) null);
    }

    @Command("auction|auc help")
    @Permission(Permissions.AUCTION_USE)
    public void auctionHelp(CommandSender sender) {
        UUID uuid = this.uuidFromSender(sender);
        this.message.sendPlayerMessage(Key.AUCTION_HELP, uuid, (AuctionScope) null);
    }

    @Command("auction|auc on")
    @Permission(Permissions.AUCTION_TOGGLE)
    public void auctionOn(CommandSender sender) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            UUID uuid = player.getUniqueId();
            if (this.plugin.removeVoluntarilyDisabled(uuid)) {
                this.message.sendPlayerMessage(Key.AUCTION_ENABLED, uuid, (AuctionScope) null);
                this.plugin.saveVoluntarilyDisabled();
            }
        }
    }

    @Command("auction|auc off|quiet|ignore|silent|silence")
    @Permission(Permissions.AUCTION_TOGGLE)
    public void auctionOff(CommandSender sender) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            UUID uuid = player.getUniqueId();
            if (!this.plugin.isVoluntarilyDisabled(uuid)) {
                this.message.sendPlayerMessage(Key.AUCTION_DISABLED, uuid, (AuctionScope) null);
                this.plugin.addVoluntarilyDisabled(uuid);
                this.plugin.saveVoluntarilyDisabled();
            }
        }
    }

    @Command("auction|auc start <quantity> <price> <increment> <time> [buynow] [sealed]")
    @Permission(Permissions.AUCTION_START)
    public void auctionStart(CommandSender sender, @Argument("quantity") String quantity,
                             @Argument("price") String price, @Argument("increment") String increment,
                             @Argument("time") String time, @Argument("buynow") String buyNow,
                             @Argument("sealed") String sealedStr) {
        if (this.canAuction(sender)) {
            if (buyNow == null) {
                buyNow = "0";
            }
            boolean sealed = false;
            if (sealedStr != null) {
                String lower = sealedStr.toLowerCase();
                if (lower.equals("sealed") || lower.equals("yes") || lower.equals("true")) {
                    sealed = true;
                }
            }

            UUID uuid = this.uuidFromSender(sender);
            Player player = this.plugin.getServer().getPlayer(uuid);
            AuctionScope userScope = this.auctionManager.getPlayerScope(player);
            if (!AuctionConfig.getBoolean(Key.ALLOW_SEALED_AUCTIONS, userScope) && sealed) {
                this.message.sendPlayerMessage(Key.AUCTION_FAIL_NO_SEALED_AUCTION, uuid, (AuctionScope) null);
                return;
            }
            if (!AuctionConfig.getBoolean(Key.ALLOW_UNSEALED_AUCTIONS, userScope) && !sealed) {
                sealed = true;
            }
            ItemStack hand = LegacyUtil.getItemInMainHand(player).clone();
            String[] args = {quantity, price, increment, time, buyNow};
            userScope.queueAuction(new Auction(this.plugin, player, args, userScope, sealed, this.message, hand));
        }
    }

    @Command("auction|auc end|e")
    @Permission(Permissions.AUCTION_END)
    public void auctionEnd(CommandSender sender) {
        UUID uuid = this.uuidFromSender(sender);
        if (uuid != null) {
            Player player = this.plugin.getServer().getPlayer(uuid);
            AuctionScope userScope = this.auctionManager.getPlayerScope(player);
            if (userScope == null) {
                this.message.sendPlayerMessage(Key.AUCTION_FAIL_NO_AUCTION_EXISTS, uuid, (AuctionScope) null);
            } else {
                Auction auction = userScope.getActiveAuction();
                if (auction == null) {
                    this.message.sendPlayerMessage(Key.AUCTION_FAIL_NO_AUCTION_EXISTS, uuid, (AuctionScope) null);
                } else {
                    if (!AuctionConfig.getBoolean(Key.ALLOW_EARLY_END, userScope)) {
                        this.message.sendPlayerMessage(Key.AUCTION_FAIL_NO_EARLY_END, uuid, (AuctionScope) null);
                    } else if (!player.getUniqueId().equals(auction.getOwnerUUID())) {
                        this.message.sendPlayerMessage(Key.AUCTION_FAIL_NOT_OWNER_END, uuid, (AuctionScope) null);
                    } else {
                        auction.end();
                    }
                }
            }
        }
    }

    @Command("auction|auc cancel|c")
    @Permission(Permissions.AUCTION_CANCEL)
    public void auctionCancel(CommandSender sender) {
        UUID uuid = this.uuidFromSender(sender);
        if (uuid == null) {
            this.message.sendPlayerMessage(Key.AUCTION_FAIL_NO_SCOPE, null, (AuctionScope) null);
        } else {
            Player player = this.plugin.getServer().getPlayer(uuid);
            UUID playerUUID = player.getUniqueId();
            AuctionScope userScope = this.auctionManager.getPlayerScope(player);
            if (userScope == null) {
                this.message.sendPlayerMessage(Key.AUCTION_FAIL_NO_SCOPE, uuid, (AuctionScope) null);
                return;
            }
            if (userScope.getActiveAuction() == null && userScope.getAuctionQueueLength() == 0) {
                this.message.sendPlayerMessage(Key.AUCTION_FAIL_NO_AUCTION_EXISTS, uuid, (AuctionScope) null);
                return;
            }
            List<Auction> auctionQueue = userScope.getAuctionQueue();
            for (int i = 0; i < auctionQueue.size(); i++) {
                if (auctionQueue.get(i).getOwnerUUID().equals(playerUUID)) {
                    auctionQueue.remove(i);
                    this.message.sendPlayerMessage(Key.AUCTION_CANCEL_QUEUED, uuid, (AuctionScope) null);
                    return;
                }
            }
            Auction auction = userScope.getActiveAuction();
            if (auction == null) {
                this.message.sendPlayerMessage(Key.AUCTION_FAIL_NO_AUCTION_EXISTS, uuid, (AuctionScope) null);
                return;
            }
            if (uuid.equals(auction.getOwnerUUID()) || player.hasPermission(Permissions.AUCTION_ADMIN_CANCEL)) {
                if (AuctionConfig.getInt(Key.CANCEL_PREVENTION_SECONDS, userScope) > auction.getRemainingTime() || AuctionConfig.getDouble(Key.CANCEL_PREVENTION_PERCENT, userScope) > (double) auction.getRemainingTime() / (double) auction.getTotalTime() * 100D) {
                    this.message.sendPlayerMessage(Key.AUCTION_FAIL_CANCEL_PREVENTION, uuid, (AuctionScope) null);
                } else {
                    auction.cancel();
                }
            } else {
                this.message.sendPlayerMessage(Key.AUCTION_FAIL_NOT_OWNER_CANCEL, uuid, (AuctionScope) null);
            }
        }
    }

    @Command("auction|auc queue|q")
    @Permission(Permissions.AUCTION_QUEUE)
    public void auctionQueue(CommandSender sender) {
        UUID uuid = this.uuidFromSender(sender);
        if (uuid != null) {
            Player player = this.plugin.getServer().getPlayer(uuid);
            AuctionScope userScope = this.auctionManager.getPlayerScope(player);
            if (userScope != null) {
                List<Auction> auctionQueue = new ArrayList<>(userScope.getAuctionQueue()); //Copy queue
                Auction activeAuction = userScope.getActiveAuction();
                if (activeAuction != null) { //Add current auction to copied queue if available
                    auctionQueue.add(0, activeAuction);
                }
                if (auctionQueue.isEmpty()) {
                    this.message.sendPlayerMessage(Key.AUCTION_QUEUE_STATUS_NOT_IN_QUEUE, uuid, (AuctionScope) null);
                } else {
                    InventoryHolder holder = new QueueInventoryHolder(userScope);
                    Inventory inv = holder.getInventory();
                    for (int i = 0; i < auctionQueue.size(); i++) {
                        if (i == inv.getSize()) {
                            break;
                        }
                        inv.setItem(i, auctionQueue.get(i).getGuiItem());
                    }
                    player.openInventory(inv);
                }
            }
        }
    }

    @Command("auction|auc info|i")
    @Permission(Permissions.AUCTION_INFO)
    public void auctionInfo(CommandSender sender) {
        UUID uuid = this.uuidFromSender(sender);
        if (uuid != null) {
            Player player = this.plugin.getServer().getPlayer(uuid);
            AuctionScope userScope = this.auctionManager.getPlayerScope(player);
            if (userScope != null) {
                Auction auction = userScope.getActiveAuction();
                if (auction == null) {
                    this.message.sendPlayerMessage(Key.AUCTION_INFO_NO_AUCTION, uuid, (AuctionScope) null);
                } else {
                    auction.info(sender, false);
                }
            }
        }
    }

    @Command("auction|auc reload")
    @Permission(Permissions.AUCTION_ADMIN_RELOAD)
    public void auctionReload(CommandSender sender) {
        UUID uuid = this.uuidFromSender(sender);
        if (this.auctionManager.areAuctionsRunning()) { // Don't reload if any auctions are running
            this.message.sendPlayerMessage(Key.PLUGIN_RELOAD_FAIL_AUCTIONS_RUNNING, uuid, (AuctionScope) null);
        } else {
            this.plugin.loadConfig();
            this.message.sendPlayerMessage(Key.PLUGIN_RELOADED, uuid, (AuctionScope) null);
        }
    }

    @Command("auction|auc suspend [player]")
    @Permission(Permissions.AUCTION_ADMIN_SUSPEND)
    public void auctionSuspend(CommandSender sender, @Argument("player") String playerName) {
        UUID uuid = this.uuidFromSender(sender);
        if (playerName != null) {
            Player player = this.plugin.getServer().getPlayer(playerName);
            if (player == null) {
                this.message.sendPlayerMessage(Key.SUSPENSION_USER_FAIL_IS_OFFLINE, uuid, (AuctionScope) null);
            } else if (player.hasPermission(Permissions.AUCTION_ADMIN_SUSPEND)) {
                this.message.sendPlayerMessage(Key.SUSPENSION_USER_IS_ADMIN, uuid, (AuctionScope) null);
            } else if (this.plugin.isSuspendedUser(player.getUniqueId())) {
                this.message.sendPlayerMessage(Key.SUSPENSION_USER_FAIL_ALREADY_SUSPENDED, uuid, (AuctionScope) null);
            } else {
                this.plugin.addSuspendedUser(player.getUniqueId());
                this.plugin.saveSuspendedUsers();
                this.message.sendPlayerMessage(Key.SUSPENSION_USER, uuid, (AuctionScope) null);
                this.message.sendPlayerMessage(Key.SUSPENSION_USER_SUCCESS, uuid, (AuctionScope) null);
            }
        } else {
            this.plugin.setSuspendAllAuctions(true);
            this.auctionManager.cancelAllAuctions();
            this.message.broadcastAuctionScopeMessage(Key.SUSPENSION_GLOBAL, null);
        }
    }

    @Command("auction|auc resume [player]")
    @Permission(Permissions.AUCTION_ADMIN_RESUME)
    public void auctionResume(CommandSender sender, @Argument("player") String playerName) {
        UUID uuid = this.uuidFromSender(sender);
        if (playerName != null) {
            Player player = this.plugin.getServer().getPlayer(playerName);
            if (player == null) {
                this.message.sendPlayerMessage(Key.UNSUSPENSION_USER_FAIL_IS_OFFLINE, uuid, (AuctionScope) null);
            } else if (player.hasPermission(Permissions.AUCTION_ADMIN_RESUME)) {
                this.message.sendPlayerMessage(Key.UNSUSPENSION_FAIL_PERMISSIONS, uuid, (AuctionScope) null);
            } else if (!this.plugin.isSuspendedUser(player.getUniqueId())) {
                this.message.sendPlayerMessage(Key.UNSUSPENSION_USER_FAIL_NOT_SUSPENDED, uuid, (AuctionScope) null);
            } else {
                this.plugin.removeSuspendedUser(player.getUniqueId());
                this.plugin.saveSuspendedUsers();
                this.message.sendPlayerMessage(Key.UNSUSPENSION_USER, uuid, (AuctionScope) null);
                this.message.sendPlayerMessage(Key.UNSUSPENSION_USER_SUCCESS, uuid, (AuctionScope) null);
            }
        } else {
            this.plugin.setSuspendAllAuctions(false);
            this.message.broadcastAuctionScopeMessage(Key.UNSUSPENSION_GLOBAL, null);
        }
    }

    @Command("auction|auc confiscate|impound")
    @Permission(Permissions.AUCTION_ADMIN_CONFISCATE)
    public void auctionConfiscate(CommandSender sender) {
        UUID uuid = this.uuidFromSender(sender);
        if (uuid != null) {
            Player player = this.plugin.getServer().getPlayer(uuid);
            AuctionScope userScope = this.auctionManager.getPlayerScope(player);
            if (userScope == null) {
                this.message.sendPlayerMessage(Key.AUCTION_FAIL_NO_AUCTION_EXISTS, uuid, (AuctionScope) null);
            } else {
                Auction auction = userScope.getActiveAuction();
                if (auction == null) {
                    this.message.sendPlayerMessage(Key.AUCTION_FAIL_NO_AUCTION_EXISTS, uuid, (AuctionScope) null);
                } else {
                    if (uuid.equals(auction.getOwnerUUID())) {
                        this.message.sendPlayerMessage(Key.CONFISCATE_FAIL_SELF, uuid, (AuctionScope) null);
                    } else {
                        auction.confiscate(player);
                    }
                }
            }
        } else {
            this.message.sendPlayerMessage(Key.CONFISCATE_FAIL_CONSOLE, null, (AuctionScope) null);
        }
    }

    @Command("bid <bid> <maxbid>")
    @Permission(Permissions.AUCTION_BID)
    public void bid(CommandSender sender, @Argument("bid") String bid, @Argument("maxbid") String maxBid) {
        if (this.canBid(sender)) {
            Player player = (Player) sender;
            AuctionScope userScope = this.auctionManager.getPlayerScope(player);
            Auction auction = userScope.getActiveAuction();
            String[] args = {bid, maxBid};
            auction.bid(player, args);
        }
    }

    private UUID uuidFromSender(CommandSender sender) {
        UUID uuid = null;
        if (sender instanceof Player) {
            uuid = ((Player) sender).getUniqueId();
        }
        return uuid;
    }

    private boolean canAuction(CommandSender sender) {
        if (!this.preAuctionLogic(sender, CommandType.AUCTION)) {
            return false;
        }
        UUID uuid = this.uuidFromSender(sender);
        Player player = this.plugin.getServer().getPlayer(uuid);
        AuctionScope userScope = this.auctionManager.getPlayerScope(player);
        if (!AuctionConfig.getBoolean(Key.ALLOW_SEALED_AUCTIONS, userScope) && !AuctionConfig.getBoolean(Key.ALLOW_UNSEALED_AUCTIONS, userScope)) {
            this.message.sendPlayerMessage(Key.AUCTION_FAIL_NO_AUCTIONS_ALLOWED, uuid, (AuctionScope) null);
            return false;
        } else if (LegacyUtil.getItemInMainHand(player) == null || LegacyUtil.getItemInMainHand(player).getAmount() == 0) {
            this.message.sendPlayerMessage(Key.AUCTION_FAIL_HAND_IS_EMPTY, uuid, (AuctionScope) null);
            return false;
        }
        return true;
    }

    private boolean canBid(CommandSender sender) {
        if (!this.preAuctionLogic(sender, CommandType.BID)) {
            return false;
        }
        UUID uuid = this.uuidFromSender(sender);
        Player player = (Player) sender;
        AuctionScope scope = this.auctionManager.getPlayerScope(player);
        boolean active = scope.getActiveAuction() != null;
        if (!active) {
            this.message.sendPlayerMessage(Key.BID_FAIL_NO_AUCTION, uuid, (AuctionScope) null);
        }
        return active;
    }

    private boolean preAuctionLogic(CommandSender sender, CommandType type) {
        UUID uuid = this.uuidFromSender(sender);
        if (this.plugin.getSuspendAllAuctions()) {
            this.message.sendPlayerMessage(Key.SUSPENSION_GLOBAL, uuid, (AuctionScope) null);
            return false;
        } else if (uuid == null) {
            if (type == CommandType.AUCTION) {
                this.message.sendPlayerMessage(Key.AUCTION_FAIL_CONSOLE, null, (AuctionScope) null);
            } else {
                this.message.sendPlayerMessage(Key.BID_FAIL_CONSOLE, null, (AuctionScope) null);
            }
            return false;
        } else if (this.plugin.isVoluntarilyDisabled(uuid)) {
            this.message.sendPlayerMessage(Key.AUCTION_FAIL_DISABLED, uuid, (AuctionScope) null);
            return false;
        } else if (this.plugin.isSuspendedUser(uuid)) {
            this.message.sendPlayerMessage(Key.SUSPENSION_USER, uuid, (AuctionScope) null);
            return false;
        }
        Player player = this.plugin.getServer().getPlayer(uuid);
        AuctionScope userScope = this.auctionManager.getPlayerScope(player);
        if (!AuctionConfig.getBoolean(Key.ALLOW_GAMEMODE_CREATIVE, userScope) && player.getGameMode() == GameMode.CREATIVE) {
            if (type == CommandType.AUCTION) {
                this.message.sendPlayerMessage(Key.AUCTION_FAIL_GAMEMODE_CREATIVE, uuid, (AuctionScope) null);
            } else {
                this.message.sendPlayerMessage(Key.BID_FAIL_GAMEMODE_CREATIVE, uuid, (AuctionScope) null);
            }
            return false;
        } else if (userScope == null) {
            this.message.sendPlayerMessage(Key.AUCTION_FAIL_NO_SCOPE, uuid, (AuctionScope) null);
            return false;
        }
        return true;
    }

    @ExceptionHandler(NoPermissionException.class)
    public void handleNoPermission(CommandSender sender, NoPermissionException ex) {
        String missing = ex.missingPermission().permissionString()
                .replace("(", "")
                .replace(")", "");
        if (missing.contains("|") || missing.equals(Permissions.AUCTION_USE)) {
            this.sendMessage(sender, Key.AUCTION_USE_PERMISSIONS);
        } else if (missing.equals(Permissions.AUCTION_TOGGLE)) {
            this.sendMessage(sender, Key.AUCTION_TOGGLE_PERMISSIONS);
        } else if (missing.equals(Permissions.AUCTION_START)) {
            this.sendMessage(sender, Key.AUCTION_FAIL_PERMISSIONS);
        } else if (missing.equals(Permissions.AUCTION_END)) {
            this.sendMessage(sender, Key.AUCTION_END_PERMISSIONS);
        } else if (missing.equals(Permissions.AUCTION_CANCEL)) {
            this.sendMessage(sender, Key.AUCTION_CANCEL_PERMISSIONS);
        } else if (missing.equals(Permissions.AUCTION_QUEUE)) {
            this.sendMessage(sender, Key.AUCTION_QUEUE_PERMISSIONS);
        } else if (missing.equals(Permissions.AUCTION_INFO)) {
            this.sendMessage(sender, Key.AUCTION_INFO_PERMISSIONS);
        } else if (missing.equals(Permissions.AUCTION_ADMIN_RELOAD)) {
            this.sendMessage(sender, Key.PLUGIN_RELOAD_FAIL_PERMISSIONS);
        } else if (missing.equals(Permissions.AUCTION_ADMIN_SUSPEND)) {
            this.sendMessage(sender, Key.SUSPENSION_FAIL_PERMISSIONS);
        } else if (missing.equals(Permissions.AUCTION_ADMIN_RESUME)) {
            this.sendMessage(sender, Key.UNSUSPENSION_FAIL_PERMISSIONS);
        } else if (missing.equals(Permissions.AUCTION_ADMIN_CONFISCATE)) {
            this.sendMessage(sender, Key.CONFISCATE_FAIL_PERMISSIONS);
        } else if (missing.equals(Permissions.AUCTION_BID)) { //Handle bid
            this.sendMessage(sender, Key.BID_FAIL_PERMISSIONS);
        }
    }

    private void sendMessage(CommandSender sender, Key key) {
        UUID uuid = null;
        if (sender instanceof Player) {
            uuid = ((Player) sender).getUniqueId();
        }
        this.message.sendPlayerMessage(key, uuid, (Auction) null);
    }

    private enum CommandType {

        AUCTION,
        BID

    }
}