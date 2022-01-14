package com.gmail.virustotalop.obsidianauctions;

import cloud.commandframework.CommandManager;
import cloud.commandframework.annotations.AnnotationParser;
import cloud.commandframework.bukkit.CloudBukkitCapabilities;
import cloud.commandframework.exceptions.NoPermissionException;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.meta.SimpleCommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import com.clubobsidian.wrappy.Configuration;
import com.gmail.virustotalop.obsidianauctions.auction.Auction;
import com.gmail.virustotalop.obsidianauctions.auction.AuctionLocationManager;
import com.gmail.virustotalop.obsidianauctions.auction.AuctionLot;
import com.gmail.virustotalop.obsidianauctions.auction.AuctionManager;
import com.gmail.virustotalop.obsidianauctions.auction.AuctionProhibitionManager;
import com.gmail.virustotalop.obsidianauctions.auction.AuctionScope;
import com.gmail.virustotalop.obsidianauctions.command.AuctionCommands;
import com.gmail.virustotalop.obsidianauctions.command.CommandPermissionHandler;
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
    public static boolean isDamagedAllowed;

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

    /*Added values
     *
     */
    public static String guiQueueName;
    public static List<String> itemBlacklist;
    public static boolean itemNameBlackListEnabled;
    public static boolean enableChatMessages;
    public static boolean enableActionbarMessages;
    public static boolean allowRenamedItems;
    public static int actionBarTicks;

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
        if(this.orphanLots != null && this.orphanLots.size() > 0) {
            Iterator<AuctionLot> it = this.orphanLots.iterator();
            while(it.hasNext()) {
                AuctionLot lot = it.next();
                if(lot.getOwner().equalsIgnoreCase(player.getName())) {
                    lot.cancelLot();
                    it.remove();
                }
            }
            FileUtil.save(this.orphanLots, "orphanLots.ser");
        }
    }


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
        if(!this.dataFolder.exists()) {
            this.dataFolder.mkdir();
        }

        this.auctionLog = new File(this.dataFolder, "auctions.log");
        if(!this.auctionLog.exists()) {
            try {
                this.auctionLog.createNewFile();
                this.auctionLog.setWritable(true);
            } catch(IOException e) {
                e.printStackTrace();
            }
        }

        this.saveResource("config.yml", false);
        this.saveResource("language.yml", false);

        File languagesDirectory = new File(this.dataFolder, "item_languages");
        if(!languagesDirectory.exists()) {
            languagesDirectory.mkdirs();
        }

        this.saveResource("item_languages/en-US.yml", false);
        this.loadConfig();

        if(Bukkit.getPluginManager().getPlugin("Vault") == null) {
            logToBukkit("plugin-disabled-no-vault", Level.SEVERE);
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        if(!this.setupVault()) {
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        Class<? extends Placeholder> placeholderClazz = NoPlaceholderImpl.class;
        if(Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            placeholderClazz = PapiPlaceholderImpl.class;
        }

        String language = config.getString("language");
        Injector injector = this.inject(config, language, placeholderClazz);
        this.registerListeners(injector);

        //Load in inventory click listener

        BukkitScheduler scheduler = this.getServer().getScheduler();
        scheduler.scheduleSyncRepeatingTask(this, () -> this.auctionManager.checkAuctionQueue(), 20L, 20L);

        long playerScopeCheckInterval = config.getLong("auctionscope-change-check-interval");
        if(playerScopeCheckTimer > 0) scheduler.cancelTask(playerScopeCheckTimer);

        if(playerScopeCheckInterval > 0) {
            playerScopeCheckTimer = scheduler.scheduleSyncRepeatingTask(this, () -> {
                this.auctionManager.sendFarewellMessages();
                this.auctionManager.sendWelcomeMessages();
            }, playerScopeCheckInterval, playerScopeCheckInterval);
        }

        this.orphanLots = FileUtil.load("orphanLots.ser", new ArrayList<>());
        this.voluntarilyDisabledUsers = FileUtil.load("voluntarilyDisabledUsers.ser", new HashSet<>());
        this.suspendedUsers = FileUtil.load("suspendedUsers.ser", new HashSet<>());

        this.commandManager = this.createCommandManager(injector);
        this.commandParser = new AnnotationParser(this.commandManager,
                CommandSender.class, parameters ->
                SimpleCommandMeta.empty());
        this.commandParser.parse(injector.getInstance(AuctionCommands.class));

        this.messageManager.sendPlayerMessage("plugin-enabled", null, (AuctionScope) null);
        scheduler.runTaskTimerAsynchronously(this, this::writeCurrentLog, 20, 20);
    }

    private boolean setupVault() {
        if(!this.setupEconomy()) {
            logToBukkit("plugin-disabled-no-economy", Level.SEVERE);
            return false;
        } else {
            decimalPlaces = Math.max(econ.fractionalDigits(), 0);
            config.set("decimal-places", decimalPlaces);
            if(decimalPlaces < 1) {
                decimalRegex = "^[0-9]{1,13}$";
            } else if(decimalPlaces == 1) {
                decimalRegex = "^[0-9]{0,13}(\\.[0-9])?$";
            } else {
                decimalRegex = "^[0-9]{0,13}(\\.[0-9]{1," + decimalPlaces + "})?$";
            }
        }
        return this.setupPermissions();
    }

    private Injector inject(Configuration config, String itemLanguage, Class<? extends Placeholder> papiClazz) {
        File itemLanguagesFolder = new File(this.dataFolder, "item_languages");
        File itemConfig = new File(itemLanguagesFolder, itemLanguage + ".yml");
        Configuration i18nItemConfig = Configuration.load(itemConfig);

        AuctionModule module = new AuctionModule(this, this.adventure, config, i18nItemConfig, papiClazz);
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
        File configFile = new File(this.dataFolder, "config.yml");

        File textConfigFile = new File(this.dataFolder, "language.yml");


        config = Configuration.load(configFile);
        textConfig = Configuration.load(textConfigFile);

        //TODO - copy defaults
        /*
        InputStream defConfigStream = plugin.getResource("config.yml");
        InputStream defTextConfigStream = plugin.getResource("language.yml");
         Configuration defConfig = null;
        Configuration defTextConfig = null;
        // Look for defaults in the jar
        if(defConfigStream != null) {
            defConfig = Configuration.load(defConfigStream, ConfigurationType.YAML);
            try {
                defConfigStream.close();
            } catch(IOException e) {
                e.printStackTrace();
            }
        }
        if(defConfig != null) {
            config.setDefaults(defConfig);
        }

        textConfig = null;

        // Look for defaults in the jar
        if(defTextConfigStream != null) {
            InputStreamReader reader = new InputStreamReader(defTextConfigStream);
            defTextConfig = Configuration.load(reader);
            try {
                reader.close();
            } catch(IOException e) {
                e.printStackTrace();
            }
            defTextConfigStream = null;
        }
        if(defTextConfig != null) {
            textConfig.setDefaults(defTextConfig);
        }

        // Clean up the configuration of any unused values.
        Configuration cleanConfig = new Configuration();
        Map<String, Object> configValues = config.getDefaults().getValues();
        for(Map.Entry<String, Object> configEntry : configValues.entrySet()) {
            cleanConfig.set(configEntry.getKey(), config.get(configEntry.getKey()));
        }

        config = cleanConfig;
        config.save();


        Configuration cleanTextConfig = new Configuration();
        Map<String, Object> textConfigValues = textConfig.getDefaults().getValues();
        for(Map.Entry<String, Object> textConfigEntry : textConfigValues.entrySet()) {
            cleanTextConfig.set(textConfigEntry.getKey(), textConfig.get(textConfigEntry.getKey()));
        }
        textConfig = cleanTextConfig;

        textConfig.save();*/

        //Gui queue inventory name
        ObsidianAuctions.guiQueueName = ChatColor.translateAlternateColorCodes('&', config.getString("queue-gui-name"));
        ObsidianAuctions.itemBlacklist = config.getStringList("name-blacklist");
        ObsidianAuctions.itemNameBlackListEnabled = config.getBoolean("name-blacklist-enabled");
        ObsidianAuctions.enableChatMessages = config.getBoolean("enable-chat-messages");
        ObsidianAuctions.enableActionbarMessages = config.getBoolean("enable-actionbar-messages");
        ObsidianAuctions.allowRenamedItems = config.getBoolean("allow-renamed-items");
        ObsidianAuctions.actionBarTicks = config.get("action-bar-ticks", Integer.class, 60);

        //Setup additional floAuction values
        ObsidianAuctions.isDamagedAllowed = config.getBoolean("allow-damaged-items");

        //make values null at the end
        //defConfig = null;
        configFile = null;
        //defTextConfig = null;
        textConfigFile = null;
    }

    /**
     * Called by Bukkit when disabling.  Cancels all auctions and clears data.
     */
    @Override
    public void onDisable() {
        this.writeCurrentLog();
        this.auctionManager.cancelAllAuctions();
        instance = null;
        this.logToBukkit("plugin-disabled", Level.INFO);
        this.auctionLog = null;
        if(this.adventure != null) {
            this.adventure.close();
            this.adventure = null;
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
        if(AuctionConfig.getBoolean("log-auctions", auctionScope)) {
            String scopeId = "NOSCOPE";
            if(auctionScope != null) {
                scopeId = auctionScope.getScopeId();
            }
            String dateStr = (new Date()).toString();
            String strippedMessage = ChatColor.stripColor(message);
            String log = dateStr + " (" + playerName + ", " + scopeId + "): " + strippedMessage;
            this.logQueue.add(log);
        }
    }

    private void writeCurrentLog() {
        try(BufferedWriter out = new BufferedWriter(new FileWriter(this.auctionLog, true))) {
            String polled;
            while((polled = this.logQueue.poll()) != null) {
                out.write(polled);
                out.newLine();
            }
            out.flush();
        } catch(IOException e) {
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
            if(rsp == null) {
                return false;
            }
            this.econ = rsp.getProvider();
            return this.econ != null;
        } catch(ClassNotFoundException ex) {
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
        } catch(ClassNotFoundException e) {
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
        if(player == null) {
            return null;
        }
        AuctionScope auctionScope = this.auctionManager.getPlayerScope(player);
        if(auctionScope == null) {
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
     * Prepares chat, prepending prefix and removing colors.
     *
     * @param message      message to prepare
     * @param auctionScope the scope of the destination
     * @return prepared message
     */
    private static String chatPrepClean(String message, AuctionScope auctionScope) {
        message = AuctionConfig.getLanguageString("chat-prefix", auctionScope) + message;
        message = ChatColor.translateAlternateColorCodes('&', message);
        message = ChatColor.stripColor(message);
        return message;
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

    private void logToBukkit(String key, Level level) {
        List<String> messageList = AuctionConfig.getLanguageStringList(key, null);

        String originalMessage;
        if(messageList == null || messageList.size() == 0) {
            originalMessage = AuctionConfig.getLanguageString(key, null);

            if(originalMessage != null && originalMessage.length() != 0) {
                messageList = Arrays.asList(originalMessage.split("(\r?\n|\r)"));
            }
        }
        for(Iterator<String> i = messageList.iterator(); i.hasNext(); ) {
            String messageListItem = i.next();
            this.getLogger().log(level, chatPrepClean(messageListItem, null));
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
            PaperCommandManager<CommandSender> commandManager = new PaperCommandManager(this,
                    CommandExecutionCoordinator.simpleCoordinator(),
                    Function.identity(),
                    Function.identity());
            CommandPermissionHandler handler = injector.getInstance(CommandPermissionHandler.class);
            commandManager.registerExceptionHandler(NoPermissionException.class, handler);
            if (commandManager.queryCapability(CloudBukkitCapabilities.BRIGADIER)) {
                commandManager.registerBrigadier();
            }
            return commandManager;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}