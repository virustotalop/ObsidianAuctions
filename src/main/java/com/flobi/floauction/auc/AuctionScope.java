package com.flobi.floauction.auc;

import com.flobi.floauction.AuctionConfig;
import com.flobi.floauction.FloAuction;
import com.flobi.floauction.message.MessageManager;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Class to manage different auction areas or scopes.
 *
 * @author Joshua "flobi" Hatfield
 */

public class AuctionScope {

    private Auction activeAuction = null;
    private final List<Auction> otherPluginsAuctions = null;
    private String scopeId = null;
    private String name = null;
    private String type = null;
    private final ArrayList<Auction> auctionQueue = new ArrayList<Auction>();
    private long lastAuctionDestroyTime = 0;

    // Definitions
    private List<String> worlds = null;
    private Location minHouseLocation = null;
    private Location maxHouseLocation = null;
    private String regionId = null;
    private boolean locationChecked = false;

    private ConfigurationSection config = null;
    private ConfigurationSection textConfig = null;

    public static List<String> auctionScopesOrder = new ArrayList<String>();
    public static Map<String, AuctionScope> auctionScopes = new HashMap<String, AuctionScope>();
    private static WorldGuardPlugin worldGuardPlugin = null;

    /**
     * Constructor to make new scopes from the name, config and language config files.
     *
     * @param scopeId    name of scope
     * @param config     configuration
     * @param textConfig language config
     */
    private AuctionScope(String scopeId, ConfigurationSection config, ConfigurationSection textConfig) {
        this.scopeId = scopeId;
        this.name = config.getString("name");

        if(this.name == null)
            this.name = scopeId;

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
        if(this.locationChecked) {
            return worlds != null || this.minHouseLocation != null || this.maxHouseLocation != null || this.regionId != null;
        } else if(this.type.equalsIgnoreCase("worlds")) {
            this.worlds = config.getStringList("worlds");
        } else if(type.equalsIgnoreCase("house")) {
            String world = config.getString("house-world");
            if(world == null || world.isEmpty()) {
                this.minHouseLocation = null;
                this.maxHouseLocation = null;
            } else {
                this.minHouseLocation = new Location(Bukkit.getWorld(world), this.config.getDouble("house-min-x"), this.config.getDouble("house-min-y"), this.config.getDouble("house-min-z"));
                this.maxHouseLocation = new Location(Bukkit.getWorld(world), this.config.getDouble("house-max-x"), this.config.getDouble("house-max-y"), this.config.getDouble("house-max-z"));
            }
        } else if(this.type.equalsIgnoreCase("worldguardregion")) {
            if(worldGuardPlugin == null) {
                // get the list of regions that contain the given location
                Plugin plugin = Bukkit.getPluginManager().getPlugin("WorldGuard");

                // WorldGuard may not be loaded
                if(plugin != null && plugin instanceof WorldGuardPlugin) {
                    worldGuardPlugin = (WorldGuardPlugin) plugin;
                }
            }
            this.regionId = config.getString("region-id");
        }
        this.locationChecked = true;
        return this.worlds != null || this.minHouseLocation != null || this.maxHouseLocation != null || this.regionId != null;
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
        if(this.activeAuction != null && auction == null) {
            this.lastAuctionDestroyTime = System.currentTimeMillis();
            checkAuctionQueue();
        }
        this.activeAuction = auction;
    }

