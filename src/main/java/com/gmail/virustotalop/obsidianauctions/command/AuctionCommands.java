package com.gmail.virustotalop.obsidianauctions.command;

import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import com.gmail.virustotalop.obsidianauctions.AuctionConfig;
import com.gmail.virustotalop.obsidianauctions.ObsidianAuctions;
import com.gmail.virustotalop.obsidianauctions.Permission;
import com.gmail.virustotalop.obsidianauctions.auction.Auction;
import com.gmail.virustotalop.obsidianauctions.auction.AuctionManager;
import com.gmail.virustotalop.obsidianauctions.auction.AuctionScope;
import com.gmail.virustotalop.obsidianauctions.message.MessageManager;
import com.gmail.virustotalop.obsidianauctions.util.LegacyUtil;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import javax.inject.Inject;
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

    @CommandMethod("auction|auc")
    @CommandPermission(Permission.AUCTION_USE)
    public void auction(CommandSender sender) {
        UUID uuid = this.uuidFromSender(sender);
        this.message.sendPlayerMessage("auction-help", uuid, (AuctionScope) null);
    }

    @CommandMethod("auction|auc help")
    @CommandPermission(Permission.AUCTION_USE)
    public void auctionHelp(CommandSender sender) {
        UUID uuid = this.uuidFromSender(sender);
        this.message.sendPlayerMessage("auction-help", uuid, (AuctionScope) null);
    }

    @CommandMethod("auction|auc on")
    @CommandPermission(Permission.AUCTION_TOGGLE)
    public void auctionOn(CommandSender sender) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            UUID uuid = player.getUniqueId();
            if (this.plugin.removeVoluntarilyDisabled(uuid)) {
                this.message.sendPlayerMessage("auction-enabled", uuid, (AuctionScope) null);
                this.plugin.saveVoluntarilyDisabled();
            }
        }
    }

    @CommandMethod("auction|auc off|quiet|ignore|silent|silence")
    @CommandPermission(Permission.AUCTION_TOGGLE)
    public void auctionOff(CommandSender sender) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            UUID uuid = player.getUniqueId();
            if (!this.plugin.isVoluntarilyDisabled(uuid)) {
                this.message.sendPlayerMessage("auction-disabled", uuid, (AuctionScope) null);
                this.plugin.addVoluntarilyDisabled(uuid);
                this.plugin.saveVoluntarilyDisabled();
            }
        }
    }

    @CommandMethod("auction|auc start <quantity> <price> <increment> <time> [buynow] [sealed]")
    @CommandPermission(Permission.AUCTION_START)
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
            if (!AuctionConfig.getBoolean("allow-sealed-auctions", userScope) && sealed) {
                this.message.sendPlayerMessage("auction-fail-no-sealed-auctions", uuid, (AuctionScope) null);
                return;
            }
            if (!AuctionConfig.getBoolean("allow-unsealed-auctions", userScope) && !sealed) {
                sealed = true;
            }
            ItemStack hand = LegacyUtil.getItemInMainHand(player).clone();
            String[] args = {quantity, price, increment, time, buyNow};
            userScope.queueAuction(new Auction(this.plugin, player, args, userScope, sealed, this.message, hand));
        }
    }

    @CommandMethod("auction|auc end|e")
    @CommandPermission(Permission.AUCTION_END)
    public void auctionEnd(CommandSender sender) {
        UUID uuid = this.uuidFromSender(sender);
        if (uuid != null) {
            Player player = this.plugin.getServer().getPlayer(uuid);
            AuctionScope userScope = this.auctionManager.getPlayerScope(player);
            if (userScope == null) {
                this.message.sendPlayerMessage("auction-fail-no-auction-exists", uuid, (AuctionScope) null);
            } else {
                Auction auction = userScope.getActiveAuction();
                if (auction == null) {
                    this.message.sendPlayerMessage("auction-fail-no-auction-exists", uuid, (AuctionScope) null);
                } else {
                    if (!AuctionConfig.getBoolean("allow-early-end", userScope)) {
                        this.message.sendPlayerMessage("auction-fail-no-early-end", uuid, (AuctionScope) null);
                    } else if (!player.getUniqueId().equals(auction.getOwnerUUID())) {
                        this.message.sendPlayerMessage("auction-fail-not-owner-end", uuid, (AuctionScope) null);
                    } else {
                        auction.end();
                    }
                }
            }
        }
    }

    @CommandMethod("auction|auc cancel")
    @CommandPermission(Permission.AUCTION_CANCEL)
    public void auctionCancel(CommandSender sender) {
        UUID uuid = this.uuidFromSender(sender);
        if (uuid == null) {
            this.message.sendPlayerMessage("auction-fail-no-scope", uuid, (AuctionScope) null);
        } else {
            Player player = this.plugin.getServer().getPlayer(uuid);
            UUID playerUUID = player.getUniqueId();
            AuctionScope userScope = this.auctionManager.getPlayerScope(player);
            if (userScope == null) {
                this.message.sendPlayerMessage("auction-fail-no-scope", uuid, (AuctionScope) null);
                return;
            }
            if (userScope.getActiveAuction() == null && userScope.getAuctionQueueLength() == 0) {
                this.message.sendPlayerMessage("auction-fail-no-auction-exists", uuid, (AuctionScope) null);
                return;
            }
            List<Auction> auctionQueue = userScope.getAuctionQueue();
            for (int i = 0; i < auctionQueue.size(); i++) {
                if (auctionQueue.get(i).getOwnerUUID().equals(playerUUID)) {
                    auctionQueue.remove(i);
                    this.message.sendPlayerMessage("auction-cancel-queued", uuid, (AuctionScope) null);
                    return;
                }
            }
            Auction auction = userScope.getActiveAuction();
            if (auction == null) {
                this.message.sendPlayerMessage("auction-fail-no-auction-exists", uuid, (AuctionScope) null);
                return;
            }
            if (uuid.equals(auction.getOwnerUUID()) || player.hasPermission(Permission.AUCTION_ADMIN_CANCEL)) {
                if (AuctionConfig.getInt("cancel-prevention-seconds", userScope) > auction.getRemainingTime() || AuctionConfig.getDouble("cancel-prevention-percent", userScope) > (double) auction.getRemainingTime() / (double) auction.getTotalTime() * 100D) {
                    this.message.sendPlayerMessage("auction-fail-cancel-prevention", uuid, (AuctionScope) null);
                } else {
                    auction.cancel();
                }
            } else {
                this.message.sendPlayerMessage("auction-fail-not-owner-cancel", uuid, (AuctionScope) null);
            }
        }
    }

    @CommandMethod("auction|auc queue|q")
    @CommandPermission(Permission.AUCTION_QUEUE)
    public void auctionQueue(CommandSender sender) {
        UUID uuid = this.uuidFromSender(sender);
        if (uuid != null) {
            Player player = this.plugin.getServer().getPlayer(uuid);
            AuctionScope userScope = this.auctionManager.getPlayerScope(player);
            if (userScope != null) {
                List<Auction> auctionQueue = userScope.getAuctionQueue();
                if (auctionQueue.isEmpty()) {
                    this.message.sendPlayerMessage("auction-queue-status-not-in-queue", uuid, (AuctionScope) null);
                } else {
                    String queueName = AuctionConfig.getString("queue-gui-name", userScope);
                    Inventory inv = this.plugin.getServer().createInventory(null, 18, queueName);
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

    @CommandMethod("auction|auc info")
    @CommandPermission(Permission.AUCTION_INFO)
    public void auctionInfo(CommandSender sender) {
        UUID uuid = this.uuidFromSender(sender);
        if (uuid != null) {
            Player player = this.plugin.getServer().getPlayer(uuid);
            AuctionScope userScope = this.auctionManager.getPlayerScope(player);
            if (userScope != null) {
                Auction auction = userScope.getActiveAuction();
                if (auction == null) {
                    this.message.sendPlayerMessage("auction-info-no-auction", uuid, (AuctionScope) null);
                } else {
                    auction.info(sender, false);
                }
            }
        }
    }

    @CommandMethod("auction|auc reload")
    @CommandPermission(Permission.AUCTION_ADMIN_RELOAD)
    public void auctionReload(CommandSender sender) {
        UUID uuid = this.uuidFromSender(sender);
        if (this.auctionManager.areAuctionsRunning()) { // Don't reload if any auctions are running.{
            this.message.sendPlayerMessage("plugin-reload-fail-auctions-running", uuid, (AuctionScope) null);
        } else {
            this.plugin.loadConfig();
            this.message.sendPlayerMessage("plugin-reloaded", uuid, (AuctionScope) null);
        }
    }

    @CommandMethod("auction|auc suspend [player]")
    @CommandPermission(Permission.AUCTION_ADMIN_SUSPEND)
    public void auctionSuspend(CommandSender sender, @Argument("player") String playerName) {
        UUID uuid = this.uuidFromSender(sender);
        if (playerName != null) {
            Player player = this.plugin.getServer().getPlayer(playerName);
            if (player == null) {
                this.message.sendPlayerMessage("suspension-user-fail-is-offline", uuid, (AuctionScope) null);
            } else if (player.hasPermission(Permission.AUCTION_ADMIN_SUSPEND)) {
                this.message.sendPlayerMessage("suspension-user-fail-is-admin", uuid, (AuctionScope) null);
            } else if (this.plugin.isSuspendedUser(player.getUniqueId())) {
                this.message.sendPlayerMessage("suspension-user-fail-already-suspended", uuid, (AuctionScope) null);
            } else {
                this.plugin.addSuspendedUser(player.getUniqueId());
                this.plugin.saveSuspendedUsers();
                this.message.sendPlayerMessage("suspension-user", uuid, (AuctionScope) null);
                this.message.sendPlayerMessage("suspension-user-success", uuid, (AuctionScope) null);
            }
        } else {
            this.plugin.setSuspendAllAuctions(true);
            this.auctionManager.cancelAllAuctions();
            this.message.broadcastAuctionScopeMessage("suspension-global", null);
        }
    }

    @CommandMethod("auction|auc resume [player]")
    @CommandPermission(Permission.AUCTION_ADMIN_RESUME)
    public void auctionResume(CommandSender sender, @Argument("player") String playerName) {
        UUID uuid = this.uuidFromSender(sender);
        if (playerName != null) {
            Player player = this.plugin.getServer().getPlayer(playerName);
            if (player == null) {
                this.message.sendPlayerMessage("unsuspension-user-fail-is-offline", uuid, (AuctionScope) null);
            } else if (player.hasPermission(Permission.AUCTION_ADMIN_RESUME)) {
                this.message.sendPlayerMessage("unsuspension-fail-permissions", uuid, (AuctionScope) null);
            } else if (!this.plugin.isSuspendedUser(player.getUniqueId())) {
                this.message.sendPlayerMessage("unsuspension-user-fail-not-suspended", uuid, (AuctionScope) null);
            } else {
                this.plugin.removeSuspendedUser(player.getUniqueId());
                this.plugin.saveSuspendedUsers();
                this.message.sendPlayerMessage("unsuspension-user", uuid, (AuctionScope) null);
                this.message.sendPlayerMessage("unsuspension-user-success", uuid, (AuctionScope) null);
            }
        } else {
            this.plugin.setSuspendAllAuctions(false);
            this.message.broadcastAuctionScopeMessage("unsuspension-global", null);
        }
    }

    @CommandMethod("auction|auc confiscate|impound")
    @CommandPermission(Permission.AUCTION_ADMIN_CONFISCATE)
    public void auctionConfiscate(CommandSender sender) {
        UUID uuid = this.uuidFromSender(sender);
        if (uuid != null) {
            Player player = this.plugin.getServer().getPlayer(uuid);
            AuctionScope userScope = this.auctionManager.getPlayerScope(player);
            if (userScope == null) {
                this.message.sendPlayerMessage("auction-fail-no-auction-exists", uuid, (AuctionScope) null);
            } else {
                Auction auction = userScope.getActiveAuction();
                if (auction == null) {
                    this.message.sendPlayerMessage("auction-fail-no-auction-exists", uuid, (AuctionScope) null);
                } else {
                    if (uuid.equals(auction.getOwnerUUID())) {
                        this.message.sendPlayerMessage("confiscate-fail-self", uuid, (AuctionScope) null);
                    } else {
                        auction.confiscate(player);
                    }
                }
            }
        } else {
            this.message.sendPlayerMessage("confiscate-fail-console", null, (AuctionScope) null);
        }
    }

    @CommandMethod("bid <bid> <maxbid>")
    @CommandPermission(Permission.AUCTION_BID)
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
        if (!AuctionConfig.getBoolean("allow-sealed-auctions", userScope) && !AuctionConfig.getBoolean("allow-unsealed-auctions", userScope)) {
            this.message.sendPlayerMessage("auction-fail-no-auctions-allowed", uuid, (AuctionScope) null);
            return false;
        } else if (LegacyUtil.getItemInMainHand(player) == null || LegacyUtil.getItemInMainHand(player).getAmount() == 0) {
            this.message.sendPlayerMessage("auction-fail-hand-is-empty", uuid, (AuctionScope) null);
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
            this.message.sendPlayerMessage("bid-fail-no-auction", uuid, (AuctionScope) null);
        }
        return active;
    }

    private boolean preAuctionLogic(CommandSender sender, CommandType type) {
        UUID uuid = this.uuidFromSender(sender);
        if (this.plugin.getSuspendAllAuctions()) {
            this.message.sendPlayerMessage("suspension-global", uuid, (AuctionScope) null);
            return false;
        } else if (uuid == null) {
            if (type == CommandType.AUCTION) {
                this.message.sendPlayerMessage("auction-fail-console", uuid, (AuctionScope) null);
            } else {
                this.message.sendPlayerMessage("bid-fail-console", uuid, (AuctionScope) null);
            }
            return false;
        } else if (this.plugin.isVoluntarilyDisabled(uuid)) {
            this.message.sendPlayerMessage("auction-fail-disabled", uuid, (AuctionScope) null);
            return false;
        } else if (this.plugin.isSuspendedUser(uuid)) {
            this.message.sendPlayerMessage("suspension-user", uuid, (AuctionScope) null);
            return false;
        }
        Player player = this.plugin.getServer().getPlayer(uuid);
        AuctionScope userScope = this.auctionManager.getPlayerScope(player);
        if (!AuctionConfig.getBoolean("allow-gamemode-creative", userScope) && player.getGameMode() == GameMode.CREATIVE) {
            if (type == CommandType.AUCTION) {
                this.message.sendPlayerMessage("auction-fail-gamemode-creative", uuid, (AuctionScope) null);
            } else {
                this.message.sendPlayerMessage("bid-fail-gamemode-creative", uuid, (AuctionScope) null);
            }
            return false;
        } else if (userScope == null) {
            this.message.sendPlayerMessage("auction-fail-no-scope", uuid, (AuctionScope) null);
            return false;
        }
        return true;
    }

    private enum CommandType {

        AUCTION,
        BID

    }
}