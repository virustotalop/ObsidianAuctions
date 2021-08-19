package com.gmail.virustotalop.obsidianauctions.command;

import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import com.gmail.virustotalop.obsidianauctions.AuctionConfig;
import com.gmail.virustotalop.obsidianauctions.ObsidianAuctions;
import com.gmail.virustotalop.obsidianauctions.Permission;
import com.gmail.virustotalop.obsidianauctions.auction.Auction;
import com.gmail.virustotalop.obsidianauctions.auction.AuctionScope;
import com.gmail.virustotalop.obsidianauctions.message.MessageManager;
import com.gmail.virustotalop.obsidianauctions.util.LegacyUtil;
import com.google.inject.Inject;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.UUID;

public class AuctionCommands {

    /*
     public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        Player player = null;
        Auction auction = null;
        AuctionScope userScope = null;
        String playerName = null;
        UUID playerUUID = null;

        if(sender instanceof Player) {
            player = (Player) sender;
            playerName = player.getName();
            playerUUID = player.getUniqueId();
            userScope = AuctionScope.getPlayerScope(player);
            if(userScope != null) {
                auction = userScope.getActiveAuction();
            }
        }

        if(
                cmd.getName().equalsIgnoreCase("auc") ||
                        cmd.getName().equalsIgnoreCase("auction") ||
                        cmd.getName().equalsIgnoreCase("sauc") ||
                        cmd.getName().equalsIgnoreCase("sealedauction")
        ) {
            if(args.length > 0) {
                 else if(
                        args[0].equalsIgnoreCase("start") ||
                                args[0].equalsIgnoreCase("s") ||
                                args[0].equalsIgnoreCase("this") ||
                                args[0].equalsIgnoreCase("hand") ||
                                args[0].equalsIgnoreCase("all") ||
                                args[0].matches("[0-9]+")
                ) {


                    if(cmd.getName().equalsIgnoreCase("sealedauction") || cmd.getName().equalsIgnoreCase("sauc")) {
                        if(AuctionConfig.getBoolean("allow-sealed-auctions", userScope)) {
                            userScope.queueAuction(new Auction(this, player, args, userScope, true, messageManager, LegacyUtil.getItemInMainHand(player).clone()));
                        } else {
                            this.messageManager.sendPlayerMessage("auction-fail-no-sealed-auctions", playerUUID, (AuctionScope) null);
                        }
                    } else {
                        if(AuctionConfig.getBoolean("allow-unsealed-auctions", userScope)) {
                            userScope.queueAuction(new Auction(this, player, args, userScope, false, messageManager, LegacyUtil.getItemInMainHand(player).clone()));
                        } else {
                            userScope.queueAuction(new Auction(this, player, args, userScope, true, messageManager, LegacyUtil.getItemInMainHand(player).clone()));
                        }
                    }

                    return true;
                }
            }
            this.messageManager.sendPlayerMessage("auction-help", playerUUID, (AuctionScope) null);
            return true;
        }
    }
     */

    private final ObsidianAuctions plugin;
    private final MessageManager messageManager;

    @Inject
    private AuctionCommands(ObsidianAuctions plugin, MessageManager messageManager) {
        this.plugin = plugin;
        this.messageManager = messageManager;
    }

    @CommandMethod("auction|auc")
    @CommandPermission(Permission.AUCTION_USE)
    public void auction(CommandSender sender) {
        UUID uuid = this.uuidFromSender(sender);
        this.messageManager.sendPlayerMessage("auction-help", uuid, (AuctionScope) null);
    }

    @CommandMethod("auction help")
    @CommandPermission(Permission.AUCTION_USE)
    public void auctionHelp(CommandSender sender) {
        UUID uuid = this.uuidFromSender(sender);
        this.messageManager.sendPlayerMessage("auction-help", uuid, (AuctionScope) null);
    }

    @CommandMethod("auction on")
    @CommandPermission(Permission.AUCTION_TOGGLE)
    public void auctionOn(CommandSender sender) {
        if(sender instanceof Player) {
            Player player = (Player) sender;
            UUID uuid = player.getUniqueId();
            if(this.plugin.removeVoluntarilyDisabled(uuid)) {
                this.messageManager.sendPlayerMessage("auction-enabled", uuid, (AuctionScope) null);
                this.plugin.saveVoluntarilyDisabled();
            }
        }
    }

    @CommandMethod("auction off|quiet|ignore|silent|silence")
    @CommandPermission(Permission.AUCTION_TOGGLE)
    public void auctionOff(CommandSender sender) {
        if(sender instanceof Player) {
            Player player = (Player) sender;
            UUID uuid = player.getUniqueId();
            if(!this.plugin.isVoluntarilyDisabled(uuid)) {
                this.messageManager.sendPlayerMessage("auction-disabled", uuid, (AuctionScope) null);
                this.plugin.addVoluntarilyDisabled(uuid);
                this.plugin.saveVoluntarilyDisabled();
            }
        }
    }

