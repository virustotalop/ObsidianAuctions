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

import java.util.UUID;

public class AuctionCommand {

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
                } else if(args[0].equalsIgnoreCase("prep") || args[0].equalsIgnoreCase("p")) {
                    // Save a users individual starting default values.
                    if(player == null) {
                        this.messageManager.sendPlayerMessage("auction-fail-console", null, (AuctionScope) null);
                        return true;
                    }
                    if(!perms.has(player, "auction.start")) {
                        this.messageManager.sendPlayerMessage("auction-fail-permissions", playerUUID, (AuctionScope) null);
                        return true;
                    }

                    // The function returns null and sends error on failure.
                    String[] mergedArgs = Functions.mergeInputArgs(playerUUID, args, true);

                    if(mergedArgs != null) {
                        ObsidianAuctions.userSavedInputArgs.put(playerUUID, mergedArgs);
                        ObsidianAuctions.saveObject(ObsidianAuctions.userSavedInputArgs, "userSavedInputArgs.ser");
                        this.messageManager.sendPlayerMessage("prep-save-success", playerUUID, (AuctionScope) null);
                    }

                    return true;
                } else if(args[0].equalsIgnoreCase("cancel") || args[0].equalsIgnoreCase("c")) {
                    if(userScope == null) {
                        this.messageManager.sendPlayerMessage("auction-fail-no-scope", playerUUID, (AuctionScope) null);
                        return true;
                    }
                    if(userScope.getActiveAuction() == null && userScope.getAuctionQueueLength() == 0) {
                        this.messageManager.sendPlayerMessage("auction-fail-no-auction-exists", playerUUID, (AuctionScope) null);
                        return true;
                    }

                    List<Auction> auctionQueue = userScope.getAuctionQueue();
                    for(int i = 0; i < auctionQueue.size(); i++) {
                        if(auctionQueue.get(i).getOwnerName().equalsIgnoreCase(playerName)) {
                            auctionQueue.remove(i);
                            this.messageManager.sendPlayerMessage("auction-cancel-queued", playerUUID, (AuctionScope) null);
                            return true;
                        }
                    }

                    if(auction == null) {
                        this.messageManager.sendPlayerMessage("auction-fail-no-auction-exists", playerUUID, (AuctionScope) null);
                        return true;
                    }

                    if(player == null || player.getName().equalsIgnoreCase(auction.getOwnerName()) || perms.has(player, "auction.admin")) {
                        if(AuctionConfig.getInt("cancel-prevention-seconds", userScope) > auction.getRemainingTime() || AuctionConfig.getDouble("cancel-prevention-percent", userScope) > (double) auction.getRemainingTime() / (double) auction.getTotalTime() * 100D) {
                            this.messageManager.sendPlayerMessage("auction-fail-cancel-prevention", playerUUID, (AuctionScope) null);
                        } else {
                            auction.cancel();
                        }
                    } else {
                        this.messageManager.sendPlayerMessage("auction-fail-not-owner-cancel", playerUUID, (AuctionScope) null);
                    }
                    return true;
                } else if(args[0].equalsIgnoreCase("confiscate") || args[0].equalsIgnoreCase("impound")) {
                    if(auction == null) {
                        this.messageManager.sendPlayerMessage("auction-fail-no-auction-exists", playerUUID, (AuctionScope) null);
                        return true;
                    }

                    if(player == null) {
                        this.messageManager.sendPlayerMessage("confiscate-fail-console", playerUUID, (AuctionScope) null);
                        return true;
                    }
                    if(!perms.has(player, "auction.admin")) {
                        this.messageManager.sendPlayerMessage("confiscate-fail-permissions", playerUUID, (AuctionScope) null);
                        return true;
                    }
                    if(playerName.equalsIgnoreCase(auction.getOwnerName())) {
                        this.messageManager.sendPlayerMessage("confiscate-fail-self", playerUUID, (AuctionScope) null);
                        return true;
                    }
                    auction.confiscate(player);
                    return true;
                } else if(args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("i")) {
                    if(auction == null) {
                        this.messageManager.sendPlayerMessage("auction-info-no-auction", playerUUID, (AuctionScope) null);
                        return true;
                    }
                    auction.info(sender, false);
                    return true;
                } else if(args[0].equalsIgnoreCase("queue") || args[0].equalsIgnoreCase("q")) {
                    List<Auction> auctionQueue = userScope.getAuctionQueue();
                    if(auctionQueue.isEmpty()) {
                        this.messageManager.sendPlayerMessage("auction-queue-status-not-in-queue", playerUUID, (AuctionScope) null);
                        return true;
                    }
                    Inventory inv = Bukkit.createInventory(null, 18, ObsidianAuctions.guiQueueName);
                    for(int i = 0; i < auctionQueue.size(); i++) {
                        if(i == inv.getSize())
                            break;
                        inv.setItem(i, auctionQueue.get(i).getGuiItem());
                    }
                    player.openInventory(inv);
                    return true;
                }
            }
            this.messageManager.sendPlayerMessage("auction-help", playerUUID, (AuctionScope) null);
            return true;
        } else if(cmd.getName().equalsIgnoreCase("bid")) {
            if(suspendAllAuctions) {
                this.messageManager.sendPlayerMessage("suspension-global", playerUUID, (AuctionScope) null);
                return true;
            } else if(player != null && suspendedUsers.contains(playerName.toLowerCase())) {
                this.messageManager.sendPlayerMessage("suspension-user", playerUUID, (AuctionScope) null);
                return true;
            } else if(player == null) {
                this.messageManager.sendPlayerMessage("bid-fail-console", playerUUID, (AuctionScope) null);
                return true;
            } else if(!AuctionConfig.getBoolean("allow-gamemode-creative", userScope) && player.getGameMode().equals(GameMode.CREATIVE)) {
                this.messageManager.sendPlayerMessage("bid-fail-gamemode-creative", playerUUID, (AuctionScope) null);
                return true;
            } else if(!perms.has(player, "auction.bid")) {
                this.messageManager.sendPlayerMessage("bid-fail-permissions", playerUUID, (AuctionScope) null);
                return true;
            } else if(auction == null) {
                this.messageManager.sendPlayerMessage("bid-fail-no-auction", playerUUID, (AuctionScope) null);
                return true;
            }
            auction.bid(player, args);
            return true;
        }
        return false;
    }
     */

    private final ObsidianAuctions plugin;
    private final MessageManager messageManager;

    @Inject
    private AuctionCommand(ObsidianAuctions plugin, MessageManager messageManager) {
        this.plugin = plugin;
        this.messageManager = messageManager;
    }

    @CommandMethod("auction|auc")
    @CommandPermission(Permission.AUCTION_USE)
    public void auction(CommandSender sender) {
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
            Player player = Bukkit.getServer().getPlayer(playerName);
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
            Player player = Bukkit.getServer().getPlayer(playerName);
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
    public void start(CommandSender sender, int quantity, long price, long increment, long buyNow) {
        if(this.canAuction(sender)) {
            //TODO - Implement auction
        }
    }

    /*
    if(auction == null) {
                        this.messageManager.sendPlayerMessage("auction-fail-no-auction-exists", playerUUID, (AuctionScope) null);
                        return true;
                    }
                    if(!AuctionConfig.getBoolean("allow-early-end", userScope)) {
                        this.messageManager.sendPlayerMessage("auction-fail-no-early-end", playerUUID, (AuctionScope) null);
                        return true;
                    }
                    if(player.getName().equalsIgnoreCase(auction.getOwnerName())) {
                        auction.end();
                    } else {
                        this.messageManager.sendPlayerMessage("auction-fail-not-owner-end", playerUUID, (AuctionScope) null);
                    }
                    return true;
     */

    @CommandMethod("auction end|e")
    @CommandPermission(Permission.AUCTION_END)
    public void end(CommandSender sender) {
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

    @CommandMethod("bid")
    @CommandPermission(Permission.AUCTION_BID)
    public void bid(CommandSender sender) {
        if(this.canBid(sender)) {
            //TODO - Implement bid
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