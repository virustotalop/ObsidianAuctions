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

package com.gmail.virustotalop.obsidianauctions;

import com.clubobsidian.wrappy.Configuration;
import com.gmail.virustotalop.obsidianauctions.auction.Auction;
import com.gmail.virustotalop.obsidianauctions.auction.AuctionLocationManager;
import com.gmail.virustotalop.obsidianauctions.auction.AuctionLot;
import com.gmail.virustotalop.obsidianauctions.auction.AuctionManager;
import com.gmail.virustotalop.obsidianauctions.auction.AuctionProhibitionManager;
import com.gmail.virustotalop.obsidianauctions.auction.AuctionScope;
import com.gmail.virustotalop.obsidianauctions.command.AuctionCommands;
import com.gmail.virustotalop.obsidianauctions.config.ConfigMigrator;
import com.gmail.virustotalop.obsidianauctions.inject.AuctionModule;
import com.gmail.virustotalop.obsidianauctions.message.MessageManager;
import com.gmail.virustotalop.obsidianauctions.placeholder.NoPlaceholderImpl;
import com.gmail.virustotalop.obsidianauctions.placeholder.PapiPlaceholderImpl;
import com.gmail.virustotalop.obsidianauctions.placeholder.Placeholder;
import com.gmail.virustotalop.obsidianauctions.util.FileUtil;
import com.gmail.virustotalop.obsidianauctions.util.InjectUtil;
import com.google.inject.Guice;
import com.google.inject.Injector;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.SenderMapper;
import org.incendo.cloud.annotations.AnnotationParser;
import org.incendo.cloud.brigadier.BrigadierSetting;
import org.incendo.cloud.bukkit.CloudBukkitCapabilities;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.meta.SimpleCommandMeta;
import org.incendo.cloud.paper.LegacyPaperCommandManager;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;
import java.util.logging.Level;

/**
 * ObsidianAuctions
 * A Bukkit based Minecraft plugin to facilitate auctions.
 *
 * @author Joshua "flobi" Hatfield
 * @author virustotalop
 */
public class ObsidianAuctions extends JavaPlugin {

    private static ObsidianAuctions instance;

    public static ObsidianAuctions get() {
        return instance;
    }

    public static int decimalPlaces = 0;
    public static String decimalRegex = "^[0-9]{0,13}(\\.[0-9]{0,1})?$";
    private File auctionLog = null;
    private boolean suspendAllAuctions = false;

    // Config files info.
    public static Configuration config = null;
    public static Configuration textConfig = null;
    private File dataFolder;

    private static int playerScopeCheckTimer;

    private List<AuctionLot> orphanLots = new ArrayList<>();
    private Collection<UUID> voluntarilyDisabledUsers = new HashSet<>();
    private Collection<UUID> suspendedUsers = new HashSet<>();

    private Queue<String> logQueue;

    private MessageManager messageManager;
    private AuctionProhibitionManager prohibitionCache;
    private AuctionLocationManager locationManager;
    private AuctionManager auctionManager;

    //Adventure
    private BukkitAudiences adventure;

    //Cloud
    private CommandManager<CommandSender> commandManager;
    private AnnotationParser<CommandSender> commandParser;

    // Vault objects
    private Economy econ = null;
    private Permission perms = null;