    @CommandMethod("auction reload")
    @CommandPermission(Permission.AUCTION_ADMIN)
    public void auctionReload(CommandSender sender) {
        UUID uuid = this.uuidFromSender(sender);
        if(AuctionScope.areAuctionsRunning()) { // Don't reload if any auctions are running.{
            this.messageManager.sendPlayerMessage("plugin-reload-fail-auctions-running", uuid, (AuctionScope) null);
        } else {
            this.plugin.loadConfig();
            this.messageManager.sendPlayerMessage("plugin-reloaded", uuid, (AuctionScope) null);
        }
    }

    @CommandMethod("auction suspend <player>")
    @CommandPermission(Permission.AUCTION_ADMIN)
    public void auctionSuspend(CommandSender sender, @Argument("player") String playerName) {
        UUID uuid = this.uuidFromSender(sender);
        if(playerName != null) {
            Player player = this.plugin.getServer().getPlayer(playerName);
            if(player == null) {
                this.messageManager.sendPlayerMessage("suspension-user-fail-is-offline", uuid, (AuctionScope) null);
            } else if(player.hasPermission(Permission.AUCTION_ADMIN)) {
                this.messageManager.sendPlayerMessage("suspension-user-fail-is-admin", uuid, (AuctionScope) null);
            } else if(this.plugin.isSuspendedUser(player.getUniqueId())) {
                this.messageManager.sendPlayerMessage("suspension-user-fail-already-suspended", uuid, (AuctionScope) null);
            } else {
                this.plugin.addSuspendedUser(player.getUniqueId());
                this.plugin.saveSuspendedUsers();
                this.messageManager.sendPlayerMessage("suspension-user", uuid, (AuctionScope) null);
                this.messageManager.sendPlayerMessage("suspension-user-success", uuid, (AuctionScope) null);
            }
        } else {
            this.plugin.setSuspendAllAuctions(true);
            AuctionScope.cancelAllAuctions();
            this.messageManager.broadcastAuctionScopeMessage("suspension-global", null);
        }
    }

    @CommandMethod("auction resume <player>")
    @CommandPermission(Permission.AUCTION_ADMIN)
    public void auctionResume(CommandSender sender, @Argument("player") String playerName) {
        UUID uuid = this.uuidFromSender(sender);
        if(playerName != null) {
            Player player = this.plugin.getServer().getPlayer(playerName);
            if(player == null) {
                this.messageManager.sendPlayerMessage("unsuspension-user-fail-is-offline", uuid, (AuctionScope) null);
            } else if(player.hasPermission(Permission.AUCTION_ADMIN)) {
                this.messageManager.sendPlayerMessage("unsuspension-fail-permissions", uuid, (AuctionScope) null);
            } else if(!this.plugin.isSuspendedUser(player.getUniqueId())) {
                this.messageManager.sendPlayerMessage("unsuspension-user-fail-not-suspended", uuid, (AuctionScope) null);
            } else {
                this.plugin.removeSuspendedUser(player.getUniqueId());
                this.plugin.saveSuspendedUsers();
                this.messageManager.sendPlayerMessage("unsuspension-user", uuid, (AuctionScope) null);
                this.messageManager.sendPlayerMessage("unsuspension-user-success", uuid, (AuctionScope) null);
            }
        } else {
            this.plugin.setSuspendAllAuctions(false);
            this.messageManager.broadcastAuctionScopeMessage("unsuspension-global", null);
        }
    }

    @CommandMethod("auction start [quantity] [price] [increment] [buynow]")
    @CommandPermission(Permission.AUCTION_START)
    public void auctionStart(CommandSender sender, @Argument("quantity") int quantity,
                             @Argument("price") long price, @Argument("increment") long increment,
                             @Argument("buynow") long buyNow) {
        if(this.canAuction(sender)) {
            //TODO - Implement auction
        }
    }



    @CommandMethod("auction end|e")
    @CommandPermission(Permission.AUCTION_END)
    public void auctionEnd(CommandSender sender) {
        UUID uuid = this.uuidFromSender(sender);
        if(uuid != null) {
            Player player = this.plugin.getServer().getPlayer(uuid);
            AuctionScope userScope = AuctionScope.getPlayerScope(player);
            if(userScope == null) {
                this.messageManager.sendPlayerMessage("auction-fail-no-auction-exists", uuid, (AuctionScope) null);
            } else {
                Auction auction = userScope.getActiveAuction();
                if(auction == null) {
                    this.messageManager.sendPlayerMessage("auction-fail-no-auction-exists", uuid, (AuctionScope) null);
                } else {
                    if(!AuctionConfig.getBoolean("allow-early-end", userScope)) {
                        this.messageManager.sendPlayerMessage("auction-fail-no-early-end", uuid, (AuctionScope) null);
                    } else if(!player.getUniqueId().equals(auction.getOwnerUUID())) {
                        this.messageManager.sendPlayerMessage("auction-fail-not-owner-end", uuid, (AuctionScope) null);
                    } else {
                        auction.end();
                    }
                }
            }
        }
    }

