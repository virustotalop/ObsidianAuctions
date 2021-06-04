package com.gmail.virustotalop.obsidianauctions;

import com.clubobsidian.wrappy.Configuration;
import com.gmail.virustotalop.obsidianauctions.area.AreaManager;
import com.gmail.virustotalop.obsidianauctions.auc.Auction;
import com.gmail.virustotalop.obsidianauctions.auc.AuctionLot;
import com.gmail.virustotalop.obsidianauctions.auc.AuctionParticipant;
import com.gmail.virustotalop.obsidianauctions.auc.AuctionScope;
import com.gmail.virustotalop.obsidianauctions.listener.InventoryClickListener;
import com.gmail.virustotalop.obsidianauctions.message.AuctionMessageManager;
import com.gmail.virustotalop.obsidianauctions.message.MessageManager;
import com.gmail.virustotalop.obsidianauctions.util.Functions;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
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

    public static int decimalPlaces = 0;
    public static String decimalRegex = "^[0-9]{0,13}(\\.[0-9]{0,1})?$";
    public static boolean loadedDecimalFromVault = false;
    private static File auctionLog = null;
    private static boolean suspendAllAuctions = false;
    public static boolean isDamagedAllowed;
    public static List<AuctionParticipant> auctionParticipants = new ArrayList<>();
    public static Map<String, String[]> userSavedInputArgs = new HashMap<>();

    // Config files info.
    public static Configuration config = null;
    public static Configuration textConfig = null;
    private static File dataFolder;
    private static int queueTimer;
    public static ObsidianAuctions plugin;

    private static int playerScopeCheckTimer;
    private static final Map<String, String> playerScopeCache = new HashMap<>();

    private static List<AuctionLot> orphanLots = new ArrayList<>();
    private static List<String> voluntarilyDisabledUsers = new ArrayList<>();
    private static List<String> suspendedUsers = new ArrayList<>();

    private static final MessageManager messageManager = new AuctionMessageManager();


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
     * Load a String array from a file.
     *
     * @param filename where the file is
     * @return the resulting string array
     */
    @SuppressWarnings({"unchecked", "finally"})
    private static List<String> loadStringList(String filename) {
        File saveFile = new File(dataFolder, filename);
        List<String> importedObjects = new ArrayList<String>();
        try {
            InputStream file = new FileInputStream(saveFile.getAbsolutePath());
            InputStream buffer = new BufferedInputStream(file);
            ObjectInput input = new ObjectInputStream(buffer);
            importedObjects = (List<String>) input.readObject();
            input.close();
            buffer.close(); //make sure these are closed
            file.close(); //make sure these are closed
        } finally {
            return importedObjects;
        }
    }

    /**
     * Load a String String map from a file.
     *
     * @param filename where the file is
     * @return the resulting string string map
     */
    @SuppressWarnings({"unchecked", "finally"})
    private static Map<String, String[]> loadMapStringStringArray(String filename) {
        File saveFile = new File(dataFolder, filename);
        Map<String, String[]> importedObjects = new HashMap<String, String[]>();
        try {
            InputStream file = new FileInputStream(saveFile.getAbsolutePath());
            InputStream buffer = new BufferedInputStream(file);
            ObjectInput input = new ObjectInputStream(buffer);
            importedObjects = (Map<String, String[]>) input.readObject();
            input.close();
            buffer.close();//make sure these are closed
            file.close();//make sure these are closed
        } finally {
            return importedObjects;
        }
    }

    /**
     * Load a list of AuctionLot from a file.
     *
     * @param filename where the file is
     * @return the loaded list
     */
    @SuppressWarnings("unchecked")
    private static List<AuctionLot> loadListAuctionLot(String filename) {
        File saveFile = new File(dataFolder, filename);
        List<AuctionLot> importedObjects = new ArrayList<>();
        try {
            InputStream file = new FileInputStream(saveFile.getAbsolutePath());
            InputStream buffer = new BufferedInputStream(file);
            ObjectInput input = new ObjectInputStream(buffer);
            importedObjects = (List<AuctionLot>) input.readObject();
            input.close();
            buffer.close(); //make sure these are closed
            file.close();   //make sure these are closed
        } catch(IOException | ClassNotFoundException ex) {
            ex.printStackTrace();
        }

        return importedObjects;
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
    public static Economy econ = null;
    public static Permission perms = null;
    // private static Chat chat = null;

    /**
     * Called by Bukkit when initializing.  Sets up basic plugin settings.
     */
    @Override
    public void onEnable() {
        dataFolder = getDataFolder();
        plugin = this;
        auctionLog = new File(dataFolder, "auctions.log");

        loadConfig();

        if(Bukkit.getPluginManager().getPlugin("Vault") == null) {
            logToBukkit("plugin-disabled-no-vault", Level.SEVERE);
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        this.setupEconomy();
        this.setupPermissions();

        if(Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            ObsidianAuctions.placeHolderApiEnabled = true;
        }

        if(econ == null) {
            logToBukkit("plugin-disabled-no-economy", Level.SEVERE);
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        AreaManager.loadArenaListeners(this);

        //Load in inventory click listener
        Bukkit.getPluginManager().registerEvents(new InventoryClickListener(), this);

        Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void playerJoin(PlayerJoinEvent event) {
                Player player = event.getPlayer();
                ObsidianAuctions.killOrphan(player);
                AuctionScope.sendWelcomeMessage(player, true);
            }

            @EventHandler(priority = EventPriority.LOWEST)
            public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
                // Hopefully the teleport and portal things I just added will make this obsolete, but I figure I'll keep it just to make sure.
                AuctionParticipant.forceLocation(event.getPlayer().getName(), null);
            }

            @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
            public void onPlayerChangedGameMode(PlayerGameModeChangeEvent event) {
                Player player = event.getPlayer();
                String playerName = player.getName();
                AuctionScope playerScope = AuctionScope.getPlayerScope(player);
                Auction playerAuction = getPlayerAuction(player);
                if(AuctionConfig.getBoolean("allow-gamemode-change", playerScope) || playerAuction == null) {
                    return;
                }

                if(AuctionParticipant.isParticipating(playerName)) {
                    event.setCancelled(true);
                    messageManager.sendPlayerMessage("gamemodechange-fail-participating", playerName, (AuctionScope) null);
                }
            }

            @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
            public void onPlayerPreprocessCommand(PlayerCommandPreprocessEvent event) {
                Player player = event.getPlayer();
                if(player == null) {
                    return;
                }
                String playerName = player.getName();
                String message = event.getMessage();
                if(message == null || message.isEmpty()) {
                    return;
                }

                AuctionScope playerScope = AuctionScope.getPlayerScope(player);

                // Check inscope disabled commands, doesn't matter if participating:
                List<String> disabledCommands = AuctionConfig.getStringList("disabled-commands-inscope", playerScope);
                for(int i = 0; i < disabledCommands.size(); i++) {
                    String disabledCommand = disabledCommands.get(i);
                    if(disabledCommand.isEmpty()) continue;
                    if(message.toLowerCase().startsWith(disabledCommand.toLowerCase())) {
                        event.setCancelled(true);
                        messageManager.sendPlayerMessage("disabled-command-inscope", playerName, (AuctionScope) null);
                        return;
                    }
                }

                // Check participating disabled commands
                if(playerScope == null) {
                    return;
                }
                if(!AuctionParticipant.isParticipating(player.getName())) {
                    return;
                }

                disabledCommands = AuctionConfig.getStringList("disabled-commands-participating", playerScope);
                for(int i = 0; i < disabledCommands.size(); i++) {
                    String disabledCommand = disabledCommands.get(i);
                    if(disabledCommand.isEmpty()) {
                        continue;
                    }
                    if(message.toLowerCase().startsWith(disabledCommand.toLowerCase())) {
                        event.setCancelled(true);
                        messageManager.sendPlayerMessage("disabled-command-participating", playerName, (AuctionScope) null);
                        return;
                    }
                }
            }

            @EventHandler(ignoreCancelled = true)
            public void onPlayerMove(PlayerMoveEvent event) {
                AuctionParticipant.forceLocation(event.getPlayer().getName(), event.getTo());
            }

            @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
            public void onPlayerTeleport(PlayerTeleportEvent event) {
                if(!AuctionParticipant.checkTeleportLocation(event.getPlayer().getName(), event.getTo()))
                    event.setCancelled(true);
            }

            @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
            public void onPlayerPortalEvent(PlayerPortalEvent event) {
                if(!AuctionParticipant.checkTeleportLocation(event.getPlayer().getName(), event.getTo()))
                    event.setCancelled(true);
            }
        }, this);

        BukkitScheduler bukkitScheduler = getServer().getScheduler();
        if(queueTimer > 0) {
            bukkitScheduler.cancelTask(queueTimer);
        }
        queueTimer = bukkitScheduler.scheduleSyncRepeatingTask(this, new Runnable() {
            public void run() {
                AuctionScope.checkAuctionQueue();
            }
        }, 20L, 20L);

        long playerScopeCheckInterval = config.getLong("auctionscope-change-check-interval");
        if(playerScopeCheckTimer > 0) bukkitScheduler.cancelTask(playerScopeCheckTimer);

        if(playerScopeCheckInterval > 0) {
            playerScopeCheckTimer = bukkitScheduler.scheduleSyncRepeatingTask(this, new Runnable() {
                public void run() {
                    AuctionScope.sendFairwellMessages();
                    AuctionScope.sendWelcomeMessages();
                }
            }, playerScopeCheckInterval, playerScopeCheckInterval);
        }

        orphanLots = loadListAuctionLot("orphanLots.ser");
        ObsidianAuctions.voluntarilyDisabledUsers = loadStringList("voluntarilyDisabledUsers.ser");
        suspendedUsers = loadStringList("suspendedUsers.ser");
        userSavedInputArgs = loadMapStringStringArray("userSavedInputArgs.ser");

        messageManager.sendPlayerMessage("plugin-enabled", null, (AuctionScope) null);

    }

    /**
     * Loads config.yml and language.yml configuration files.
     */
    private void loadConfig() {
        File configFile = new File(dataFolder, "config.yml");
        InputStream defConfigStream = plugin.getResource("config.yml");
		File textConfigFile = new File(dataFolder, "language.yml");
        InputStream defTextConfigStream = plugin.getResource("language.yml");

        Configuration defConfig = null;
        Configuration defTextConfig = null;

        config = Configuration.load(configFile);

        //TODO - copy defaults
        /*
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
        ObsidianAuctions.isDamagedAllowed = defConfig.getBoolean("allow-damaged-items");

        //make values null at the end
        defConfig = null;
        configFile = null;
        defTextConfig = null;
        textConfigFile = null;
    }

    /**
     * Called by Bukkit when disabling.  Cancels all auctions and clears data.
     */
    @Override
    public void onDisable() {
        AuctionScope.cancelAllAuctions();
        this.getServer().getScheduler().cancelTask(queueTimer);
        ObsidianAuctions.plugin = null;
        this.logToBukkit("plugin-disabled", Level.INFO);
        ObsidianAuctions.auctionLog = null;
    }

    // Overrides onCommand from Plugin
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        // Make sure the decimalPlaces loaded correctly.
        // Sometimes the econ loads after floAuction.
        if(!loadedDecimalFromVault && econ.isEnabled()) {
            loadedDecimalFromVault = true;
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
            messageManager.sendPlayerMessage("auction-enabled", playerName, (AuctionScope) null);
            saveObject(getVoluntarilyDisabledUsers(), "voluntarilyDisabledUsers.ser");
            return true;
        }

        if(getVoluntarilyDisabledUsers().contains(playerName)) {
            getVoluntarilyDisabledUsers().remove(playerName);
            messageManager.sendPlayerMessage("auction-fail-disabled", playerName, (AuctionScope) null);
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
                        messageManager.sendPlayerMessage("plugin-reload-fail-permissions", playerName, (AuctionScope) null);
                        return true;
                    } else if(AuctionScope.areAuctionsRunning()) // Don't reload if any auctions are running.
                    {
                        messageManager.sendPlayerMessage("plugin-reload-fail-auctions-running", playerName, (AuctionScope) null);
                        return true;
                    }

                    loadConfig();
                    messageManager.sendPlayerMessage("plugin-reloaded", playerName, (AuctionScope) null);
                    return true;
                } else if(args[0].equalsIgnoreCase("resume")) {
                    if(args.length == 1) {
                        if(player != null && !perms.has(player, "auction.admin")) {
                            messageManager.sendPlayerMessage("unsuspension-fail-permissions", playerName, (AuctionScope) null);
                            return true;
                        }
                        // Resume globally:
                        suspendAllAuctions = false;
                        messageManager.broadcastAuctionScopeMessage("unsuspension-global", null);
                        return true;
                    }

                    if(player != null && !perms.has(player, "auction.admin")) {
                        messageManager.sendPlayerMessage("unsuspension-fail-permissions", playerName, (AuctionScope) null);
                        return true;
                    }

                    if(!suspendedUsers.contains(args[1].toLowerCase())) {
                        messageManager.sendPlayerMessage("unsuspension-user-fail-not-suspended", playerName, (AuctionScope) null);
                        return true;
                    }

                    suspendedUsers.remove(args[1].toLowerCase());
                    saveObject(suspendedUsers, "suspendedUsers.ser");
                    messageManager.sendPlayerMessage("unsuspension-user", args[1], (AuctionScope) null);
                    messageManager.sendPlayerMessage("unsuspension-user-success", playerName, (AuctionScope) null);

                    return true;
                } else if(args[0].equalsIgnoreCase("suspend")) {
                    if(player != null && !perms.has(player, "auction.admin")) {
                        messageManager.sendPlayerMessage("suspension-fail-permissions", playerName, (AuctionScope) null);
                        return true;
                    }
                    if(args.length > 1) {
                        // Suspend a player:
                        if(suspendedUsers.contains(args[1].toLowerCase())) {
                            messageManager.sendPlayerMessage("suspension-user-fail-already-suspended", playerName, (AuctionScope) null);
                            return true;
                        }

                        Player playerToSuspend = getServer().getPlayer(args[1]);

                        if(playerToSuspend == null || !playerToSuspend.isOnline()) {
                            messageManager.sendPlayerMessage("suspension-user-fail-is-offline", playerName, (AuctionScope) null);
                            return true;
                        }

                        if(perms.has(playerToSuspend, "auction.admin")) {
                            messageManager.sendPlayerMessage("suspension-user-fail-is-admin", playerName, (AuctionScope) null);
                            return true;
                        }

                        suspendedUsers.add(args[1].toLowerCase());
                        saveObject(suspendedUsers, "suspendedUsers.ser");
                        messageManager.sendPlayerMessage("suspension-user", playerToSuspend.getName(), (AuctionScope) null);
                        messageManager.sendPlayerMessage("suspension-user-success", playerName, (AuctionScope) null);

                        return true;
                    }
                    // Suspend globally:
                    suspendAllAuctions = true;

                    AuctionScope.cancelAllAuctions();

                    messageManager.broadcastAuctionScopeMessage("suspension-global", null);

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
                        messageManager.sendPlayerMessage("suspension-global", playerName, (AuctionScope) null);
                        return true;
                    }
                    if(player != null && suspendedUsers.contains(playerName.toLowerCase())) {
                        messageManager.sendPlayerMessage("suspension-user", playerName, (AuctionScope) null);
                        return true;
                    }

                    // Start new auction!
                    if(player == null) {
                        messageManager.sendPlayerMessage("auction-fail-console", playerName, (AuctionScope) null);
                        return true;
                    }
                    if(!AuctionConfig.getBoolean("allow-gamemode-creative", userScope) && player.getGameMode() == GameMode.CREATIVE) {
                        messageManager.sendPlayerMessage("auction-fail-gamemode-creative", playerName, (AuctionScope) null);
                        return true;
                    }

                    if(userScope == null) {
                        messageManager.sendPlayerMessage("auction-fail-no-scope", playerName, (AuctionScope) null);
                        return true;
                    }

                    if(!perms.has(player, "auction.start")) {
                        messageManager.sendPlayerMessage("auction-fail-permissions", playerName, (AuctionScope) null);
                        return true;
                    }

                    if(!AuctionConfig.getBoolean("allow-sealed-auctions", userScope) && !AuctionConfig.getBoolean("allow-unsealed-auctions", userScope)) {
                        messageManager.sendPlayerMessage("auction-fail-no-auctions-allowed", playerName, (AuctionScope) null);
                        return true;
                    }

                    if(player.getInventory().getItemInHand() == null || player.getInventory().getItemInHand().getAmount() == 0) {
                        messageManager.sendPlayerMessage("auction-fail-hand-is-empty", playerName, (AuctionScope) null);
                        return true;
                    }

                    if(cmd.getName().equalsIgnoreCase("sealedauction") || cmd.getName().equalsIgnoreCase("sauc")) {
                        if(AuctionConfig.getBoolean("allow-sealed-auctions", userScope)) {
                            userScope.queueAuction(new Auction(this, player, args, userScope, true, messageManager, player.getItemInHand().clone()));
                        } else {
                            messageManager.sendPlayerMessage("auction-fail-no-sealed-auctions", playerName, (AuctionScope) null);
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
                        messageManager.sendPlayerMessage("auction-fail-console", playerName, (AuctionScope) null);
                        return true;
                    }
                    if(!perms.has(player, "auction.start")) {
                        messageManager.sendPlayerMessage("auction-fail-permissions", playerName, (AuctionScope) null);
                        return true;
                    }

                    // The function returns null and sends error on failure.
                    String[] mergedArgs = Functions.mergeInputArgs(playerName, args, true);

                    if(mergedArgs != null) {
                        ObsidianAuctions.userSavedInputArgs.put(playerName, mergedArgs);
                        ObsidianAuctions.saveObject(ObsidianAuctions.userSavedInputArgs, "userSavedInputArgs.ser");
                        messageManager.sendPlayerMessage("prep-save-success", playerName, (AuctionScope) null);
                    }

                    return true;
                } else if(args[0].equalsIgnoreCase("cancel") || args[0].equalsIgnoreCase("c")) {
                    if(userScope == null) {
                        messageManager.sendPlayerMessage("auction-fail-no-scope", playerName, (AuctionScope) null);
                        return true;
                    }
                    if(userScope.getActiveAuction() == null && userScope.getAuctionQueueLength() == 0) {
                        messageManager.sendPlayerMessage("auction-fail-no-auction-exists", playerName, (AuctionScope) null);
                        return true;
                    }

                    List<Auction> auctionQueue = userScope.getAuctionQueue();
                    for(int i = 0; i < auctionQueue.size(); i++) {
                        if(auctionQueue.get(i).getOwner().equalsIgnoreCase(playerName)) {
                            auctionQueue.remove(i);
                            messageManager.sendPlayerMessage("auction-cancel-queued", playerName, (AuctionScope) null);
                            return true;
                        }
                    }

                    if(auction == null) {
                        messageManager.sendPlayerMessage("auction-fail-no-auction-exists", playerName, (AuctionScope) null);
                        return true;
                    }

                    if(player == null || player.getName().equalsIgnoreCase(auction.getOwner()) || perms.has(player, "auction.admin")) {
                        if(AuctionConfig.getInt("cancel-prevention-seconds", userScope) > auction.getRemainingTime() || AuctionConfig.getDouble("cancel-prevention-percent", userScope) > (double) auction.getRemainingTime() / (double) auction.getTotalTime() * 100D) {
                            messageManager.sendPlayerMessage("auction-fail-cancel-prevention", playerName, (AuctionScope) null);
                        } else {
                            auction.cancel();
                        }
                    } else {
                        messageManager.sendPlayerMessage("auction-fail-not-owner-cancel", playerName, (AuctionScope) null);
                    }
                    return true;
                } else if(args[0].equalsIgnoreCase("confiscate") || args[0].equalsIgnoreCase("impound")) {
                    if(auction == null) {
                        messageManager.sendPlayerMessage("auction-fail-no-auction-exists", playerName, (AuctionScope) null);
                        return true;
                    }

                    if(player == null) {
                        messageManager.sendPlayerMessage("confiscate-fail-console", playerName, (AuctionScope) null);
                        return true;
                    }
                    if(!perms.has(player, "auction.admin")) {
                        messageManager.sendPlayerMessage("confiscate-fail-permissions", playerName, (AuctionScope) null);
                        return true;
                    }
                    if(playerName.equalsIgnoreCase(auction.getOwner())) {
                        messageManager.sendPlayerMessage("confiscate-fail-self", playerName, (AuctionScope) null);
                        return true;
                    }
                    auction.confiscate(player);
                    return true;
                } else if(args[0].equalsIgnoreCase("end") || args[0].equalsIgnoreCase("e")) {
                    if(auction == null) {
                        messageManager.sendPlayerMessage("auction-fail-no-auction-exists", playerName, (AuctionScope) null);
                        return true;
                    }
                    if(!AuctionConfig.getBoolean("allow-early-end", userScope)) {
                        messageManager.sendPlayerMessage("auction-fail-no-early-end", playerName, (AuctionScope) null);
                        return true;
                    }
                    if(player.getName().equalsIgnoreCase(auction.getOwner())) {
                        auction.end();
                    } else {
                        messageManager.sendPlayerMessage("auction-fail-not-owner-end", playerName, (AuctionScope) null);
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
                        messageManager.sendPlayerMessage("auction-disabled", playerName, (AuctionScope) null);
                        getVoluntarilyDisabledUsers().add(playerName);
                        saveObject(getVoluntarilyDisabledUsers(), "voluntarilyDisabledUsers.ser");
                    }
                    return true;
                } else if(args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("i")) {
                    if(auction == null) {
                        messageManager.sendPlayerMessage("auction-info-no-auction", playerName, (AuctionScope) null);
                        return true;
                    }
                    auction.info(sender, false);
                    return true;
                } else if(args[0].equalsIgnoreCase("queue") || args[0].equalsIgnoreCase("q")) {
                    List<Auction> auctionQueue = userScope.getAuctionQueue();
                    if(auctionQueue.isEmpty()) {
                        messageManager.sendPlayerMessage("auction-queue-status-not-in-queue", playerName, (AuctionScope) null);
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
            messageManager.sendPlayerMessage("auction-help", playerName, (AuctionScope) null);
            return true;
        } else if(cmd.getName().equalsIgnoreCase("bid")) {
            if(suspendAllAuctions) {
                messageManager.sendPlayerMessage("suspension-global", playerName, (AuctionScope) null);
                return true;
            } else if(player != null && suspendedUsers.contains(playerName.toLowerCase())) {
                messageManager.sendPlayerMessage("suspension-user", playerName, (AuctionScope) null);
                return true;
            } else if(player == null) {
                messageManager.sendPlayerMessage("bid-fail-console", playerName, (AuctionScope) null);
                return true;
            } else if(!AuctionConfig.getBoolean("allow-gamemode-creative", userScope) && player.getGameMode().equals(GameMode.CREATIVE)) {
                messageManager.sendPlayerMessage("bid-fail-gamemode-creative", playerName, (AuctionScope) null);
                return true;
            } else if(!perms.has(player, "auction.bid")) {
                messageManager.sendPlayerMessage("bid-fail-permissions", playerName, (AuctionScope) null);
                return true;
            } else if(auction == null) {
                messageManager.sendPlayerMessage("bid-fail-no-auction", playerName, (AuctionScope) null);
                return true;
            }
            auction.Bid(player, args);
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
    public static void log(String playerName, String message, AuctionScope auctionScope) {
        if(AuctionConfig.getBoolean("log-auctions", auctionScope)) {
            String scopeId = null;

            BufferedWriter out = null;
            try {
                if(auctionLog == null || !auctionLog.exists()) {
                    auctionLog.createNewFile();
                    auctionLog.setWritable(true);
                }

                out = new BufferedWriter(new FileWriter(auctionLog.getAbsolutePath(), true));

                if(auctionScope == null) {
                    scopeId = "NOSCOPE";
                } else {
                    scopeId = auctionScope.getScopeId();
                }

                out.append((new Date()).toString() + " (" + playerName + ", " + scopeId + "): " + ChatColor.stripColor(message) + "\n");
                out.close();

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
    public static Auction getPlayerAuction(String playerName) {
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
    public static Auction getPlayerAuction(Player player) {
        if(player == null) {
            return null;
        }
        AuctionScope auctionScope = AuctionScope.getPlayerScope(player);
        if(auctionScope == null) {
            return null;
        }
        return auctionScope.getActiveAuction();
    }

    public static List<String> getVoluntarilyDisabledUsers() {
        return voluntarilyDisabledUsers;
    }

    public static Map<String, String> getPlayerScopeCache() {
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

    public static MessageManager getMessageManager() {
        return messageManager;
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
}