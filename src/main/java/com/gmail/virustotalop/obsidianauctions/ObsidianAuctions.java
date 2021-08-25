package com.gmail.virustotalop.obsidianauctions;

import cloud.commandframework.CommandManager;
import cloud.commandframework.annotations.AnnotationParser;
import cloud.commandframework.bukkit.CloudBukkitCapabilities;
import cloud.commandframework.exceptions.NoPermissionException;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.meta.SimpleCommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import com.clubobsidian.wrappy.Configuration;
import com.gmail.virustotalop.obsidianauctions.area.AreaManager;
import com.gmail.virustotalop.obsidianauctions.auction.Auction;
import com.gmail.virustotalop.obsidianauctions.auction.AuctionLot;
import com.gmail.virustotalop.obsidianauctions.auction.AuctionParticipant;
import com.gmail.virustotalop.obsidianauctions.auction.AuctionProhibitionManager;
import com.gmail.virustotalop.obsidianauctions.auction.AuctionScope;
import com.gmail.virustotalop.obsidianauctions.command.AuctionCommands;
import com.gmail.virustotalop.obsidianauctions.command.CommandPermissionHandler;
import com.gmail.virustotalop.obsidianauctions.inject.AuctionModule;
import com.gmail.virustotalop.obsidianauctions.message.MessageManager;
import com.gmail.virustotalop.obsidianauctions.util.FileLoadUtil;
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

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.logging.Level;

/**
 * A Bukkit based Minecraft plugin to facilitate auctions.
 *
 * @author Joshua "flobi" Hatfield
 */
public class ObsidianAuctions extends JavaPlugin {

    private static ObsidianAuctions instance;

    public static int decimalPlaces = 0;
    public static String decimalRegex = "^[0-9]{0,13}(\\.[0-9]{0,1})?$";
    private File auctionLog = null;
    private boolean suspendAllAuctions = false;
    public static boolean isDamagedAllowed;
    public static List<AuctionParticipant> auctionParticipants = new ArrayList<>();

    // Config files info.
    public static Configuration config = null;
    public static Configuration textConfig = null;
    private static File dataFolder;
    private static int queueTimer;


    private static int playerScopeCheckTimer;
    private static final Map<UUID, String> playerScopeCache = new HashMap<>();

    private static List<AuctionLot> orphanLots = new ArrayList<>();
    private Collection<UUID> voluntarilyDisabledUsers = new HashSet<>();
    private Collection<UUID> suspendedUsers = new HashSet<>();

    private MessageManager messageManager;
    private AuctionProhibitionManager prohibitionCache;

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

    /* Check if addon plugins are enabled
     *
     */
    public static boolean placeHolderApiEnabled = false;

    /**
     * Used by AuctinLot to store auction lots which could not be given to players because they were offline.
     *
     * @param auctionLot AuctionLot to save.
     */
    public void saveOrphanLot(AuctionLot auctionLot) {
        ObsidianAuctions.orphanLots.add(auctionLot);
        saveObject(ObsidianAuctions.orphanLots, "orphanLots.ser");
    }