    /**
     * Called by Bukkit when initializing.  Sets up basic plugin settings.
     */
    @Override
    public void onEnable() {

        this.logQueue = new ConcurrentLinkedQueue<>();
        this.adventure = BukkitAudiences.create(this);
        instance = this;
        this.dataFolder = this.getDataFolder();
        if (!this.dataFolder.exists()) {
            this.dataFolder.mkdir();
        }

        this.auctionLog = new File(this.dataFolder, "auctions.log");
        if (!this.auctionLog.exists()) {
            try {
                this.auctionLog.createNewFile();
                this.auctionLog.setWritable(true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        this.saveResource("config.yml", false);
        this.saveResource("language.yml", false);

        File languagesDirectory = new File(this.dataFolder, "item_languages");
        if (!languagesDirectory.exists()) {
            languagesDirectory.mkdirs();
        }

        this.saveResource("item_languages/en-US.yml", false);
        this.loadConfig();

        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        if (!this.setupVault()) {
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        Class<? extends Placeholder> placeholderClazz = NoPlaceholderImpl.class;
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            placeholderClazz = PapiPlaceholderImpl.class;
        }

        String language = config.getString("language");
        Injector injector = this.inject(config, language, placeholderClazz);
        this.registerListeners(injector);

        BukkitScheduler scheduler = this.getServer().getScheduler();
        scheduler.scheduleSyncRepeatingTask(this, () -> this.auctionManager.checkAuctionQueue(), 20L, 20L);

        long playerScopeCheckInterval = config.getLong("auctionscope-change-check-interval");
        if (playerScopeCheckTimer > 0) scheduler.cancelTask(playerScopeCheckTimer);

        if (playerScopeCheckInterval > 0) {
            playerScopeCheckTimer = scheduler.scheduleSyncRepeatingTask(this, () -> {
                this.auctionManager.sendFarewellMessages();
                this.auctionManager.sendWelcomeMessages();
            }, playerScopeCheckInterval, playerScopeCheckInterval);
        }

        this.orphanLots = FileUtil.load("orphanLots.ser", new ArrayList<>());
        this.voluntarilyDisabledUsers = FileUtil.load("voluntarilyDisabledUsers.ser", new HashSet<>());
        this.suspendedUsers = FileUtil.load("suspendedUsers.ser", new HashSet<>());

        this.commandManager = this.createCommandManager(injector);
        this.commandParser = new AnnotationParser<>(this.commandManager,
                CommandSender.class, parameters ->
                SimpleCommandMeta.empty());
        this.commandParser.parse(injector.getInstance(AuctionCommands.class));
        scheduler.runTaskTimerAsynchronously(this, this::writeCurrentLog, 20, 20);
    }

    private boolean setupVault() {
        if (!this.setupEconomy()) {
            logToBukkit(Key.PLUGIN_DISABLED_NO_ECONOMY, Level.SEVERE);
            return false;
        } else {
            decimalPlaces = Math.max(econ.fractionalDigits(), 0);
            config.set("decimal-places", decimalPlaces);
            if (decimalPlaces < 1) {
                decimalRegex = "^[0-9]{1,13}$";
            } else if (decimalPlaces == 1) {
                decimalRegex = "^[0-9]{0,13}(\\.[0-9])?$";
            } else {
                decimalRegex = "^[0-9]{0,13}(\\.[0-9]{1," + decimalPlaces + "})?$";
            }
        }
        return this.setupPermissions();
    }

    private Injector inject(Configuration config, String itemLanguage, Class<? extends Placeholder> placeholderClazz) {
        File itemLanguagesFolder = new File(this.dataFolder, "item_languages");
        File itemConfig = new File(itemLanguagesFolder, itemLanguage + ".yml");
        Configuration i18nItemConfig = Configuration.load(itemConfig);

        AuctionModule module = new AuctionModule(this, this.adventure, config, i18nItemConfig, placeholderClazz);
        Injector injector = Guice.createInjector(module);
        this.messageManager = injector.getInstance(MessageManager.class);
        this.prohibitionCache = injector.getInstance(AuctionProhibitionManager.class);
        this.locationManager = injector.getInstance(AuctionLocationManager.class);
        this.auctionManager = injector.getInstance(AuctionManager.class);
        return injector;
    }

    private void registerListeners(Injector injector) {
        InjectUtil.collect(Listener.class, injector).forEach(listener -> {
            this.getServer().getPluginManager().registerEvents(listener, this);
        });
    }

    /**
     * Loads config.yml and language.yml configuration files.
     */
    public void loadConfig() {
        File mainConfigFile = new File(this.dataFolder, "config.yml");
        File languageConfigFile = new File(this.dataFolder, "language.yml");
        int version = Configuration.load(mainConfigFile).getInteger("config-version");
        this.loadMainConfig(mainConfigFile, version);
        this.loadLanguageConfig(languageConfigFile, version);
    }

    private void loadMainConfig(File configFile, int version) {
        config = Configuration.load(configFile);
        ConfigMigrator.migrateMainConfig(config, version);
    }

    private void loadLanguageConfig(File textConfigFile, int version) {
        textConfig = Configuration.load(textConfigFile);
        ConfigMigrator.migrateLanguageConfig(textConfig, version);
    }

    /**
     * Called by Bukkit when disabling.  Cancels all auctions and clears data.
     */
    @Override
    public void onDisable() {
        this.writeCurrentLog();
        this.auctionManager.cancelAllAuctions();
        instance = null;
        this.auctionLog = null;
        if (this.adventure != null) {
            this.adventure.close();
            this.adventure = null;
        }
    }

    /**
     * Used by AuctionLot to store auction lots which could not be given to players because they were offline.
     *
     * @param auctionLot AuctionLot to save.
     */
    public void saveOrphanLot(AuctionLot auctionLot) {
        this.orphanLots.add(auctionLot);
        FileUtil.save(this.orphanLots, "orphanLots.ser");
    }

    /**
     * Attempts to give lost AuctionLots back to their intended destination.
     *
     * @param player the player to check for missing items
     */
    // Eliminate orphan lots (i.e. try to give the items to a player again).
    public void killOrphan(Player player) {
        if (this.orphanLots != null && this.orphanLots.size() > 0) {
            Iterator<AuctionLot> it = this.orphanLots.iterator();
            while (it.hasNext()) {
                AuctionLot lot = it.next();
                if (lot.getOwner().equalsIgnoreCase(player.getName())) {
                    lot.cancelLot();
                    it.remove();
                }
            }
            FileUtil.save(this.orphanLots, "orphanLots.ser");
        }
    }

    /**
     * Log data to the floAuction log file if logging is enabled.
     *
     * @param playerName   who is initiating the logged event
     * @param message      message to save
     * @param auctionScope the auction scope being referenced if any
     */
    public void log(String playerName, String message, AuctionScope auctionScope) {
        if (AuctionConfig.getBoolean(Key.LOG_AUCTIONS, auctionScope)) {
            String scopeId = "NOSCOPE";
            if (auctionScope != null) {
                scopeId = auctionScope.getScopeId();
            }
            String dateStr = (new Date()).toString();
            String strippedMessage = ChatColor.stripColor(message);
            String log = dateStr + " (" + playerName + ", " + scopeId + "): " + strippedMessage;
            this.logQueue.add(log);
        }
    }

    private void writeCurrentLog() {
        try (BufferedWriter out = new BufferedWriter(new FileWriter(this.auctionLog, true))) {
            String polled;
            while ((polled = this.logQueue.poll()) != null) {
                out.write(polled);
                out.newLine();
            }
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Setup Vault economy.
     *
     * @return success level
     */
    private boolean setupEconomy() {
        try {
            Class.forName("net.milkbowl.vault.economy.Economy");
            RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
            if (rsp == null) {
                return false;
            }
            this.econ = rsp.getProvider();
            return this.econ != null;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }

    /**
     * Setup Vault permission.
     *
     * @return success level
     */
    private boolean setupPermissions() {
        try {
            Class.forName("net.milkbowl.vault.permission.Permission");
            RegisteredServiceProvider<net.milkbowl.vault.permission.Permission> rsp = Bukkit
                    .getServicesManager()
                    .getRegistration(net.milkbowl.vault.permission.Permission.class);
            this.perms = rsp.getProvider();
            return this.perms != null;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Gets the active auction instance from the scope where the player is.
     *
     * @param player player in reference
     * @return auction instance
     */
    public Auction getPlayerAuction(Player player) {
        if (player == null) {
            return null;
        }
        AuctionScope auctionScope = this.auctionManager.getPlayerScope(player);
        if (auctionScope == null) {
            return null;
        }
        return auctionScope.getActiveAuction();
    }

    public boolean isVoluntarilyDisabled(UUID uuid) {
        return this.voluntarilyDisabledUsers.contains(uuid);
    }

    public boolean addVoluntarilyDisabled(UUID uuid) {
        return this.voluntarilyDisabledUsers.add(uuid);
    }

    public boolean removeVoluntarilyDisabled(UUID uuid) {
        return this.voluntarilyDisabledUsers.remove(uuid);
    }

    public void saveVoluntarilyDisabled() {
        FileUtil.save(this.voluntarilyDisabledUsers, "voluntarilyDisabledUsers.ser");
    }

    public boolean isSuspendedUser(UUID uuid) {
        return this.suspendedUsers.contains(uuid);
    }

    public boolean addSuspendedUser(UUID uuid) {
        return this.suspendedUsers.add(uuid);
    }

    public boolean removeSuspendedUser(UUID uuid) {
        return this.suspendedUsers.remove(uuid);
    }

    public void saveSuspendedUsers() {
        FileUtil.save(this.suspendedUsers, "suspendedUsers.ser");
    }

    /**
     * Prepares chat removing colors.
     *
     * @param message message to prepare
     * @return prepared message
     */
    private String chatPrepClean(String message) {
        return MiniMessage.miniMessage().stripTags(message);
    }

    public MessageManager getMessageManager() {
        return this.messageManager;
    }

    public AuctionProhibitionManager getProhibitionManager() {
        return this.prohibitionCache;
    }

    public Economy getEconomy() {
        return this.econ;
    }

    public Permission getPermission() {
        return this.perms;
    }

    public AuctionLocationManager getAuctionLocationManager() {
        return this.locationManager;
    }

    public AuctionManager getAuctionManager() {
        return this.auctionManager;
    }

    private void logToBukkit(Key key, Level level) {
        List<String> messageList = AuctionConfig.getLanguageStringList(key, null);

        String originalMessage;
        if (messageList == null || messageList.size() == 0) {
            originalMessage = AuctionConfig.getLanguageString(key, null);

            if (originalMessage != null && originalMessage.length() != 0) {
                messageList = Arrays.asList(originalMessage.split("(\r?\n|\r)"));
            }
        }
        if (messageList != null) {
            for (String messageListItem : messageList) {
                this.getLogger().log(level, chatPrepClean(messageListItem));
            }
        }
    }

    public void setSuspendAllAuctions(boolean suspend) {
        this.suspendAllAuctions = suspend;
    }

    public boolean getSuspendAllAuctions() {
        return this.suspendAllAuctions;
    }

    private CommandManager<CommandSender> createCommandManager(Injector injector) {
        try {
            LegacyPaperCommandManager<CommandSender> commandManager = new LegacyPaperCommandManager<>(
                    this,
                    ExecutionCoordinator.simpleCoordinator(),
                    SenderMapper.identity()
            );
            if (commandManager.hasBrigadierManager()) {
                commandManager.brigadierManager().settings().set(BrigadierSetting.FORCE_EXECUTABLE, true);
            }
            if (commandManager.hasCapability(CloudBukkitCapabilities.ASYNCHRONOUS_COMPLETION)) {
                commandManager.registerAsynchronousCompletions();
            }
            return commandManager;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}