    @CommandMethod("auction cancel")
    @CommandPermission(Permission.AUCTION_CANCEL)
    public void auctionCancel(CommandSender sender) {
        UUID uuid = this.uuidFromSender(sender);
        if(uuid == null) {
            this.messageManager.sendPlayerMessage("auction-fail-no-scope", uuid, (AuctionScope) null);
        } else {
            Player player = this.plugin.getServer().getPlayer(uuid);
            String playerName = player.getName();
            AuctionScope userScope = AuctionScope.getPlayerScope(player);
            if(userScope == null) {
                this.messageManager.sendPlayerMessage("auction-fail-no-scope", uuid, (AuctionScope) null);
                return;
            }
            if(userScope.getActiveAuction() == null && userScope.getAuctionQueueLength() == 0) {
                this.messageManager.sendPlayerMessage("auction-fail-no-auction-exists", uuid, (AuctionScope) null);
                return;
            }
            List<Auction> auctionQueue = userScope.getAuctionQueue();
            for(int i = 0; i < auctionQueue.size(); i++) {
                if(auctionQueue.get(i).getOwnerName().equalsIgnoreCase(playerName)) {
                    auctionQueue.remove(i);
                    this.messageManager.sendPlayerMessage("auction-cancel-queued", uuid, (AuctionScope) null);
                    return;
                }
            }
            Auction auction = userScope.getActiveAuction();
            if(auction == null) {
                this.messageManager.sendPlayerMessage("auction-fail-no-auction-exists", uuid, (AuctionScope) null);
                return;
            }
            if(player == null || uuid.equals(auction.getOwnerUUID()) || player.hasPermission(Permission.AUCTION_ADMIN)) {
                if(AuctionConfig.getInt("cancel-prevention-seconds", userScope) > auction.getRemainingTime() || AuctionConfig.getDouble("cancel-prevention-percent", userScope) > (double) auction.getRemainingTime() / (double) auction.getTotalTime() * 100D) {
                    this.messageManager.sendPlayerMessage("auction-fail-cancel-prevention", uuid, (AuctionScope) null);
                } else {
                    auction.cancel();
                }
            } else {
                this.messageManager.sendPlayerMessage("auction-fail-not-owner-cancel", uuid, (AuctionScope) null);
            }
        }
    }

    @CommandMethod("auction queue|q")
    @CommandPermission(Permission.AUCTION_QUEUE)
    public void auctionQueue(CommandSender sender) {
        UUID uuid = this.uuidFromSender(sender);
        if(uuid != null) {
            Player player = this.plugin.getServer().getPlayer(uuid);
            AuctionScope userScope = AuctionScope.getPlayerScope(player);
            if(userScope != null) {
                List<Auction> auctionQueue = userScope.getAuctionQueue();
                if(auctionQueue.isEmpty()) {
                    this.messageManager.sendPlayerMessage("auction-queue-status-not-in-queue", uuid, (AuctionScope) null);
                } else {
                    Inventory inv = Bukkit.createInventory(null, 18, ObsidianAuctions.guiQueueName);
                    for(int i = 0; i < auctionQueue.size(); i++) {
                        if(i == inv.getSize()) {
                            break;
                        }
                        inv.setItem(i, auctionQueue.get(i).getGuiItem());
                    }
                    player.openInventory(inv);
                }
            }
        }
    }

    @CommandMethod("auction info")
    @CommandPermission(Permission.AUCTION_INFO)
    public void auctionInfo(CommandSender sender) {
        UUID uuid = this.uuidFromSender(sender);
        if(uuid != null) {
            Player player = this.plugin.getServer().getPlayer(uuid);
            AuctionScope userScope = AuctionScope.getPlayerScope(player);
            if(userScope != null) {
                Auction auction = userScope.getActiveAuction();
                if(auction == null) {
                    this.messageManager.sendPlayerMessage("auction-info-no-auction", uuid, (AuctionScope) null);
                } else {
                    auction.info(sender, false);
                }
            }
        }
    }

