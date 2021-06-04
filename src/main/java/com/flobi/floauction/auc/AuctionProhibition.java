package com.flobi.floauction.auc;

import com.flobi.floauction.FloAuction;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;

/**
 * Class to manipulate remote plugin prohibitions on player auctioning.
 *
 * @author Joshua "flobi" Hatfield
 */
public class AuctionProhibition {

    private Plugin prohibiterPlugin = null;
    private String playerName = null;
    private String enableMessage = null;
    private String reminderMessage = null;
    private String disableMessage = null;

    private static final ArrayList<AuctionProhibition> involuntarilyDisabledUsers = new ArrayList<AuctionProhibition>();

    /**
     * Gets the prohibition instance by plugin and player name.
     *
     * @param prohibiterPlugin prohibiting plugin
     * @param playerName       prohibited player
     * @return prohibition instance
     */
    private static AuctionProhibition getProhibition(Plugin prohibiterPlugin, String playerName) {
        for(int i = 0; i < AuctionProhibition.involuntarilyDisabledUsers.size(); i++) {
            AuctionProhibition auctionProhibition = AuctionProhibition.involuntarilyDisabledUsers.get(i);
            if(auctionProhibition.playerName.equalsIgnoreCase(playerName) && auctionProhibition.prohibiterPlugin.equals(prohibiterPlugin)) {
                return auctionProhibition;
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
    private static AuctionProhibition getProhibition(String playerName) {
        for(int i = 0; i < AuctionProhibition.involuntarilyDisabledUsers.size(); i++) {
            AuctionProhibition auctionProhibition = AuctionProhibition.involuntarilyDisabledUsers.get(i);
            if(auctionProhibition.playerName.equalsIgnoreCase(playerName)) {
                return auctionProhibition;
            }
        }
        return null;
    }

    /**
     * Checks to see if a player is on prohibition by any plugin and optionally sends a reminder to them that they are.
     *
     * @param playerName          player to check
     * @param sendReminderMessage whether to remind
     * @return whether they're prohibited
     */
    public static boolean isOnProhibition(String playerName, boolean sendReminderMessage) {
        AuctionProhibition auctionProhibition = AuctionProhibition.getProhibition(playerName);
        if(auctionProhibition != null) {
            if(sendReminderMessage) {
                Player player = Bukkit.getPlayer(playerName);
                if(player == null) {
                    return true;
                }
                if(auctionProhibition.reminderMessage == null) {
                    // Send stock message.
                    FloAuction.getMessageManager().sendPlayerMessage("remote-plugin-prohibition-reminder", playerName, (AuctionScope) null);
                } else {
                    player.sendMessage(auctionProhibition.reminderMessage);
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
    public static boolean isOnProhibition(Plugin prohibiterPlugin, String playerName, boolean sendReminderMessage) {
        AuctionProhibition auctionProhibition = AuctionProhibition.getProhibition(prohibiterPlugin, playerName);
        if(auctionProhibition != null) {
            if(sendReminderMessage) {
                Player player = Bukkit.getPlayer(playerName);
                if(player == null) {
                    return true;
                }
                if(auctionProhibition.reminderMessage == null) {
                    // Send stock message.
                    FloAuction.getMessageManager().sendPlayerMessage("remote-plugin-prohibition-reminder", playerName, (AuctionScope) null);
                } else {
                    player.sendMessage(auctionProhibition.reminderMessage);
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
    public static boolean prohibitPlayer(Plugin prohibiterPlugin, String playerName) {
        return AuctionProhibition.prohibitPlayer(prohibiterPlugin, playerName, null, null, null);
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
    public static boolean prohibitPlayer(Plugin prohibiterPlugin, String playerName, String enableMessage, String reminderMessage, String disableMessage) {
        if(AuctionParticipant.isParticipating(playerName)) {
            return false;
        } else if(AuctionProhibition.isOnProhibition(prohibiterPlugin, playerName, false)) {
            return true;
        } else if(AuctionProhibition.getProhibition(playerName) != null) {
            AuctionProhibition.prohibitPlayer(prohibiterPlugin, playerName, disableMessage, reminderMessage, enableMessage);
            return true;
        }

        AuctionProhibition.prohibitPlayer(prohibiterPlugin, playerName, disableMessage, reminderMessage, enableMessage);

        Player player = Bukkit.getPlayer(playerName);
        if(player == null) {
            return true;
        }
        if(enableMessage == null) {
            // Send stock message.
            FloAuction.getMessageManager().sendPlayerMessage("remote-plugin-prohibition-enabled", playerName, (AuctionScope) null);
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
    public static void removeProhibition(Plugin prohibiterPlugin, String playerName) {
        Player player = Bukkit.getPlayer(playerName);
        for(int i = 0; i < AuctionProhibition.involuntarilyDisabledUsers.size(); i++) {
            AuctionProhibition auctionProhibition = involuntarilyDisabledUsers.get(i);
            if(auctionProhibition.playerName.equalsIgnoreCase(playerName) && auctionProhibition.prohibiterPlugin.equals(prohibiterPlugin)) {
                if(player != null) {
                    if(auctionProhibition.disableMessage == null) {
                        // Send stock message.
                        FloAuction.getMessageManager().sendPlayerMessage("remote-plugin-prohibition-disabled", playerName, (AuctionScope) null);
                    } else {
                        player.sendMessage(auctionProhibition.disableMessage);
                    }
                }
                AuctionProhibition.involuntarilyDisabledUsers.remove(i);
                i--;
            }
        }

        AuctionProhibition auctionProhibition = getProhibition(playerName);
        if(auctionProhibition != null) {
            if(player != null) {
                if(auctionProhibition.enableMessage == null) {
                    // Send stock message.
                    FloAuction.getMessageManager().sendPlayerMessage("remote-plugin-prohibition-enabled", playerName, (AuctionScope) null);
                } else {
                    player.sendMessage(auctionProhibition.enableMessage);
                }
            }
        }
    }

    /**
     * Private constructor called when initializing prohibition.
     *
     * @param prohibiterPlugin prohibiting plugin
     * @param playerName       prohibited player's name
     * @param enableMessage    message to send when starting prohibition
     * @param reminderMessage  message to send when reminding player of prohibition
     * @param disableMessage   message to send when ending prohibition
     */
    private AuctionProhibition(Plugin prohibiterPlugin, String playerName, String enableMessage, String reminderMessage, String disableMessage) {
        this.prohibiterPlugin = prohibiterPlugin;
        this.playerName = playerName;
        this.enableMessage = enableMessage;
        this.reminderMessage = reminderMessage;
        this.disableMessage = disableMessage;
    }
}