    /**
     * Saves an object to a file.
     *
     * @param object   object to save
     * @param filename name of file
     */
    private void saveObject(Object object, String filename) {
        File saveFile = new File(dataFolder, filename);
        try {
            if(saveFile.exists()) {
                saveFile.delete();
            }
            FileOutputStream file = new FileOutputStream(saveFile.getAbsolutePath());
            OutputStream buffer = new BufferedOutputStream(file);
            ObjectOutput output = new ObjectOutputStream(buffer);
            try {
                output.writeObject(object);
            } finally {
                output.close();
                buffer.close(); //make sure these are closed
                file.close(); //make sure these are closed
            }
        } catch(IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Attempts to give lost AuctionLots back to their intended destination.
     *
     * @param player the player to check for missing items
     */
    // Eliminate orphan lots (i.e. try to give the items to a player again).
    public void killOrphan(Player player) {
        if(orphanLots != null && orphanLots.size() > 0) {
            Iterator<AuctionLot> iter = orphanLots.iterator();
            while(iter.hasNext()) {
                AuctionLot lot = iter.next();
                if(lot.getOwner().equalsIgnoreCase(player.getName())) {
                    lot.cancelLot();
                    iter.remove();
                }
            }
            saveObject(orphanLots, "orphanLots.ser");
        }
    }


    // Vault objects
    private Economy econ = null;
    private Permission perms = null;
    // private static Chat chat = null;

    /**
     * Called by Bukkit when initializing.  Sets up basic plugin settings.
     */
    @Override
    public void onEnable() {
        this.adventure = BukkitAudiences.create(this);
        if(this.adventure == null) {
            this.getLogger().log(Level.SEVERE, "Unable to create adventure, shutting down...");
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }
        instance = this;
        dataFolder = getDataFolder();
        if(!dataFolder.exists()) {
            dataFolder.mkdir();
        }

        this.auctionLog = new File(dataFolder, "auctions.log");
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

        File languagesDirectory = new File(dataFolder, "item_languages");
        if(!languagesDirectory.exists()) {
            languagesDirectory.mkdirs();
        }

        this.saveResource("item_languages/en-US.yml", false);

        loadConfig();

        if(Bukkit.getPluginManager().getPlugin("Vault") == null) {
            logToBukkit("plugin-disabled-no-vault", Level.SEVERE);
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        if(!this.setupEconomy()) {
            logToBukkit("plugin-disabled-no-economy", Level.SEVERE);
            Bukkit.getPluginManager().disablePlugin(this);
            return;
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
        this.setupPermissions();

        if(Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            ObsidianAuctions.placeHolderApiEnabled = true;
        }

        String language = config.getString("language");
        Injector injector = this.inject(language);
        this.registerListeners(injector);

        AreaManager.loadArenaListeners(this);

        //Load in inventory click listener

        BukkitScheduler bukkitScheduler = getServer().getScheduler();
        if(queueTimer > 0) {
            bukkitScheduler.cancelTask(queueTimer);
        }
        queueTimer = bukkitScheduler.scheduleSyncRepeatingTask(this, () -> AuctionScope.checkAuctionQueue(), 20L, 20L);

        long playerScopeCheckInterval = config.getLong("auctionscope-change-check-interval");
        if(playerScopeCheckTimer > 0) bukkitScheduler.cancelTask(playerScopeCheckTimer);

        if(playerScopeCheckInterval > 0) {
            playerScopeCheckTimer = bukkitScheduler.scheduleSyncRepeatingTask(this, () -> {
                AuctionScope.sendFairwellMessages();
                AuctionScope.sendWelcomeMessages();
            }, playerScopeCheckInterval, playerScopeCheckInterval);
        }

        File orphanLotsFile = new File(this.getDataFolder(), "orphanLots.ser");
        File voluntarilyDisabledUsersFile = new File(this.getDataFolder(), "voluntarilyDisabledUsers.ser");
        File suspendedUserFile = new File(this.getDataFolder(), "suspendedUsers.ser");
        orphanLots = FileLoadUtil.loadListAuctionLot(orphanLotsFile);
        this.voluntarilyDisabledUsers = FileLoadUtil.loadUUIDSet(voluntarilyDisabledUsersFile);
        this.suspendedUsers = FileLoadUtil.loadUUIDSet(suspendedUserFile);

        this.commandManager = this.createCommandManager(injector);
        this.commandParser = new AnnotationParser(this.commandManager,
                CommandSender.class, parameters ->
                SimpleCommandMeta.empty());
        this.commandParser.parse(injector.getInstance(AuctionCommands.class));

        this.messageManager.sendPlayerMessage("plugin-enabled", null, (AuctionScope) null);

    }

    private <T> Collection<T> collectInstances(Class<T> superClazz, Injector injector) {
        Collection<T> bindings = new ArrayList<>();
        injector.getAllBindings().values().forEach(binding -> {
            Class<?> bindingClazz = binding.getKey().getTypeLiteral().getRawType();
            if(superClazz.isAssignableFrom(bindingClazz)) {
                bindings.add((T) binding.getProvider().get());
            }
        });
        return bindings;
    }

    private Injector inject(String language) {
        File itemLanguagesFolder = new File(dataFolder, "item_languages");
        File itemConfig = new File(itemLanguagesFolder, language + ".yml");
        Configuration i18nItemConfig = Configuration.load(itemConfig);

        AuctionModule module = new AuctionModule(this, this.adventure, i18nItemConfig);
        Injector injector = Guice.createInjector(module);
        this.messageManager = injector.getInstance(MessageManager.class);
        this.prohibitionCache = injector.getInstance(AuctionProhibitionManager.class);
        return injector;
    }

    private void registerListeners(Injector injector) {
        this.collectInstances(Listener.class, injector).forEach(listener -> {
            this.getServer().getPluginManager().registerEvents(listener, this);
        });
    }

    /**
     * Loads config.yml and language.yml configuration files.
     */
    public void loadConfig() {
        File configFile = new File(dataFolder, "config.yml");

        File textConfigFile = new File(dataFolder, "language.yml");


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

        // Build auction scopes.
        AuctionScope.setupScopeList(config.getConfigurationSection("auction-scopes"), dataFolder);

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
        AuctionScope.cancelAllAuctions();
        this.getServer().getScheduler().cancelTask(queueTimer);
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
            try(BufferedWriter out = new BufferedWriter(new FileWriter(this.auctionLog, true))) {
                String scopeId = "NOSCOPE";
                if(auctionScope != null) {
                    scopeId = auctionScope.getScopeId();
                }
                String dateStr = (new Date()).toString();
                String strippedMessage = ChatColor.stripColor(message);
                String log = dateStr + " (" + playerName + ", " + scopeId + "): " + strippedMessage;
                out.write(log);
                out.newLine();
                out.flush();
            } catch(IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Setup Vault economy.
     *
     * @return success level
     */
    private boolean setupEconomy() {
        if(Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if(rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    /**
     * Setup Vault permission.
     *
     * @return success level
     */
    private boolean setupPermissions() {
        try {
            Class<?> vaultClazz = Class.forName("net.milkbowl.vault.permission.Permission");
            RegisteredServiceProvider<net.milkbowl.vault.permission.Permission> rsp = Bukkit
                    .getServicesManager()
                    .getRegistration(net.milkbowl.vault.permission.Permission.class);
            perms = rsp.getProvider();
            return perms != null;
        } catch(ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Gets the active auction instance from the scope where the player is.
     *
     * @param playerName player in reference
     * @return auction instance
     */
    public Auction getPlayerAuction(String playerName) {
        if(playerName == null) {
            return null;
        }
        return getPlayerAuction(Bukkit.getPlayer(playerName));
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
        AuctionScope auctionScope = AuctionScope.getPlayerScope(player);
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
        this.saveObject(this.voluntarilyDisabledUsers, "voluntarilyDisabledUsers.ser");
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
        this.saveObject(this.suspendedUsers, "suspendedUsers.ser");
    }

    public Map<UUID, String> getPlayerScopeCache() {
        return playerScopeCache;
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

    private void logToBukkit(String key, Level level) {
        List<String> messageList = AuctionConfig.getLanguageStringList(key, null);

        String originalMessage = null;
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

    public static ObsidianAuctions get() {
        return instance;
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