    /**
     * Adds an auction instance to the auction queue for this scope.
     *
     * @param auctionToQueue auction instance to queue
     */
    public void queueAuction(Auction auctionToQueue) {
        String playerName = auctionToQueue.getOwner();
        MessageManager messageManager = auctionToQueue.messageManager;

        if(this.activeAuction == null) {
            // Queuing because of interval not yet timed out.
            // Allow a queue of 1 to override if 0 for this condition.
            if(Math.max(AuctionConfig.getInt("max-auction-queue-length", this), 1) <= this.auctionQueue.size()) {
                messageManager.sendPlayerMessage("auction-queue-fail-full", playerName, auctionToQueue);
                return;
            }
        } else {
            if(AuctionConfig.getInt("max-auction-queue-length", this) <= 0) {
                messageManager.sendPlayerMessage("auction-fail-auction-exists", playerName, auctionToQueue);
                return;
            } else if(this.activeAuction.getOwner().equalsIgnoreCase(playerName)) {
                messageManager.sendPlayerMessage("auction-queue-fail-current-auction", playerName, auctionToQueue);
                return;
            } else if(AuctionConfig.getInt("max-auction-queue-length", this) <= auctionQueue.size()) {
                messageManager.sendPlayerMessage("auction-queue-fail-full", playerName, auctionToQueue);
                return;
            }
        }
        for(int i = 0; i < this.auctionQueue.size(); i++) {
            if(this.auctionQueue.get(i) != null) {
                Auction queuedAuction = this.auctionQueue.get(i);
                if(queuedAuction.getOwner().equalsIgnoreCase(playerName)) {
                    messageManager.sendPlayerMessage("auction-queue-fail-in-queue", playerName, auctionToQueue);
                    return;
                }
            }
        }
        if((this.auctionQueue.size() == 0 && System.currentTimeMillis() - this.lastAuctionDestroyTime >= AuctionConfig.getInt("min-auction-interval-secs", this) * 1000) || auctionToQueue.isValid()) {
            this.auctionQueue.add(auctionToQueue);
            AuctionParticipant.addParticipant(playerName, this);
            AuctionScope.checkAuctionQueue();
            if(this.auctionQueue.contains(auctionToQueue)) {
                messageManager.sendPlayerMessage("auction-queue-enter", playerName, auctionToQueue);
            }
        }
    }

    /**
     * Checks the auction queue to see if the next auction is ready to start.
     */
    private void checkThisAuctionQueue() {
        if(this.activeAuction != null) {
            return;
        } else if(System.currentTimeMillis() - this.lastAuctionDestroyTime < AuctionConfig.getInt("min-auction-interval-secs", this) * 1000) {
            return;
        } else if(this.auctionQueue.size() == 0) {
            return;
        }
        Auction auction = this.auctionQueue.remove(0);
        if(auction == null) {
            return;
        }
        MessageManager messageManager = auction.messageManager;

        String playerName = auction.getOwner();
        Player player = Bukkit.getPlayer(playerName);
        if(player == null || !player.isOnline()) {
            return;
        } else if(AuctionProhibition.isOnProhibition(auction.getOwner(), false)) {
            messageManager.sendPlayerMessage("remote-plugin-prohibition-reminder", playerName, auction);
            return;
        } else if(!AuctionConfig.getBoolean("allow-gamemode-creative", this) && player.getGameMode() == GameMode.CREATIVE) {
            messageManager.sendPlayerMessage("auction-fail-gamemode-creative", playerName, auction);
            return;
        } else if(!FloAuction.perms.has(player, "auction.start")) {
            messageManager.sendPlayerMessage("auction-fail-permissions", playerName, auction);
            return;
        } else if(!auction.isValid()) {
            return;
        }
        this.activeAuction = auction;
        if(!auction.start()) {
            this.activeAuction = null;
        }
    }

