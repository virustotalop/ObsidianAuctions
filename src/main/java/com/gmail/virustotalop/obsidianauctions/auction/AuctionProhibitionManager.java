package com.gmail.virustotalop.obsidianauctions.auction;

import com.gmail.virustotalop.obsidianauctions.message.MessageManager;
import com.google.inject.Inject;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

public class AuctionProhibitionManager {

    private final MessageManager messageManager;
    private final List<AuctionProhibition> involuntarilyDisabledUsers;

    @Inject
    private AuctionProhibitionManager(MessageManager messageManager) {
        this.messageManager = messageManager;
        this.involuntarilyDisabledUsers = new ArrayList<>();
    }

    /**
     * Checks to see if a player is on prohibition by any plugin and optionally sends a reminder to them that they are.
     *
     * @param playerName          player to check
     * @param sendReminderMessage whether to remind
     * @return whether they're prohibited
     */
    public boolean isOnProhibition(String playerName, boolean sendReminderMessage) {
        AuctionProhibition prohibition = this.getProhibition(playerName);
        if(prohibition != null) {
            if(sendReminderMessage) {
                Player player = Bukkit.getPlayer(playerName);
                if(player == null) {
                    return true;
                }
                if(prohibition.getReminderMessage() == null) {
                    // Send stock message.
                    this.messageManager.sendPlayerMessage("remote-plugin-prohibition-reminder", playerName, (AuctionScope) null);
                } else {
                    player.sendMessage(prohibition.getReminderMessage());
                }
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * Checks to see if a player is on prohibition by a specified plugin and optionally sends a reminder to them that they are.
     *
     * @param playerName          player to check
     * @param sendReminderMessage whether to remind
     * @return whether they're prohibited
     * @prohibiterPlugin plugin to check
     */
    public boolean isOnProhibition(Plugin prohibiterPlugin, String playerName, boolean sendReminderMessage) {
        AuctionProhibition prohibition = this.getProhibition(prohibiterPlugin, playerName);
        if(prohibition != null) {
            if(sendReminderMessage) {
                Player player = Bukkit.getPlayer(playerName);
                if(player == null) {
                    return true;
                }
                if(prohibition.getReminderMessage() == null) {
                    // Send stock message.
                    this.messageManager.sendPlayerMessage("remote-plugin-prohibition-reminder", playerName, (AuctionScope) null);
                } else {
                    player.sendMessage(prohibition.getReminderMessage());
                }
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * Mark a player as prohibited from auctioning and notify them.  Will return false if unable to prohibit user.  Plugin instituted prohibitions cannot be instituted while a player is participating in an auction.
     *
     * @param prohibiterPlugin plugin requesting prohibition
     * @param playerName       player being prohibited
     * @return success as prohibiting
     */
    public boolean prohibitPlayer(Plugin prohibiterPlugin, String playerName) {
        return this.prohibitPlayer(prohibiterPlugin, playerName, null, null, null);
    }

    /**
     * Mark a player as prohibited from auctioning.  Will return false if unable to prohibit user.  Plugin instituted prohibitions cannot be instituted while a player is participating in an auction.
     * <p>
     * Optional messages can be set to override floAuction's normal notification.  Set these to null to use the floAuction's normal messages.
     *
     * @param prohibiterPlugin plugin requesting prohibition
     * @param playerName       player being prohibited
     * @param enableMessage    message to send when starting prohibition
     * @param reminderMessage  message to send when reminding player of prohibition
     * @param disableMessage   message to send when ending prohibition
     * @return success as prohibiting
     */
    public boolean prohibitPlayer(Plugin prohibiterPlugin, String playerName, String enableMessage, String reminderMessage, String disableMessage) {
        if(AuctionParticipant.isParticipating(playerName)) {
            return false;
        } else if(this.isOnProhibition(prohibiterPlugin, playerName, false)) {
            return true;
        } else if(this.getProhibition(playerName) != null) {
            this.prohibitPlayer(prohibiterPlugin, playerName, disableMessage, reminderMessage, enableMessage);
            return true;
        }

        this.prohibitPlayer(prohibiterPlugin, playerName, disableMessage, reminderMessage, enableMessage);

        Player player = Bukkit.getPlayer(playerName);
        if(player == null) {
            return true;
        }
        if(enableMessage == null) {
            // Send stock message.
            this.messageManager.sendPlayerMessage("remote-plugin-prohibition-enabled", playerName, (AuctionScope) null);
        } else {
            player.sendMessage(enableMessage);
        }
        return true;
    }

    /**
     * Removes the prohibition set by a specific plugin and notifies user of such.
     *
     * @param prohibiterPlugin plugin which requested prohibition
     * @param playerName       prohibited player
     */
    public void removeProhibition(Plugin prohibiterPlugin, String playerName) {
        Player player = Bukkit.getPlayer(playerName);
        for(int i = 0; i < this.involuntarilyDisabledUsers.size(); i++) {
            AuctionProhibition prohibition = involuntarilyDisabledUsers.get(i);
            if(prohibition.getPlayerName().equalsIgnoreCase(playerName) && prohibition.getProhibiterPlugin().equals(prohibiterPlugin)) {
                if(player != null) {
                    if(prohibition.getDisableMessage() == null) {
                        // Send stock message.
                        this.messageManager.sendPlayerMessage("remote-plugin-prohibition-disabled", playerName, (AuctionScope) null);
                    } else {
                        player.sendMessage(prohibition.getDisableMessage());
                    }
                }
                this.involuntarilyDisabledUsers.remove(i);
                i--;
            }
        }

        AuctionProhibition prohibition = getProhibition(playerName);
        if(prohibition != null) {
            if(player != null) {
                if(prohibition.getEnableMessage() == null) {
                    // Send stock message.
                    this.messageManager.sendPlayerMessage("remote-plugin-prohibition-enabled", playerName, (AuctionScope) null);
                } else {
                    player.sendMessage(prohibition.getEnableMessage());
                }
            }
        }
    }

    /**
     * Gets the prohibition instance by plugin and player name.
     *
     * @param prohibiterPlugin prohibiting plugin
     * @param playerName       prohibited player
     * @return prohibition instance
     */
    private AuctionProhibition getProhibition(Plugin prohibiterPlugin, String playerName) {
        for(int i = 0; i < this.involuntarilyDisabledUsers.size(); i++) {
            AuctionProhibition prohibition = this.involuntarilyDisabledUsers.get(i);
            if(prohibition.getPlayerName().equalsIgnoreCase(playerName) && prohibition.equals(prohibiterPlugin)) {
                return prohibition;
            }
        }
        return null;
    }

    /**
     * Gets the first prohibition instance of a player by name.
     *
     * @param playerName prohibited player
     * @return prohibition instance
     */
    private AuctionProhibition getProhibition(String playerName) {
        for(int i = 0; i < this.involuntarilyDisabledUsers.size(); i++) {
            AuctionProhibition prohibition = this.involuntarilyDisabledUsers.get(i);
            if(prohibition.getPlayerName().equalsIgnoreCase(playerName)) {
                return prohibition;
            }
        }
        return null;
    }
}
