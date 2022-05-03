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

import com.clubobsidian.wrappy.ConfigurationSection;
import com.gmail.virustotalop.obsidianauctions.AuctionConfig;
import com.gmail.virustotalop.obsidianauctions.ObsidianAuctions;
import com.gmail.virustotalop.obsidianauctions.message.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Class to manage different auction areas or scopes.
 *
 * @author Joshua "flobi" Hatfield
 */

public class AuctionScope {

    private Auction activeAuction = null;
    private final String scopeId;
    private final String name;
    private final String type;
    private final List<Auction> auctionQueue = new ArrayList<>();
    private long lastAuctionDestroyTime = 0;

    // Definitions
    private List<String> worlds = null;
    private Location minHouseLocation = null;
    private Location maxHouseLocation = null;
    private boolean locationChecked = false;

    private final ConfigurationSection config;
    private final ConfigurationSection textConfig;

    /**
     * Constructor to make new scopes from the name, config and language config files.
     *
     * @param scopeId    name of scope
     * @param config     configuration
     * @param textConfig language config
     */
    public AuctionScope(String scopeId, ConfigurationSection config, ConfigurationSection textConfig) {
        this.scopeId = scopeId;
        this.name = this.resolveName(config);
        this.type = config.getString("type");
        this.config = config;
        this.textConfig = textConfig;
    }

    /**
     * Checks whether the scopes definition is valid.
     *
     * @return true if valid location, false if invalid
     */
    private boolean scopeLocationIsValid() {
        if (this.locationChecked) {
            return worlds != null || this.minHouseLocation != null || this.maxHouseLocation != null;
        } else if (this.type.equalsIgnoreCase("worlds")) {
            this.worlds = config.getStringList("worlds");
        } else if (type.equalsIgnoreCase("house")) {
            String world = config.getString("house-world");
            if (world == null || world.isEmpty()) {
                this.minHouseLocation = null;
                this.maxHouseLocation = null;
            } else {
                double configMinX = this.config.getDouble("house-min-x");
                double configMinY = this.config.getDouble("house-min-y");
                double configMinZ = this.config.getDouble("house-min-z");
                double configMaxX = this.config.getDouble("house-max-x");
                double configMaxY = this.config.getDouble("house-max-y");
                double configMaxZ = this.config.getDouble("house-max-z");
                double minX = Math.min(configMinX, configMaxX);
                double minY = Math.min(configMinY, configMaxY);
                double minZ = Math.min(configMinZ, configMaxZ);
                double maxX = Math.max(configMinX, configMaxX);
                double maxY = Math.max(configMinY, configMaxY);
                double maxZ = Math.max(configMinZ, configMaxZ);
                this.minHouseLocation = new Location(Bukkit.getWorld(world), minX, minY, minZ);
                this.maxHouseLocation = new Location(Bukkit.getWorld(world), maxX, maxY, maxZ);
            }
        }
        this.locationChecked = true;
        return this.worlds != null || this.minHouseLocation != null || this.maxHouseLocation != null;
    }

    private String resolveName(ConfigurationSection config) {
        String name = config.getString("name");
        return name != null ? name : this.scopeId;
    }

    /**
     * Retrieves and instance of the active auction from this scope.
     *
     * @return active auction instance
     */
    public Auction getActiveAuction() {
        return this.activeAuction;
    }

    /**
     * Gets the number of queued auctions.
     *
     * @return length of auction queue
     */
    public int getAuctionQueueLength() {
        return this.auctionQueue.size();
    }

    /**
     * Sets the passed in auction instance as the active auction.
     *
     * @param auction auction instance to set as active
     */
    public void setActiveAuction(Auction auction) {
        if (this.activeAuction != null && auction == null) {
            this.lastAuctionDestroyTime = System.currentTimeMillis();
            ObsidianAuctions.get().getAuctionManager().checkAuctionQueue();
        }
        this.activeAuction = auction;
    }

    /**
     * Adds an auction instance to the auction queue for this scope.
     *
     * @param auctionToQueue auction instance to queue
     */
    public void queueAuction(Auction auctionToQueue) {
        UUID playerUUID = auctionToQueue.getOwnerUUID();
        MessageManager messageManager = auctionToQueue.getMessageManager();

        if (this.activeAuction == null) {
            // Queuing because of interval not yet timed out.
            // Allow a queue of 1 to override if 0 for this condition.
            if (Math.max(AuctionConfig.getInt("max-auction-queue-length", this), 1) <= this.auctionQueue.size()) {
                messageManager.sendPlayerMessage("auction-queue-fail-full", playerUUID, auctionToQueue);
                return;
            }
        } else {
            if (AuctionConfig.getInt("max-auction-queue-length", this) <= 0) {
                messageManager.sendPlayerMessage("auction-fail-auction-exists", playerUUID, auctionToQueue);
                return;
            } else if (this.activeAuction.getOwnerUUID().equals(playerUUID)) {
                messageManager.sendPlayerMessage("auction-queue-fail-current-auction", playerUUID, auctionToQueue);
                return;
            } else if (AuctionConfig.getInt("max-auction-queue-length", this) <= auctionQueue.size()) {
                messageManager.sendPlayerMessage("auction-queue-fail-full", playerUUID, auctionToQueue);
                return;
            }
        }
        for (Auction auction : this.auctionQueue) {
            if (auction != null) {
                if (auction.getOwnerUUID().equals(playerUUID)) {
                    messageManager.sendPlayerMessage("auction-queue-fail-in-queue", playerUUID, auctionToQueue);
                    return;
                }
            }
        }
        if ((this.auctionQueue.size() == 0 && System.currentTimeMillis() - this.lastAuctionDestroyTime >= AuctionConfig.getInt("min-auction-interval-secs", this) * 1000L) || auctionToQueue.isValid()) {
            this.auctionQueue.add(auctionToQueue);
            ObsidianAuctions.get().getAuctionManager().addParticipant(playerUUID, this);
            ObsidianAuctions.get().getAuctionManager().checkAuctionQueue();
            if (this.auctionQueue.contains(auctionToQueue)) {
                messageManager.sendPlayerMessage("auction-queue-enter", playerUUID, auctionToQueue);
            }
        }
    }