    /**
     * Checks to see if the player is inside the scope boundaries.
     *
     * @param player player to check
     * @return whether he's in the scope
     */
    private boolean isPlayerInScope(Player player) {
        if(player == null) {
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
    private boolean isLocationInScope(Location location) {
        if(location == null) {
            return false;
        }
        World world = location.getWorld();
        if(world == null) {
            return false;
        }
        String worldName = world.getName();
        if(!scopeLocationIsValid()) {
            return false;
        }
        if(this.type.equalsIgnoreCase("worlds")) {
            for(int i = 0; i < worlds.size(); i++) {
                if(this.worlds.get(i).equalsIgnoreCase(worldName) || this.worlds.get(i).equalsIgnoreCase("*")) {
                    return true;
                }
            }
        } else if(this.type.equalsIgnoreCase("house")) {
            if(this.minHouseLocation == null || this.maxHouseLocation == null) {
                return false;
            } else if(!location.getWorld().equals(this.minHouseLocation.getWorld())) {
                return false;
            } else if(location.getX() > Math.max(this.minHouseLocation.getX(), this.maxHouseLocation.getX()) || location.getX() < Math.min(minHouseLocation.getX(), this.maxHouseLocation.getX())) {
                return false;
            } else if(location.getZ() > Math.max(this.minHouseLocation.getZ(), this.maxHouseLocation.getZ()) || location.getZ() < Math.min(minHouseLocation.getZ(), this.maxHouseLocation.getZ())) {
                return false;
            } else return !(location.getY() > Math.max(this.minHouseLocation.getY(), this.maxHouseLocation.getY())) && !(location.getY() < Math.min(this.minHouseLocation.getY(), this.maxHouseLocation.getY()));
		} else if(this.type.equalsIgnoreCase("worldguardregion")) {
            if(worldGuardPlugin == null) {
                return false;
            }
            RegionManager regionManager = AuctionScope.worldGuardPlugin.getRegionManager(location.getWorld());
            ApplicableRegionSet applicableRegions = regionManager.getApplicableRegions(location);
            for(ProtectedRegion region : applicableRegions) {
                if(region.getId().equalsIgnoreCase(this.regionId)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Retrieves the configuration for this scope.
     *
     * @return config for the scope
     */
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
     * Checks the auction queues for each scope, starting any auctions which are ready.
     */
    public static void checkAuctionQueue() {
        for(Map.Entry<String, AuctionScope> auctionScopesEntry : auctionScopes.entrySet()) {
            auctionScopesEntry.getValue().checkThisAuctionQueue();
        }
    }

    /**
     * Gets the position of the named player's auction in the queue or zero if not in queue.
     *
     * @param playerName name of player
     * @return players position in queue or zero if not in queue
     */
    public int getQueuePosition(String playerName) {
        for(int t = 0; t < this.auctionQueue.size(); t++) {
            Auction auction = this.auctionQueue.get(t);
            if(auction.getOwner().equalsIgnoreCase(playerName)) {
                return t + 1;
            }
        }
        return 0;
    }

    /**
     * Gets the AuctionScope instance in which the player is.
     *
     * @param player player to check
     * @return scope where the player is
     */
    public static AuctionScope getPlayerScope(Player player) {
        if(player == null) {
            return null;
        }
        for(int i = 0; i < AuctionScope.auctionScopesOrder.size(); i++) {
            String auctionScopeId = AuctionScope.auctionScopesOrder.get(i);
            AuctionScope auctionScope = AuctionScope.auctionScopes.get(auctionScopeId);
            if(auctionScope.isPlayerInScope(player)) {
                return auctionScope;
            }
        }
        return null;
    }

    /**
     * Gets the AuctionScope instance in which the location is.
     *
     * @param location the location to check
     * @return scope where the location is
     */
    public static AuctionScope getLocationScope(Location location) {
        if(location == null) {
            return null;
        }
        for(int i = 0; i < AuctionScope.auctionScopesOrder.size(); i++) {
            String auctionScopeId = AuctionScope.auctionScopesOrder.get(i);
            AuctionScope auctionScope = AuctionScope.auctionScopes.get(auctionScopeId);
            if(auctionScope.isLocationInScope(location)) {
                return auctionScope;
            }
        }
        return null;
    }

    /**
     * Builds list of AuctionScope instances based on the configuration loaded by the plugin.
     *
     * @param auctionScopesConfig
     * @param dataFolder
     */
    public static void setupScopeList(ConfigurationSection auctionScopesConfig, File dataFolder) {
        AuctionScope.auctionScopes.clear();
        AuctionScope.auctionScopesOrder.clear();
        if(auctionScopesConfig != null) {
            for(String scopeName : auctionScopesConfig.getKeys(false)) {
                AuctionScope.auctionScopesOrder.add(scopeName);
                ConfigurationSection auctionScopeConfig = auctionScopesConfig.getConfigurationSection(scopeName);
                File scopeTextConfigFile = new File(dataFolder, "language-" + scopeName + ".yml");
                YamlConfiguration scopeTextConfig = null;
                if(scopeTextConfigFile.exists()) {
                    scopeTextConfig = YamlConfiguration.loadConfiguration(scopeTextConfigFile);
                }
                AuctionScope auctionScope = new AuctionScope(scopeName, auctionScopeConfig, scopeTextConfig);
                AuctionScope.auctionScopes.put(scopeName, auctionScope);
            }
        }
    }

    /**
     * Big red button.
     */
    public static void cancelAllAuctions() {
        for(Map.Entry<String, AuctionScope> auctionScopesEntry : AuctionScope.auctionScopes.entrySet()) {
            AuctionScope auctionScope = auctionScopesEntry.getValue();
            auctionScope.auctionQueue.clear();
            if(auctionScope.activeAuction != null) {
                auctionScope.activeAuction.cancel();
            }
        }
    }

    /**
     * Checks to see if any auctions are running.
     *
     * @return
     */
    public static boolean areAuctionsRunning() {
        for(Map.Entry<String, AuctionScope> auctionScopesEntry : AuctionScope.auctionScopes.entrySet()) {
            AuctionScope auctionScope = auctionScopesEntry.getValue();
            if(auctionScope.getActiveAuction() != null || auctionScope.getAuctionQueueLength() > 0) {
                return true;
            }
        }
        return false;
    }

    public int getOtherPluginsAuctionsLength() {
        if(this.otherPluginsAuctions == null) {
            return 0;
        }
        return this.otherPluginsAuctions.size();
    }

    public static void sendFairwellMessages() {
        Iterator<String> playerIterator = FloAuction.getPlayerScopeCache().keySet().iterator();
        while(playerIterator.hasNext()) {
            String playerName = playerIterator.next();
            if(!AuctionParticipant.isParticipating(playerName)) {
                Player player = Bukkit.getPlayer(playerName);
                if(player != null && player.isOnline()) {
                    String oldScopeId = FloAuction.getPlayerScopeCache().get(playerName);
                    AuctionScope oldScope = AuctionScope.auctionScopes.get(oldScopeId);
                    AuctionScope playerScope = AuctionScope.getPlayerScope(player);
                    String playerScopeId = null;
                    if(playerScope != null) {
                        playerScopeId = playerScope.getScopeId();
                    }
                    if(playerScopeId == null || playerScopeId.isEmpty() || !playerScopeId.equalsIgnoreCase(oldScopeId)) {
                        FloAuction.getMessageManager().sendPlayerMessage("auctionscope-fairwell", playerName, oldScope);
                        playerIterator.remove();
                        FloAuction.getPlayerScopeCache().remove(playerName);
                    }
                }
            }
        }
    }

    public static void sendWelcomeMessages() {
        Collection<? extends Player> players = Bukkit.getServer().getOnlinePlayers();
        for(Player player : players) {
            AuctionScope.sendWelcomeMessage(player, false);
        }
    }

    public static void sendWelcomeMessage(Player player, boolean isOnJoin) {
        String welcomeMessageKey = "auctionscope-welcome";
        if(isOnJoin) {
            welcomeMessageKey += "-onjoin";
        }
        String playerName = player.getName();
        if(!AuctionParticipant.isParticipating(playerName)) {
            AuctionScope playerScope = AuctionScope.getPlayerScope(player);
            String oldScopeId = FloAuction.getPlayerScopeCache().get(playerName);
            if(playerScope == null) {
                if(oldScopeId != null) {
                    FloAuction.getPlayerScopeCache().remove(playerName);
                }
            } else {
                if(oldScopeId == null || oldScopeId.isEmpty() || !oldScopeId.equalsIgnoreCase(playerScope.getScopeId())) {
                    FloAuction.getMessageManager().sendPlayerMessage(welcomeMessageKey, playerName, playerScope);
                    FloAuction.getPlayerScopeCache().put(playerName, playerScope.getScopeId());
                }
            }
        }
    }
}