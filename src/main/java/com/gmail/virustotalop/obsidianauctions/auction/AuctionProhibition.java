package com.gmail.virustotalop.obsidianauctions.auction;

import com.gmail.virustotalop.obsidianauctions.ObsidianAuctions;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Class to manipulate remote plugin prohibitions on player auctioning.
 *
 * @author Joshua "flobi" Hatfield
 */
public class AuctionProhibition {

    private final Plugin prohibiterPlugin;
    private final String playerName;
    private final String enableMessage;
    private final String reminderMessage;
    private final String disableMessage;

    /**
     * Protected constructor called when initializing prohibition.
     *
     * @param prohibiterPlugin prohibiting plugin
     * @param playerName       prohibited player's name
     * @param enableMessage    message to send when starting prohibition
     * @param reminderMessage  message to send when reminding player of prohibition
     * @param disableMessage   message to send when ending prohibition
     */
    protected AuctionProhibition(Plugin prohibiterPlugin, String playerName, String enableMessage, String reminderMessage, String disableMessage) {
        this.prohibiterPlugin = prohibiterPlugin;
        this.playerName = playerName;
        this.enableMessage = enableMessage;
        this.reminderMessage = reminderMessage;
        this.disableMessage = disableMessage;
    }

    public Plugin getProhibiterPlugin() {
        return this.prohibiterPlugin;
    }

    public String getPlayerName() {
        return this.playerName;
    }

    public String getEnableMessage() {
        return this.enableMessage;
    }

    public String getReminderMessage() {
        return this.reminderMessage;
    }

    public String getDisableMessage() {
        return this.disableMessage;
    }
}