    /**
     * Checks the auction queue to see if the next auction is ready to start.
     */
    void checkThisAuctionQueue() {
        if (this.activeAuction != null) {
            return;
        } else if (System.currentTimeMillis() - this.lastAuctionDestroyTime < AuctionConfig.getInt("min-auction-interval-secs", this) * 1000L) {
            return;
        } else if (this.auctionQueue.size() == 0) {
            return;
        }
        Auction auction = this.auctionQueue.remove(0);
        if (auction == null) {
            return;
        }
        MessageManager messageManager = auction.getMessageManager();

        UUID playerUUID = auction.getOwnerUUID();
        Player player = Bukkit.getPlayer(playerUUID);
        if (player == null || !player.isOnline()) {
            return;
        } else if (ObsidianAuctions.get().getProhibitionManager().isOnProhibition(auction.getOwnerUUID(), false)) {
            messageManager.sendPlayerMessage("remote-plugin-prohibition-reminder", playerUUID, auction);
            return;
        } else if (!AuctionConfig.getBoolean("allow-gamemode-creative", this) && player.getGameMode() == GameMode.CREATIVE) {
            messageManager.sendPlayerMessage("auction-fail-gamemode-creative", playerUUID, auction);
            return;
        } else if (!ObsidianAuctions.get().getPermission().has(player, "auction.start")) {
            messageManager.sendPlayerMessage("auction-fail-permissions", playerUUID, auction);
            return;
        } else if (!auction.isValid()) {
            return;
        }
        this.activeAuction = auction;
        if (!auction.start()) {
            this.activeAuction = null;
        }
    }

    /**
     * Checks to see if the player is inside the scope boundaries.
     *
     * @param player player to check
     * @return whether he's in the scope
     */
    public boolean isPlayerInScope(Player player) {
        if (player == null) {
            return false;
        }
        return this.isLocationInScope(player.getLocation());
    }

    /**
     * Checks to see if a location is inside the scope boundaries.
     *
     * @param location location to check
     * @return whether it's in the scope
     */
    public boolean isLocationInScope(Location location) {
        if (location == null) {
            return false;
        }
        World world = location.getWorld();
        if (world == null) {
            return false;
        }
        String worldName = world.getName();
        if (!scopeLocationIsValid()) {
            return false;
        }
        if (this.type.equalsIgnoreCase("worlds")) {
            for (String s : this.worlds) {
                if (s.equalsIgnoreCase(worldName) || s.equalsIgnoreCase("*")) {
                    return true;
                }
            }
        } else if (this.type.equalsIgnoreCase("house")) {
            if (this.minHouseLocation == null || this.maxHouseLocation == null) {
                return false;
            } else if (!location.getWorld().equals(this.minHouseLocation.getWorld())) {
                return false;
            } else if (location.getX() > Math.max(this.minHouseLocation.getX(), this.maxHouseLocation.getX()) || location.getX() < Math.min(minHouseLocation.getX(), this.maxHouseLocation.getX())) {
                return false;
            } else if (location.getZ() > Math.max(this.minHouseLocation.getZ(), this.maxHouseLocation.getZ()) || location.getZ() < Math.min(minHouseLocation.getZ(), this.maxHouseLocation.getZ())) {
                return false;
            } else
                return !(location.getY() > Math.max(this.minHouseLocation.getY(), this.maxHouseLocation.getY())) && !(location.getY() < Math.min(this.minHouseLocation.getY(), this.maxHouseLocation.getY()));
        }
        return false;
    }

    /**
     * Retrieves the configuration for this scope.
     *
     * @return config for the scope
     */
    @ApiStatus.Internal
    public ConfigurationSection getConfig() {
        return this.config.getConfigurationSection("config");
    }

    /**
     * Gets the name of the scope.
     *
     * @return name of the scope
     */
    public String getName() {
        return this.name;
    }

    /**
     * Gets the id of the scope.
     *
     * @return id of the scope
     */
    public String getScopeId() {
        return this.scopeId;
    }

    /**
     * Retrieves the language configuration for this scope.
     *
     * @return language config for the scope
     */
    @ApiStatus.Internal
    public ConfigurationSection getTextConfig() {
        return this.textConfig;
    }

    /**
     * Retrieves the list of auctions queued.
     *
     * @return auction queue
     */
    public List<Auction> getAuctionQueue() {
        return this.auctionQueue;
    }

    /**
     * Gets the position of the named player's auction in the queue or zero if not in queue.
     *
     * @param playerUUID uuid of player
     * @return players position in queue or zero if not in queue
     */
    public int getQueuePosition(UUID playerUUID) {
        for (int i = 0; i < this.auctionQueue.size(); i++) {
            Auction auction = this.auctionQueue.get(i);
            if (auction.getOwnerUUID().equals(playerUUID)) {
                return i + 1;
            }
        }
        return 0;
    }

    @ApiStatus.Internal
    public void clearAuctionQueue() {
        this.auctionQueue.clear();
    }

    public boolean cancelActiveAuction() {
        if (this.activeAuction != null) {
            this.activeAuction.cancel();
            return true;
        }
        return false;
    }
}