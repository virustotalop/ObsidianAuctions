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

package com.gmail.virustotalop.obsidianauctions.auction;

import org.bukkit.plugin.Plugin;

import java.util.UUID;

/**
 * Class to manipulate remote plugin prohibitions on player auctioning.
 *
 * @author Joshua "flobi" Hatfield
 */
public class AuctionProhibition {

    private final Plugin prohibitingPlugin;
    private final UUID playerUUID;
    private final String enableMessage;
    private final String reminderMessage;
    private final String disableMessage;

    /**
     * Protected constructor called when initializing prohibition.
     *
     * @param prohibitingPlugin prohibiting plugin
     * @param playerUUID        prohibited player's uuid
     * @param enableMessage     message to send when starting prohibition
     * @param reminderMessage   message to send when reminding player of prohibition
     * @param disableMessage    message to send when ending prohibition
     */
    protected AuctionProhibition(Plugin prohibitingPlugin, UUID playerUUID, String enableMessage, String reminderMessage, String disableMessage) {
        this.prohibitingPlugin = prohibitingPlugin;
        this.playerUUID = playerUUID;
        this.enableMessage = enableMessage;
        this.reminderMessage = reminderMessage;
        this.disableMessage = disableMessage;
    }

    public Plugin getProhibitingPlugin() {
        return this.prohibitingPlugin;
    }

    public UUID getPlayerUUID() {
        return this.playerUUID;
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