    @CommandMethod("auction confiscate|impound")
    @CommandPermission(Permission.AUCTION_ADMIN)
    public void auctionConfiscate(CommandSender sender) {
        UUID uuid = this.uuidFromSender(sender);
        if(uuid != null) {
            Player player = this.plugin.getServer().getPlayer(uuid);
            AuctionScope userScope = AuctionScope.getPlayerScope(player);
            if(userScope == null) {
                this.messageManager.sendPlayerMessage("auction-fail-no-auction-exists", uuid, (AuctionScope) null);
            } else {
                Auction auction = userScope.getActiveAuction();
                if(auction == null) {
                    this.messageManager.sendPlayerMessage("auction-fail-no-auction-exists", uuid, (AuctionScope) null);
                } else {
                    if(uuid.equals(auction.getOwnerUUID())) {
                        this.messageManager.sendPlayerMessage("confiscate-fail-self", uuid, (AuctionScope) null);
                    } else {
                        auction.confiscate(player);
                    }
                }
            }
        } else {
            this.messageManager.sendPlayerMessage("confiscate-fail-console", null, (AuctionScope) null);
        }
    }

    @CommandMethod("bid [bid] [maxbid]")
    @CommandPermission(Permission.AUCTION_BID)
    public void bid(CommandSender sender, @Argument("bid") String bid, @Argument("maxbid") String maxBid) {
        if(this.canBid(sender)) {
            Player player = (Player) sender;
            AuctionScope userScope = AuctionScope.getPlayerScope(player);
            Auction auction = userScope.getActiveAuction();
            String[] args = {bid, maxBid};
            auction.bid(player, args);
        }
    }

    private UUID uuidFromSender(CommandSender sender) {
        UUID uuid = null;
        if(sender instanceof Player) {
            uuid = ((Player) sender).getUniqueId();
        }
        return uuid;
    }

    private boolean canAuction(CommandSender sender) {
        if(!this.preAuctionLogic(sender, CommandType.AUCTION)) {
            return false;
        }
        UUID uuid = this.uuidFromSender(sender);
        Player player = this.plugin.getServer().getPlayer(uuid);
        AuctionScope userScope = AuctionScope.getPlayerScope(player);
        if(!AuctionConfig.getBoolean("allow-sealed-auctions", userScope) && !AuctionConfig.getBoolean("allow-unsealed-auctions", userScope)) {
            this.messageManager.sendPlayerMessage("auction-fail-no-auctions-allowed", uuid, (AuctionScope) null);
            return false;
        } else if(LegacyUtil.getItemInMainHand(player) == null || LegacyUtil.getItemInMainHand(player).getAmount() == 0) {
            this.messageManager.sendPlayerMessage("auction-fail-hand-is-empty", uuid, (AuctionScope) null);
            return false;
        }
        return true;
    }

    private boolean canBid(CommandSender sender) {
        if(!this.preAuctionLogic(sender, CommandType.BID)) {
            return false;
        }
        UUID uuid = this.uuidFromSender(sender);
        Player player = (Player) sender;
        AuctionScope scope = AuctionScope.getPlayerScope(player);
        boolean active = scope.getActiveAuction() != null;
        if(!active) {
            this.messageManager.sendPlayerMessage("bid-fail-no-auction", uuid, (AuctionScope) null);
        }
        return active;
    }

    private boolean preAuctionLogic(CommandSender sender, CommandType type) {
        UUID uuid = this.uuidFromSender(sender);
        if(this.plugin.getSuspendAllAuctions()) {
            this.messageManager.sendPlayerMessage("suspension-global", uuid, (AuctionScope) null);
            return false;
        } else if(uuid == null) {
            if(type == CommandType.AUCTION) {
                this.messageManager.sendPlayerMessage("auction-fail-console", uuid, (AuctionScope) null);
            } else {
                this.messageManager.sendPlayerMessage("bid-fail-console", uuid, (AuctionScope) null);
            }
            return false;
        } else if(this.plugin.isVoluntarilyDisabled(uuid)) {
            this.messageManager.sendPlayerMessage("auction-fail-disabled", uuid, (AuctionScope) null);
            return false;
        } else if(this.plugin.isSuspendedUser(uuid)) {
            this.messageManager.sendPlayerMessage("suspension-user", uuid, (AuctionScope) null);
            return false;
        }
        Player player = this.plugin.getServer().getPlayer(uuid);
        AuctionScope userScope = AuctionScope.getPlayerScope(player);
        if(!AuctionConfig.getBoolean("allow-gamemode-creative", userScope) && player.getGameMode() == GameMode.CREATIVE) {
            if(type == CommandType.AUCTION) {
                this.messageManager.sendPlayerMessage("auction-fail-gamemode-creative", uuid, (AuctionScope) null);
            } else {
                this.messageManager.sendPlayerMessage("bid-fail-gamemode-creative", uuid, (AuctionScope) null);
            }
            return false;
        } else if(userScope == null) {
            this.messageManager.sendPlayerMessage("auction-fail-no-scope", uuid, (AuctionScope) null);
            return false;
        }
        return true;
    }

    private enum CommandType {

        AUCTION,
        BID

    }
}