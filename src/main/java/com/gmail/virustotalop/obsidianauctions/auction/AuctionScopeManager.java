package com.gmail.virustotalop.obsidianauctions.auction;

import com.clubobsidian.wrappy.Configuration;
import com.clubobsidian.wrappy.ConfigurationSection;
import com.gmail.virustotalop.obsidianauctions.ObsidianAuctions;
import com.gmail.virustotalop.obsidianauctions.inject.annotation.Config;
import com.google.inject.Inject;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.ApiStatus;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AuctionScopeManager {

    private final List<String> auctionScopesOrder = new ArrayList<>();
    private final Map<String, AuctionScope> auctionScopes = new HashMap<>();
    private final Map<UUID, String> playerScopeCache = new HashMap<>();

    @Inject
    private AuctionScopeManager(JavaPlugin plugin, @Config Configuration config) {
        this.loadScopeList(config, plugin);
    }

    private void loadScopeList(ConfigurationSection config, JavaPlugin plugin) {
        File dataFolder = plugin.getDataFolder();
        ConfigurationSection auctionScopesConfig = config.getConfigurationSection("auction-scopes");
        this.auctionScopes.clear();
        this.auctionScopesOrder.clear();
        if(auctionScopesConfig != null) {
            for(String scopeName : auctionScopesConfig.getKeys()) {
                this.auctionScopesOrder.add(scopeName);
                ConfigurationSection auctionScopeConfig = auctionScopesConfig.getConfigurationSection(scopeName);
                File scopeTextConfigFile = new File(dataFolder, "language-" + scopeName + ".yml");
                Configuration scopeTextConfig = null;
                if(scopeTextConfigFile.exists()) {
                    scopeTextConfig = Configuration.load(scopeTextConfigFile);
                }
                AuctionScope auctionScope = new AuctionScope(scopeName, auctionScopeConfig, scopeTextConfig);
                this.auctionScopes.put(scopeName, auctionScope);
            }
        }
    }


    /**
     * Gets the AuctionScope instance in which the player is.
     *
     * @param player player to check
     * @return scope where the player is
     */
    public AuctionScope getPlayerScope(Player player) {
        if(player == null) {
            return null;
        }
        return this.getLocationScope(player.getLocation());
    }

    /**
     * Gets the AuctionScope instance in which the location is.
     *
     * @param location the location to check
     * @return scope where the location is
     */
    public AuctionScope getLocationScope(Location location) {
        if(location == null) {
            return null;
        }
        for(String auctionScopeId : this.auctionScopesOrder) {
            AuctionScope auctionScope = this.auctionScopes.get(auctionScopeId);
            if(auctionScope.isLocationInScope(location)) {
                return auctionScope;
            }
        }
        return null;
    }

    /**
     * Cancels all running auctions AKA the Big red button.
     */
    public void cancelAllAuctions() {
        for(Map.Entry<String, AuctionScope> auctionScopesEntry : this.auctionScopes.entrySet()) {
            AuctionScope auctionScope = auctionScopesEntry.getValue();
            auctionScope.clearAuctionQueue();
            auctionScope.cancelActiveAuction();
        }
    }

    /**
     * Checks to see if any auctions are running.
     *
     * @return whether at least one auction is running
     */
    public boolean areAuctionsRunning() {
        for(Map.Entry<String, AuctionScope> auctionScopesEntry : this.auctionScopes.entrySet()) {
            AuctionScope auctionScope = auctionScopesEntry.getValue();
            if(auctionScope.getActiveAuction() != null || auctionScope.getAuctionQueueLength() > 0) {
                return true;
            }
        }
        return false;
    }

    @ApiStatus.Internal
    public void sendFarewellMessages() {
        Iterator<UUID> playerIterator = this.playerScopeCache.keySet().iterator();
        while(playerIterator.hasNext()) {
            UUID playerUUID = playerIterator.next();
            if(!AuctionParticipant.isParticipating(playerUUID)) {
                Player player = Bukkit.getPlayer(playerUUID);
                if(player != null && player.isOnline()) {
                    String oldScopeId = this.playerScopeCache.get(playerUUID);
                    AuctionScope oldScope = this.auctionScopes.get(oldScopeId);
                    AuctionScope playerScope = this.getPlayerScope(player);
                    String playerScopeId = null;
                    if(playerScope != null) {
                        playerScopeId = playerScope.getScopeId();
                    }
                    if(playerScopeId == null || playerScopeId.isEmpty() || !playerScopeId.equalsIgnoreCase(oldScopeId)) {
                        ObsidianAuctions.get().getMessageManager().sendPlayerMessage("auctionscope-fairwell", playerUUID, oldScope);
                        playerIterator.remove();
                        this.playerScopeCache.remove(playerUUID);
                    }
                }
            }
        }
    }

    @ApiStatus.Internal
    public void sendWelcomeMessages() {
        Collection<? extends Player> players = Bukkit.getServer().getOnlinePlayers();
        for(Player player : players) {
            this.sendWelcomeMessage(player, false);
        }
    }

    @ApiStatus.Internal
    public void sendWelcomeMessage(Player player, boolean isOnJoin) {
        String welcomeMessageKey = "auctionscope-welcome";
        if(isOnJoin) {
            welcomeMessageKey += "-onjoin";
        }
        UUID playerUUID = player.getUniqueId();
        if(!AuctionParticipant.isParticipating(playerUUID)) {
            AuctionScope playerScope = this.getPlayerScope(player);
            String oldScopeId = this.playerScopeCache.get(playerUUID);
            if(playerScope == null) {
                if(oldScopeId != null) {
                    this.playerScopeCache.remove(playerUUID);
                }
            } else {
                if(oldScopeId == null || oldScopeId.isEmpty() || !oldScopeId.equalsIgnoreCase(playerScope.getScopeId())) {
                    ObsidianAuctions.get().getMessageManager().sendPlayerMessage(welcomeMessageKey, playerUUID, playerScope);
                    this.playerScopeCache.put(playerUUID, playerScope.getScopeId());
                }
            }
        }
    }

    /**
     * Checks the auction queues for each scope, starting any auctions which are ready.
     */
    public void checkAuctionQueue() {
        for(Map.Entry<String, AuctionScope> auctionScopesEntry : this.auctionScopes.entrySet()) {
            auctionScopesEntry.getValue().checkThisAuctionQueue();
        }
    }

    @ApiStatus.Internal
    void clearPlayerScope(Player player) {
        this.playerScopeCache.remove(player.getUniqueId());
    }
}