package com.gmail.virustotalop.obsidianauctions;

import com.clubobsidian.wrappy.Configuration;
import com.gmail.virustotalop.obsidianauctions.area.AreaManager;
import com.gmail.virustotalop.obsidianauctions.auction.Auction;
import com.gmail.virustotalop.obsidianauctions.auction.AuctionLot;
import com.gmail.virustotalop.obsidianauctions.auction.AuctionParticipant;
import com.gmail.virustotalop.obsidianauctions.auction.AuctionProhibitionManager;
import com.gmail.virustotalop.obsidianauctions.auction.AuctionScope;
import com.gmail.virustotalop.obsidianauctions.inject.AuctionModule;
import com.gmail.virustotalop.obsidianauctions.message.MessageManager;
import com.gmail.virustotalop.obsidianauctions.util.FileLoadUtil;
import com.gmail.virustotalop.obsidianauctions.util.Functions;
import com.google.inject.Guice;
import com.google.inject.Injector;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
    private static boolean suspendAllAuctions = false;
    public static boolean isDamagedAllowed;
    public static List<AuctionParticipant> auctionParticipants = new ArrayList<>();
    public static Map<String, String[]> userSavedInputArgs = new HashMap<>();

    // Config files info.
    public static Configuration config = null;
    public static Configuration textConfig = null;
    private static File dataFolder;
    private static int queueTimer;


    private static int playerScopeCheckTimer;
    private static final Map<String, String> playerScopeCache = new HashMap<>();

    private static List<AuctionLot> orphanLots = new ArrayList<>();
    private List<String> voluntarilyDisabledUsers = new ArrayList<>();
    private List<String> suspendedUsers = new ArrayList<>();

    private MessageManager messageManager;
    private AuctionProhibitionManager prohibitionCache;

    //Adventure
    private BukkitAudiences adventure;

    /*Added values
     *
     */
    public static String guiQueueName;
    public static List<String> itemBlacklist;
    public static boolean itemNameBlackListEnabled;
    public static boolean enableChatMessages;
    public static boolean enableActionbarMessages;
    public static boolean allowRenamedItems;

    /* Check if addon plugins are enabled
     *
     */
    public static boolean placeHolderApiEnabled = false;

    /**
     * Used by AuctinLot to store auction lots which could not be given to players because they were offline.
     *
     * @param auctionLot AuctionLot to save.
     */
    public static void saveOrphanLot(AuctionLot auctionLot) {
        ObsidianAuctions.orphanLots.add(auctionLot);
        saveObject(ObsidianAuctions.orphanLots, "orphanLots.ser");
    }

    /**
     * Saves an object to a file.
     *
     * @param object   object to save
     * @param filename name of file
     */
    private static void saveObject(Object object, String filename) {
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
    public static void killOrphan(Player player) {
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
            this.getLogger().log(Level.SEVERE, "Unable to look adventure, shutting down...");
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
        this.bootstrapInject(language);

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
        File savedUserInputsFile = new File(this.getDataFolder(), "userSavedInputArgs.ser");
        orphanLots =  FileLoadUtil.loadListAuctionLot(orphanLotsFile);
        this.voluntarilyDisabledUsers =  FileLoadUtil.loadStringList(voluntarilyDisabledUsersFile);
        suspendedUsers =  FileLoadUtil.loadStringList(suspendedUserFile);
        userSavedInputArgs = FileLoadUtil.loadMapStringStringArray(savedUserInputsFile);

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

    private void bootstrapInject(String language) {
        File itemLanguagesFolder = new File(dataFolder, "item_languages");
        File itemConfig = new File(itemLanguagesFolder, language + ".yml");
        Configuration i18nItemConfig = Configuration.load(itemConfig);

        Injector injector = Guice.createInjector(new AuctionModule(this.adventure, i18nItemConfig));
        this.messageManager = injector.getInstance(MessageManager.class);
        this.prohibitionCache = injector.getInstance(AuctionProhibitionManager.class);
        this.collectInstances(Listener.class, injector).forEach(listener -> {
            this.getServer().getPluginManager().registerEvents(listener, this);
        });
    }

    /**
     * Loads config.yml and language.yml configuration files.
     */
    private void loadConfig() {
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

    // Overrides onCommand from Plugin
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        Player player = null;
        Auction auction = null;
        AuctionScope userScope = null;
        String playerName = null;

        if(sender instanceof Player) {
            player = (Player) sender;
            playerName = player.getName();
            userScope = AuctionScope.getPlayerScope(player);
            if(userScope != null) {
                auction = userScope.getActiveAuction();
            }
        }

        if(
                (cmd.getName().equalsIgnoreCase("auction") || cmd.getName().equalsIgnoreCase("auc")) &&
                        args.length > 0 &&
                        args[0].equalsIgnoreCase("on")
        ) {
            int index = getVoluntarilyDisabledUsers().indexOf(playerName);
            if(index != -1) {
                getVoluntarilyDisabledUsers().remove(index);
            }
            this.messageManager.sendPlayerMessage("auction-enabled", playerName, (AuctionScope) null);
            saveObject(getVoluntarilyDisabledUsers(), "voluntarilyDisabledUsers.ser");
            return true;
        }

        if(getVoluntarilyDisabledUsers().contains(playerName)) {
            getVoluntarilyDisabledUsers().remove(playerName);
            this.messageManager.sendPlayerMessage("auction-fail-disabled", playerName, (AuctionScope) null);
            getVoluntarilyDisabledUsers().add(playerName);
            saveObject(getVoluntarilyDisabledUsers(), "voluntarilyDisabledUsers.ser");
            return true;
        }

        if(
                cmd.getName().equalsIgnoreCase("auc") ||
                        cmd.getName().equalsIgnoreCase("auction") ||
                        cmd.getName().equalsIgnoreCase("sauc") ||
                        cmd.getName().equalsIgnoreCase("sealedauction")
        ) {
            if(args.length > 0) {
                if(args[0].equalsIgnoreCase("reload")) {
                    if(player != null && !perms.has(player, "auction.admin")) {
                        this.messageManager.sendPlayerMessage("plugin-reload-fail-permissions", playerName, (AuctionScope) null);
                        return true;
                    } else if(AuctionScope.areAuctionsRunning()) // Don't reload if any auctions are running.
                    {
                        this.messageManager.sendPlayerMessage("plugin-reload-fail-auctions-running", playerName, (AuctionScope) null);
                        return true;
                    }

                    loadConfig();
                    this.messageManager.sendPlayerMessage("plugin-reloaded", playerName, (AuctionScope) null);
                    return true;
                } else if(args[0].equalsIgnoreCase("resume")) {
                    if(args.length == 1) {
                        if(player != null && !perms.has(player, "auction.admin")) {
                            this.messageManager.sendPlayerMessage("unsuspension-fail-permissions", playerName, (AuctionScope) null);
                            return true;
                        }
                        // Resume globally:
                        suspendAllAuctions = false;
                        this.messageManager.broadcastAuctionScopeMessage("unsuspension-global", null);
                        return true;
                    }

                    if(player != null && !perms.has(player, "auction.admin")) {
                        this.messageManager.sendPlayerMessage("unsuspension-fail-permissions", playerName, (AuctionScope) null);
                        return true;
                    }

                    if(!suspendedUsers.contains(args[1].toLowerCase())) {
                        this.messageManager.sendPlayerMessage("unsuspension-user-fail-not-suspended", playerName, (AuctionScope) null);
                        return true;
                    }

                    suspendedUsers.remove(args[1].toLowerCase());
                    saveObject(suspendedUsers, "suspendedUsers.ser");
                    this.messageManager.sendPlayerMessage("unsuspension-user", args[1], (AuctionScope) null);
                    this.messageManager.sendPlayerMessage("unsuspension-user-success", playerName, (AuctionScope) null);

                    return true;
                } else if(args[0].equalsIgnoreCase("suspend")) {
                    if(player != null && !perms.has(player, "auction.admin")) {
                        this.messageManager.sendPlayerMessage("suspension-fail-permissions", playerName, (AuctionScope) null);
                        return true;
                    }
                    if(args.length > 1) {
                        // Suspend a player:
                        if(suspendedUsers.contains(args[1].toLowerCase())) {
                            this.messageManager.sendPlayerMessage("suspension-user-fail-already-suspended", playerName, (AuctionScope) null);
                            return true;
                        }

                        Player playerToSuspend = getServer().getPlayer(args[1]);

                        if(playerToSuspend == null || !playerToSuspend.isOnline()) {
                            this.messageManager.sendPlayerMessage("suspension-user-fail-is-offline", playerName, (AuctionScope) null);
                            return true;
                        }

                        if(perms.has(playerToSuspend, "auction.admin")) {
                            this.messageManager.sendPlayerMessage("suspension-user-fail-is-admin", playerName, (AuctionScope) null);
                            return true;
                        }

                        suspendedUsers.add(args[1].toLowerCase());
                        saveObject(suspendedUsers, "suspendedUsers.ser");
                        this.messageManager.sendPlayerMessage("suspension-user", playerToSuspend.getName(), (AuctionScope) null);
                        this.messageManager.sendPlayerMessage("suspension-user-success", playerName, (AuctionScope) null);

                        return true;
                    }
                    // Suspend globally:
                    suspendAllAuctions = true;

                    AuctionScope.cancelAllAuctions();

                    this.messageManager.broadcastAuctionScopeMessage("suspension-global", null);

                    return true;
                } else if(
                        args[0].equalsIgnoreCase("start") ||
                                args[0].equalsIgnoreCase("s") ||
                                args[0].equalsIgnoreCase("this") ||
                                args[0].equalsIgnoreCase("hand") ||
                                args[0].equalsIgnoreCase("all") ||
                                args[0].matches("[0-9]+")
                ) {
                    if(suspendAllAuctions) {
                        this.messageManager.sendPlayerMessage("suspension-global", playerName, (AuctionScope) null);
                        return true;
                    }
                    if(player != null && suspendedUsers.contains(playerName.toLowerCase())) {
                        this.messageManager.sendPlayerMessage("suspension-user", playerName, (AuctionScope) null);
                        return true;
                    }

                    // Start new auction!
                    if(player == null) {
                        this.messageManager.sendPlayerMessage("auction-fail-console", playerName, (AuctionScope) null);
                        return true;
                    }
                    if(!AuctionConfig.getBoolean("allow-gamemode-creative", userScope) && player.getGameMode() == GameMode.CREATIVE) {
                        this.messageManager.sendPlayerMessage("auction-fail-gamemode-creative", playerName, (AuctionScope) null);
                        return true;
                    }

                    if(userScope == null) {
                        this.messageManager.sendPlayerMessage("auction-fail-no-scope", playerName, (AuctionScope) null);
                        return true;
                    }

                    if(!perms.has(player, "auction.start")) {
                        this.messageManager.sendPlayerMessage("auction-fail-permissions", playerName, (AuctionScope) null);
                        return true;
                    }

                    if(!AuctionConfig.getBoolean("allow-sealed-auctions", userScope) && !AuctionConfig.getBoolean("allow-unsealed-auctions", userScope)) {
                        this.messageManager.sendPlayerMessage("auction-fail-no-auctions-allowed", playerName, (AuctionScope) null);
                        return true;
                    }

                    if(player.getInventory().getItemInHand() == null || player.getInventory().getItemInHand().getAmount() == 0) {
                        this.messageManager.sendPlayerMessage("auction-fail-hand-is-empty", playerName, (AuctionScope) null);
                        return true;
                    }

                    if(cmd.getName().equalsIgnoreCase("sealedauction") || cmd.getName().equalsIgnoreCase("sauc")) {
                        if(AuctionConfig.getBoolean("allow-sealed-auctions", userScope)) {
                            userScope.queueAuction(new Auction(this, player, args, userScope, true, messageManager, player.getItemInHand().clone()));
                        } else {
                            this.messageManager.sendPlayerMessage("auction-fail-no-sealed-auctions", playerName, (AuctionScope) null);
                        }
                    } else {
                        if(AuctionConfig.getBoolean("allow-unsealed-auctions", userScope)) {
                            userScope.queueAuction(new Auction(this, player, args, userScope, false, messageManager, player.getItemInHand().clone()));
                        } else {
                            userScope.queueAuction(new Auction(this, player, args, userScope, true, messageManager, player.getItemInHand().clone()));
                        }
                    }

                    return true;
                } else if(args[0].equalsIgnoreCase("prep") || args[0].equalsIgnoreCase("p")) {
                    // Save a users individual starting default values.
                    if(player == null) {
                        this.messageManager.sendPlayerMessage("auction-fail-console", playerName, (AuctionScope) null);
                        return true;
                    }
                    if(!perms.has(player, "auction.start")) {
                        this.messageManager.sendPlayerMessage("auction-fail-permissions", playerName, (AuctionScope) null);
                        return true;
                    }

                    // The function returns null and sends error on failure.
                    String[] mergedArgs = Functions.mergeInputArgs(playerName, args, true);

                    if(mergedArgs != null) {
                        ObsidianAuctions.userSavedInputArgs.put(playerName, mergedArgs);
                        ObsidianAuctions.saveObject(ObsidianAuctions.userSavedInputArgs, "userSavedInputArgs.ser");
                        this.messageManager.sendPlayerMessage("prep-save-success", playerName, (AuctionScope) null);
                    }

                    return true;
                } else if(args[0].equalsIgnoreCase("cancel") || args[0].equalsIgnoreCase("c")) {
                    if(userScope == null) {
                        this.messageManager.sendPlayerMessage("auction-fail-no-scope", playerName, (AuctionScope) null);
                        return true;
                    }
                    if(userScope.getActiveAuction() == null && userScope.getAuctionQueueLength() == 0) {
                        this.messageManager.sendPlayerMessage("auction-fail-no-auction-exists", playerName, (AuctionScope) null);
                        return true;
                    }

                    List<Auction> auctionQueue = userScope.getAuctionQueue();
                    for(int i = 0; i < auctionQueue.size(); i++) {
                        if(auctionQueue.get(i).getOwner().equalsIgnoreCase(playerName)) {
                            auctionQueue.remove(i);
                            this.messageManager.sendPlayerMessage("auction-cancel-queued", playerName, (AuctionScope) null);
                            return true;
                        }
                    }

                    if(auction == null) {
                        this.messageManager.sendPlayerMessage("auction-fail-no-auction-exists", playerName, (AuctionScope) null);
                        return true;
                    }

                    if(player == null || player.getName().equalsIgnoreCase(auction.getOwner()) || perms.has(player, "auction.admin")) {
                        if(AuctionConfig.getInt("cancel-prevention-seconds", userScope) > auction.getRemainingTime() || AuctionConfig.getDouble("cancel-prevention-percent", userScope) > (double) auction.getRemainingTime() / (double) auction.getTotalTime() * 100D) {
                            this.messageManager.sendPlayerMessage("auction-fail-cancel-prevention", playerName, (AuctionScope) null);
                        } else {
                            auction.cancel();
                        }
                    } else {
                        this.messageManager.sendPlayerMessage("auction-fail-not-owner-cancel", playerName, (AuctionScope) null);
                    }
                    return true;
                } else if(args[0].equalsIgnoreCase("confiscate") || args[0].equalsIgnoreCase("impound")) {
                    if(auction == null) {
                        this.messageManager.sendPlayerMessage("auction-fail-no-auction-exists", playerName, (AuctionScope) null);
                        return true;
                    }

                    if(player == null) {
                        this.messageManager.sendPlayerMessage("confiscate-fail-console", playerName, (AuctionScope) null);
                        return true;
                    }
                    if(!perms.has(player, "auction.admin")) {
                        this.messageManager.sendPlayerMessage("confiscate-fail-permissions", playerName, (AuctionScope) null);
                        return true;
                    }
                    if(playerName.equalsIgnoreCase(auction.getOwner())) {
                        this.messageManager.sendPlayerMessage("confiscate-fail-self", playerName, (AuctionScope) null);
                        return true;
                    }
                    auction.confiscate(player);
                    return true;
                } else if(args[0].equalsIgnoreCase("end") || args[0].equalsIgnoreCase("e")) {
                    if(auction == null) {
                        this.messageManager.sendPlayerMessage("auction-fail-no-auction-exists", playerName, (AuctionScope) null);
                        return true;
                    }
                    if(!AuctionConfig.getBoolean("allow-early-end", userScope)) {
                        this.messageManager.sendPlayerMessage("auction-fail-no-early-end", playerName, (AuctionScope) null);
                        return true;
                    }
                    if(player.getName().equalsIgnoreCase(auction.getOwner())) {
                        auction.end();
                    } else {
                        this.messageManager.sendPlayerMessage("auction-fail-not-owner-end", playerName, (AuctionScope) null);
                    }
                    return true;
                } else if(
                        args[0].equalsIgnoreCase("stfu") ||
                                args[0].equalsIgnoreCase("ignore") ||
                                args[0].equalsIgnoreCase("quiet") ||
                                args[0].equalsIgnoreCase("off") ||
                                args[0].equalsIgnoreCase("silent") ||
                                args[0].equalsIgnoreCase("silence")
                ) {
                    if(getVoluntarilyDisabledUsers().indexOf(playerName) == -1) {
                        this.messageManager.sendPlayerMessage("auction-disabled", playerName, (AuctionScope) null);
                        getVoluntarilyDisabledUsers().add(playerName);
                        saveObject(getVoluntarilyDisabledUsers(), "voluntarilyDisabledUsers.ser");
                    }
                    return true;
                } else if(args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("i")) {
                    if(auction == null) {
                        this.messageManager.sendPlayerMessage("auction-info-no-auction", playerName, (AuctionScope) null);
                        return true;
                    }
                    auction.info(sender, false);
                    return true;
                } else if(args[0].equalsIgnoreCase("queue") || args[0].equalsIgnoreCase("q")) {
                    List<Auction> auctionQueue = userScope.getAuctionQueue();
                    if(auctionQueue.isEmpty()) {
                        this.messageManager.sendPlayerMessage("auction-queue-status-not-in-queue", playerName, (AuctionScope) null);
                        return true;
                    }
                    Inventory inv = Bukkit.createInventory(null, 18, ObsidianAuctions.guiQueueName);
                    for(int i = 0; i < auctionQueue.size(); i++) {
                        if(i == inv.getSize())
                            break;
                        inv.setItem(i, auctionQueue.get(i).getGuiItem());
                    }
                    player.openInventory(inv);
                    return true;
                }
            }
            this.messageManager.sendPlayerMessage("auction-help", playerName, (AuctionScope) null);
            return true;
        } else if(cmd.getName().equalsIgnoreCase("bid")) {
            if(suspendAllAuctions) {
                this.messageManager.sendPlayerMessage("suspension-global", playerName, (AuctionScope) null);
                return true;
            } else if(player != null && suspendedUsers.contains(playerName.toLowerCase())) {
                this.messageManager.sendPlayerMessage("suspension-user", playerName, (AuctionScope) null);
                return true;
            } else if(player == null) {
                this.messageManager.sendPlayerMessage("bid-fail-console", playerName, (AuctionScope) null);
                return true;
            } else if(!AuctionConfig.getBoolean("allow-gamemode-creative", userScope) && player.getGameMode().equals(GameMode.CREATIVE)) {
                this.messageManager.sendPlayerMessage("bid-fail-gamemode-creative", playerName, (AuctionScope) null);
                return true;
            } else if(!perms.has(player, "auction.bid")) {
                this.messageManager.sendPlayerMessage("bid-fail-permissions", playerName, (AuctionScope) null);
                return true;
            } else if(auction == null) {
                this.messageManager.sendPlayerMessage("bid-fail-no-auction", playerName, (AuctionScope) null);
                return true;
            }
            auction.bid(player, args);
            return true;
        }
        return false;
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
        RegisteredServiceProvider<Permission> rsp = Bukkit.getServicesManager().getRegistration(Permission.class);
        perms = rsp.getProvider();
        return perms != null;
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

    public List<String> getVoluntarilyDisabledUsers() {
        return voluntarilyDisabledUsers;
    }

    public Map<String, String> getPlayerScopeCache() {
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
}