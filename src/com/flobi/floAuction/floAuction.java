package com.flobi.floauction;

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
import java.util.logging.Logger;

import me.virustotal.floauction.listeners.InventoryClickListener;
import me.virustotal.floauction.utility.CArrayList;
import me.virustotal.floauction.utility.MigrationUtil;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
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
import org.bukkit.util.FileUtil;

import com.flobi.floauction.utilities.Functions;

/**
 * A Bukkit based Minecraft plugin to facilitate auctions.
 * 
 * @author Joshua "flobi" Hatfield
 */
public class FloAuction extends JavaPlugin {
	private static final Logger log = Logger.getLogger("Minecraft");

	public static int decimalPlaces = 0;
	public static String decimalRegex = "^[0-9]{0,13}(\\.[0-9]{0,1})?$";
	public static boolean loadedDecimalFromVault = false;
	private static File auctionLog = null;
	private static boolean suspendAllAuctions = false;
	public static boolean isDamagedAllowed;
	public static List<AuctionParticipant> auctionParticipants = new ArrayList<AuctionParticipant>();
	public static Map<String, String[]> userSavedInputArgs = new HashMap<String, String[]>();

	// Config files info.
	public static FileConfiguration config = null;
	public static FileConfiguration textConfig = null;
	private static File dataFolder;
	private static int queueTimer;
	public static FloAuction plugin;
	
	private static int playerScopeCheckTimer;
	protected static Map<String, String> playerScopeCache = new HashMap<String, String>();
	
	private static ArrayList<AuctionLot> orphanLots = new ArrayList<AuctionLot>();
	private static ArrayList<String> voluntarilyDisabledUsers = new ArrayList<String>();
	private static ArrayList<String> suspendedUsers = new ArrayList<String>();
	
	private static MessageManager messageManager = new AuctionMessageManager();
	
	/*Added values
	 * 
	 */
	public static String guiQueueName;
	public static List<String> itemBlacklist;
	public static boolean itemBlackListEnabled;
	
	public HashMap<String,String> names = new HashMap<String,String>();
	
	/**
	 * Used by AuctinLot to store auction lots which could not be given to players because they were offline.
	 * 
	 * @param auctionLot AuctionLot to save.
	 */
	public static void saveOrphanLot(AuctionLot auctionLot) 
	{
		FloAuction.orphanLots.add(auctionLot);
		saveObject(FloAuction.orphanLots, "orphanLots.ser");		
	}
	
	/**
	 * Saves an object to a file.
	 * 
	 * @param object object to save
	 * @param filename name of file
	 */
	private static void saveObject(Object object, String filename) 
	{
    	File saveFile = new File(dataFolder, filename);
    	try 
    	{
    		if (saveFile.exists()) //file exists, delete it
    		{
    			saveFile.delete();
    		}
    		FileOutputStream file = new FileOutputStream(saveFile.getAbsolutePath());
			OutputStream buffer = new BufferedOutputStream(file);
			ObjectOutput output = new ObjectOutputStream(buffer);
			try 
			{
				output.writeObject(object);
			}
			finally 
			{
				output.close();
				buffer.close(); //make sure these are closed
				file.close(); //make sure these are closed
			}
  	    }  
  	    catch(IOException ex){
    		return;
  	    }
	}
	
	/**
	 * Load a String array from a file.
	 * 
	 * @param filename where the file is
	 * @return the resulting string array
	 */
	@SuppressWarnings({ "unchecked", "finally" })
	private static ArrayList<String> loadArrayListString(String filename) 
	{
    	File saveFile = new File(dataFolder, filename);
    	ArrayList<String> importedObjects = new ArrayList<String>();
    	try 
    	{
			InputStream file = new FileInputStream(saveFile.getAbsolutePath());
			InputStream buffer = new BufferedInputStream(file);
			ObjectInput input = new ObjectInputStream (buffer);
			importedObjects = (ArrayList<String>) input.readObject();
			input.close();
			buffer.close(); //make sure these are closed
			file.close(); //make sure these are closed
  	    }  
		finally 
		{
  	    	return importedObjects;
		}
	}
	
	/**
	 * Load a String String map from a file.
	 * 
	 * @param filename where the file is
	 * @return the resulting string string map
	 */
	@SuppressWarnings({ "unchecked", "finally" })
	private static Map<String, String[]> loadMapStringStringArray(String filename) 
	{
    	File saveFile = new File(dataFolder, filename);
    	Map<String, String[]> importedObjects = new HashMap<String, String[]>();
    	try 
    	{
			InputStream file = new FileInputStream(saveFile.getAbsolutePath());
			InputStream buffer = new BufferedInputStream(file);
			ObjectInput input = new ObjectInputStream (buffer);
			importedObjects = (Map<String, String[]>) input.readObject();
			input.close();
			buffer.close();//make sure these are closed
			file.close();//make sure these are closed
  	    }  
		finally 
		{
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
	private static ArrayList<AuctionLot> loadArrayListAuctionLot(String filename) 
	{
    	File saveFile = new File(dataFolder, filename);
    	ArrayList<AuctionLot> importedObjects = new ArrayList<AuctionLot>();
    	try 
    	{
			InputStream file = new FileInputStream(saveFile.getAbsolutePath());
			InputStream buffer = new BufferedInputStream(file);
			ObjectInput input = new ObjectInputStream (buffer);
			importedObjects = (ArrayList<AuctionLot>) input.readObject();
			input.close();
			buffer.close(); //make sure these are closed
			file.close();   //make sure these are closed
  	    } 
    	catch (IOException e) {} 
    	catch (ClassNotFoundException e) {}  
    	return importedObjects;
	}
	
	/**
	 * Attempts to give lost AuctionLots back to their intended destination.
	 * 
	 * @param player the player to check for missing items
	 */
	// Eliminate orphan lots (i.e. try to give the items to a player again).
	public static void killOrphan(Player player) 
	{
		if (orphanLots != null && orphanLots.size() > 0) 
		{
			Iterator<AuctionLot> iter = orphanLots.iterator();
			while (iter.hasNext()) 
			{
				AuctionLot lot = iter.next();
			    if (lot.getOwner().equalsIgnoreCase(player.getName())) 
			    {
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
    	
    	MigrationUtil.migrateOldData(this);
		dataFolder = getDataFolder();
		plugin = this;
    	auctionLog = new File(dataFolder, "auctions.log");
		
        loadConfig();
        
		if (Bukkit.getPluginManager().getPlugin("Vault") == null) 
		{
			logToBukkit("plugin-disabled-no-vault", Level.SEVERE);
			Bukkit.getPluginManager().disablePlugin(this);
            return;
		}

		this.setupEconomy();
		this.setupPermissions();

		if (econ == null) 
		{
			logToBukkit("plugin-disabled-no-economy", Level.SEVERE);
			Bukkit.getPluginManager().disablePlugin(this);
            return;
		}
        
		ArenaManager.loadArenaListeners(this);
		
		//Load in inventory click listener
		Bukkit.getPluginManager().registerEvents(new InventoryClickListener(),this);
		
		Bukkit.getPluginManager().registerEvents(new Listener() 
		{
            @EventHandler
            public void playerJoin(PlayerJoinEvent event) 
            {
            	Player player = event.getPlayer();
        	    FloAuction.killOrphan(player);
        	    AuctionScope.sendWelcomeMessage(player, true);
            }
            @EventHandler
            public void onPlayerChangedWorld(PlayerChangedWorldEvent event)
            {
            	// Hopefully the teleport and portal things I just added will make this obsolete, but I figure I'll keep it just to make sure.
        		AuctionParticipant.forceLocation(event.getPlayer().getName(), null);
            }
            @EventHandler
            public void onPlayerChangedGameMode(PlayerGameModeChangeEvent event)
            {
            	if (event.isCancelled()) return;
            	Player player = event.getPlayer();
            	String playerName = player.getName();
            	AuctionScope playerScope = AuctionScope.getPlayerScope(player);
            	Auction playerAuction = getPlayerAuction(player);
            	if (AuctionConfig.getBoolean("allow-gamemode-change", playerScope) || playerAuction == null) return;
            	
            	if (AuctionParticipant.isParticipating(playerName)) 
            	{
                	event.setCancelled(true);
                	messageManager.sendPlayerMessage(new CArrayList<String>("gamemodechange-fail-participating"), playerName, (AuctionScope) null);
            	}
            }
            @EventHandler(priority = EventPriority.LOWEST)
            public void onPlayerPreprocessCommand(PlayerCommandPreprocessEvent event){

            	if (event.isCancelled()) return;
            	Player player = event.getPlayer();
            	if (player == null) return;
            	String playerName = player.getName();
            	String message = event.getMessage();
            	if (message == null || message.isEmpty()) return;

            	AuctionScope playerScope = AuctionScope.getPlayerScope(player);
            	
            	// Check inscope disabled commands, doesn't matter if participating:
            	List<String> disabledCommands = AuctionConfig.getStringList("disabled-commands-inscope", playerScope);
        		for (int i = 0; i < disabledCommands.size(); i++) 
        		{
        			String disabledCommand = disabledCommands.get(i);
        			if (disabledCommand.isEmpty()) continue;
        			if (message.toLowerCase().startsWith(disabledCommand.toLowerCase())) {
    	            	event.setCancelled(true);
    	            	messageManager.sendPlayerMessage(new CArrayList<String>("disabled-command-inscope"), playerName, (AuctionScope) null);
        				return;
        			}
        		}
            	
            	// Check participating disabled commands
            	if (playerScope == null) return;
            	if (!AuctionParticipant.isParticipating(player.getName())) return;

            	disabledCommands = AuctionConfig.getStringList("disabled-commands-participating", playerScope);
        		for (int i = 0; i < disabledCommands.size(); i++) 
        		{
        			String disabledCommand = disabledCommands.get(i);
        			if (disabledCommand.isEmpty()) continue;
        			if (message.toLowerCase().startsWith(disabledCommand.toLowerCase())) 
        			{
    	            	event.setCancelled(true);
    	            	messageManager.sendPlayerMessage(new CArrayList<String>("disabled-command-participating"), playerName, (AuctionScope) null);
        				return;
        			}
        		}
            }
        	@EventHandler()
        	public void onPlayerMove(PlayerMoveEvent event) 
        	{
        		if (event.isCancelled()) return;
        		AuctionParticipant.forceLocation(event.getPlayer().getName(), event.getTo());
        	}
        	@EventHandler()
        	public void onPlayerTeleport(PlayerTeleportEvent event) 
        	{
        		if (event.isCancelled()) return;
        		if (!AuctionParticipant.checkTeleportLocation(event.getPlayer().getName(), event.getTo())) event.setCancelled(true);
        	}
        	@EventHandler()
        	public void onPlayerPortalEvent(PlayerPortalEvent event) 
        	{
        		if (event.isCancelled()) return;
        		if (!AuctionParticipant.checkTeleportLocation(event.getPlayer().getName(), event.getTo())) event.setCancelled(true);
        	}
        }, this);
		
		BukkitScheduler bukkitScheduler = getServer().getScheduler();
		if (queueTimer > 0) 
		{
			bukkitScheduler.cancelTask(queueTimer);
		}
		queueTimer = bukkitScheduler.scheduleSyncRepeatingTask(this, new Runnable() 
		{
		    public void run() 
		    {
		    	AuctionScope.checkAuctionQueue();
		    }
		}, 20L, 20L);
		
		long playerScopeCheckInterval = config.getLong("auctionscope-change-check-interval");
		if (playerScopeCheckTimer > 0) bukkitScheduler.cancelTask(playerScopeCheckTimer);
		
		if (playerScopeCheckInterval > 0) 
		{
			playerScopeCheckTimer = bukkitScheduler.scheduleSyncRepeatingTask(this, new Runnable() 
			{
			    public void run() 
			    {
			    	AuctionScope.sendFairwellMessages();
			    	AuctionScope.sendWelcomeMessages();
			    }
			}, playerScopeCheckInterval, playerScopeCheckInterval);
		}
		
		orphanLots = loadArrayListAuctionLot("orphanLots.ser");
		FloAuction.voluntarilyDisabledUsers = loadArrayListString("voluntarilyDisabledUsers.ser");
		suspendedUsers = loadArrayListString("suspendedUsers.ser");
		userSavedInputArgs = loadMapStringStringArray("userSavedInputArgs.ser");

        messageManager.sendPlayerMessage(new CArrayList<String>("plugin-enabled"), null, (AuctionScope) null);
		
	}
    /**
	 * Loads config.yml and language.yml configuration files.
	 */
    private static void loadConfig() 
    {
		File configFile = new File(dataFolder, "config.yml");
    	InputStream defConfigStream = plugin.getResource("config.yml");;
    	File textConfigFile = new File(dataFolder, "language.yml");
    	InputStream defTextConfigStream = plugin.getResource("language.yml");
    	File nameConfigFile = new File(dataFolder, "names.yml");
    	
    	if(!nameConfigFile.exists())
    	{
    		FloAuction.plugin.saveResource("names.yml", false);
    	}
    		
    		
    	YamlConfiguration defConfig = null;
    	YamlConfiguration defTextConfig = null;
		YamlConfiguration nameConfig = null;
		
		config = null;
	    config = YamlConfiguration.loadConfiguration(configFile);
	    nameConfig = YamlConfiguration.loadConfiguration(nameConfigFile);
	    
	    if(config.get("queue-gui-name") == null)
	    {
	    	config.set("queue-gui-name", "&9ObsidianAuction Queue");
	    	try 
	    	{
				config.save(new File(FloAuction.dataFolder.getPath(),"config.yml"));
			} 
	    	catch (IOException e) 
	    	{
				e.printStackTrace();
			}
	    }
	    
	    if(config.get("name-blacklist") == null){
	    	List<String> blackListDefault = FloAuction.plugin.getConfig().getDefaults().getStringList("name-blacklist");
	    	config.set("name-blacklist", blackListDefault);
	    	try {
				config.save(new File(FloAuction.dataFolder.getPath(),"config.yml"));
			} catch (IOException e) {
				e.printStackTrace();
			}
	    }
	    
	    if(config.get("blacklist-enabled") == null) {
	    	config.set("blacklist-enabled", false);
	    	try {
				config.save(new File(FloAuction.dataFolder.getPath(),"config.yml"));
			} catch (IOException e) {
				e.printStackTrace();
			}
	    }
	    
	    
	    //set whether or not to allow mob spawners in the config, default is no
	    if(config.get("allow-mobspawners") == null)
	    {
	    	config.set("allow-mobspawners", true);
	    	try 
	    	{
				config.save(new File(FloAuction.dataFolder.getPath(),"config.yml"));
			} 
	    	catch (IOException e) 
	    	{
				e.printStackTrace();
			}
	    }
	    
	    if(config.get("renamed-items-override") == null) 
	    {
	    	config.set("renamed-items-override", false);
	    	try 
	    	{
				config.save(new File(FloAuction.dataFolder.getPath(),"config.yml"));
			} 
	    	catch (IOException e) 
	    	{
				e.printStackTrace();
			}
	    }
	    
	    
	    // Look for defaults in the jar
	    if (defConfigStream != null) 
	    {
	        defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
	        defConfigStream = null;
	    }
	    if (defConfig != null) 
	    {
	    	config.setDefaults(defConfig);
	    }
	    
	    textConfig = null;
	    
	    // Check to see if this needs converstion from floAuction version 2:
	    // suppress-auction-start-info was added in 2.6 and removed in 3.0.
	    if (config.contains("suppress-auction-start-info")) 
	    {
	    	// I want to save a copy of the config and language files for them.
	    	FileUtil.copy(configFile, new File(dataFolder, "config.v2-backup.yml"));
	    	FileUtil.copy(textConfigFile, new File(dataFolder, "language.v2-backup.yml"));
	    	
	    	// Late version 2's also had an auction house.  If it has this, it needs to be converted.
	    	String houseWorld = config.getString("auctionhouse-world");
	    	if (houseWorld != null && !houseWorld.isEmpty()) 
	    	{
	    		YamlConfiguration house = new YamlConfiguration();
	    		house.set("name", "Auction House");
	    		house.set("type", "house");
	    		house.set("house-world", houseWorld);
	    		house.set("house-min-x", config.get("auctionhouse-min-x"));
	    		house.set("house-min-y", config.get("auctionhouse-min-y"));
	    		house.set("house-min-z", config.get("auctionhouse-min-z"));
	    		house.set("house-max-x", config.get("auctionhouse-max-x"));
	    		house.set("house-max-y", config.get("auctionhouse-max-y"));
	    		house.set("house-max-z", config.get("auctionhouse-max-z"));
	    		YamlConfiguration scopes = new YamlConfiguration();
	    		scopes.set("house", house);
	    		config.set("auction-scopes", scopes);
	    	}
	    	config.set("disabled-commands-participating", config.get("disabled-commands"));
	    	// The unused rows will be removed through the cleaning process.
	    	// The entire language file needs to be purged though.
		    textConfig = new YamlConfiguration();
	    } 
	    else 
	    {
		    textConfig = YamlConfiguration.loadConfiguration(textConfigFile);
	    }

	    // Look for defaults in the jar
	    if (defTextConfigStream != null) 
	    {
	        defTextConfig = YamlConfiguration.loadConfiguration(defTextConfigStream);
	        defTextConfigStream = null;
	    }
	    if (defTextConfig != null) 
	    {
	        textConfig.setDefaults(defTextConfig);
	    }
	    
		// Clean up the configuration of any unused values.
		FileConfiguration cleanConfig = new YamlConfiguration();
		Map<String, Object> configValues = config.getDefaults().getValues(false);
		for (Map.Entry<String, Object> configEntry : configValues.entrySet()) {
			cleanConfig.set(configEntry.getKey(), config.get(configEntry.getKey()));
		}
		config = cleanConfig;

    	try 
    	{
    		config.save(configFile);
		} 
    	catch(IOException ex) 
		{
			log.severe("Cannot save config.yml");
		}
	    
	    // Another typo fix from 3.0.0
	    if (textConfig.contains("plogin-reload-fail-permissions")) 
	    {
	    	textConfig.set("plugin-reload-fail-permissions", textConfig.get("plogin-reload-fail-permissions"));
	    }
	    
		FileConfiguration cleanTextConfig = new YamlConfiguration();
		Map<String, Object> textConfigValues = textConfig.getDefaults().getValues(false);
		for (Map.Entry<String, Object> textConfigEntry : textConfigValues.entrySet()) 
		{
			cleanTextConfig.set(textConfigEntry.getKey(), textConfig.get(textConfigEntry.getKey()));
		}
		textConfig = cleanTextConfig;
		
		// Here's an oppsie fix for a typo in 3.0.0.
		if (textConfig.getString("bid-fail-under-starting-bid") != null && textConfig.getString("bid-fail-under-starting-bid").equals("&6The bidding must start at %auction-pre-tax%.")) //%A8
		{
			textConfig.set("bid-fail-under-starting-bid", "&6The bidding must start at %auction-bid-starting%."); //%A4
		}

		try 
		{
    		textConfig.save(textConfigFile);
		} catch(IOException ex) 
		{
			log.severe("Cannot save language.yml");
		}

	    
	    // Build auction scopes.
	    AuctionScope.setupScopeList(config.getConfigurationSection("auction-scopes"), dataFolder);
	    
	    //Gui queue inventory name
	    FloAuction.guiQueueName = ChatColor.translateAlternateColorCodes('&', config.getString("queue-gui-name"));
	    FloAuction.itemBlacklist = config.getStringList("name-blacklist");
	    FloAuction.itemBlackListEnabled = config.getBoolean("blacklist-enabled");
	    
	    //Get name from id
	    for(String string : nameConfig.getKeys(false))
	    {
	    	FloAuction.plugin.names.put(string, nameConfig.getString(string));
	    }
	    
	    //Setup additional floAuction values
	    FloAuction.isDamagedAllowed = defConfig.getBoolean("allow-damaged-items");
	    
	    //make values null at the end
		defConfig = null;
	    configFile = null;
        defTextConfig = null;
	    textConfigFile = null;
	    MigrationUtil.mapOldStrings(); //Used to map the old config strings to new strings, check mappings.yml
	    
    }
    
    /**
     * Called by Bukkit when disabling.  Cancels all auctions and clears data.
     */
    @Override
	public void onDisable() {
		AuctionScope.cancelAllAuctions();
		this.getServer().getScheduler().cancelTask(queueTimer);
		FloAuction.plugin = null;
		this.logToBukkit("plugin-disabled", Level.INFO);
		FloAuction.auctionLog = null;
	}
	
    // Overrides onCommand from Plugin
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

    	// Make sure the decimalPlaces loaded correctly.
    	// Sometimes the econ loads after floAuction.
	    if (!loadedDecimalFromVault && econ.isEnabled()) 
	    {
	    	loadedDecimalFromVault = true;
			decimalPlaces = Math.max(econ.fractionalDigits(), 0);
			config.set("decimal-places", decimalPlaces);
			if (decimalPlaces < 1) 
			{
				decimalRegex = "^[0-9]{1,13}$";
			} else if (decimalPlaces == 1) 
			{
				decimalRegex = "^[0-9]{0,13}(\\.[0-9])?$";
			} else 
			{
				decimalRegex = "^[0-9]{0,13}(\\.[0-9]{1," + decimalPlaces + "})?$";
			}
	    }
    	
    	Player player = null;
    	Auction auction = null;
		AuctionScope userScope = null;
		String playerName = null;

    	if (sender instanceof Player) 
    	{
    		player = (Player) sender;
			playerName = player.getName();
			userScope = AuctionScope.getPlayerScope(player);
			if (userScope != null) 
			{
				auction = userScope.getActiveAuction();
			}
    	}

		if (
				(cmd.getName().equalsIgnoreCase("auction") || cmd.getName().equalsIgnoreCase("auc")) &&
				args.length > 0 &&
				args[0].equalsIgnoreCase("on")
		) {
			int index = getVoluntarilyDisabledUsers().indexOf(playerName);
			if (index != -1) {
				getVoluntarilyDisabledUsers().remove(index);
			}
			messageManager.sendPlayerMessage(new CArrayList<String>("auction-enabled"), playerName, (AuctionScope) null);
			saveObject(getVoluntarilyDisabledUsers(), "voluntarilyDisabledUsers.ser");
			return true;
		}
     
    	if (getVoluntarilyDisabledUsers().contains(playerName)) 
    	{
    		getVoluntarilyDisabledUsers().remove(getVoluntarilyDisabledUsers().indexOf(playerName));
    		messageManager.sendPlayerMessage(new CArrayList<String>("auction-fail-disabled"), playerName, (AuctionScope) null);
			getVoluntarilyDisabledUsers().add(playerName);
			saveObject(getVoluntarilyDisabledUsers(), "voluntarilyDisabledUsers.ser");
			return true;
		}
    	
    	if (
    		cmd.getName().equalsIgnoreCase("auc") ||
    		cmd.getName().equalsIgnoreCase("auction") ||
    		cmd.getName().equalsIgnoreCase("sauc") ||
    		cmd.getName().equalsIgnoreCase("sealedauction")
    	) {
    		if (args.length > 0) 
    		{
				if (args[0].equalsIgnoreCase("reload")) 
				{
    				if (player != null && !perms.has(player, "auction.admin")) 
    				{
    					messageManager.sendPlayerMessage(new CArrayList<String>("plugin-reload-fail-permissions"), playerName, (AuctionScope) null);
    	    			return true;
    				}
    				else if (AuctionScope.areAuctionsRunning()) // Don't reload if any auctions are running.
    				{
    					messageManager.sendPlayerMessage(new CArrayList<String>("plugin-reload-fail-auctions-running"), playerName, (AuctionScope) null);
						return true;
    				}

    				loadConfig();
    				messageManager.sendPlayerMessage(new CArrayList<String>("plugin-reloaded"), playerName, (AuctionScope) null);
    				return true;
    			} 
				else if (args[0].equalsIgnoreCase("resume")) 
    			{
			    	if (args.length == 1) 
			    	{
						if (player != null && !perms.has(player, "auction.admin")) 
						{
							messageManager.sendPlayerMessage(new CArrayList<String>("unsuspension-fail-permissions"), playerName, (AuctionScope) null);
			    			return true;
						}
						// Resume globally:
						suspendAllAuctions = false;
						messageManager.broadcastAuctionScopeMessage(new CArrayList<String>("unsuspension-global"), (AuctionScope) null);
						return true;
    		    	}
    		    	
    				if (player != null && !perms.has(player, "auction.admin")) 
    				{
    					messageManager.sendPlayerMessage(new CArrayList<String>("unsuspension-fail-permissions"), playerName, (AuctionScope) null);
    	    			return true;
    				}

					if (!suspendedUsers.contains(args[1].toLowerCase())) 
					{
						messageManager.sendPlayerMessage(new CArrayList<String>("unsuspension-user-fail-not-suspended"), playerName, (AuctionScope) null);
		    			return true;
					}

					suspendedUsers.remove(args[1].toLowerCase());
					saveObject(suspendedUsers, "suspendedUsers.ser");
					messageManager.sendPlayerMessage(new CArrayList<String>("unsuspension-user"), args[1], (AuctionScope) null);
			    	messageManager.sendPlayerMessage(new CArrayList<String>("unsuspension-user-success"), playerName, (AuctionScope) null);
    				
    				return true;
    			} 
    			else if (args[0].equalsIgnoreCase("suspend")) {
    				if (player != null && !perms.has(player, "auction.admin")) 
    				{
    					messageManager.sendPlayerMessage(new CArrayList<String>("suspension-fail-permissions"), playerName, (AuctionScope) null);
    	    			return true;
    				}
    				if (args.length > 1) {
    					// Suspend a player:
    					if (suspendedUsers.contains(args[1].toLowerCase())) 
    					{
    						messageManager.sendPlayerMessage(new CArrayList<String>("suspension-user-fail-already-suspended"), playerName, (AuctionScope) null);
    		    			return true;
    					}
    					
    					Player playerToSuspend = getServer().getPlayer(args[1]);
    					
    					if (playerToSuspend == null || !playerToSuspend.isOnline()) 
    					{
    						messageManager.sendPlayerMessage(new CArrayList<String>("suspension-user-fail-is-offline"), playerName, (AuctionScope) null);
    		    			return true;
    					}
    					
    					if (perms.has(playerToSuspend, "auction.admin")) 
    					{
    						messageManager.sendPlayerMessage(new CArrayList<String>("suspension-user-fail-is-admin"), playerName, (AuctionScope) null);
    		    			return true;
    					}
    					
    					suspendedUsers.add(args[1].toLowerCase());
    					saveObject(suspendedUsers, "suspendedUsers.ser");
    					messageManager.sendPlayerMessage(new CArrayList<String>("suspension-user"), playerToSuspend.getName(), (AuctionScope) null);
    			    	messageManager.sendPlayerMessage(new CArrayList<String>("suspension-user-success"), playerName, (AuctionScope) null);
    					
    					return true;
    				}
    				// Suspend globally:
    				suspendAllAuctions = true;
    				
    				AuctionScope.cancelAllAuctions();

    		    	messageManager.broadcastAuctionScopeMessage(new CArrayList<String>("suspension-global"), null);

	    			return true;
    			} else if (
        				args[0].equalsIgnoreCase("start") || 
        				args[0].equalsIgnoreCase("s") ||
        				args[0].equalsIgnoreCase("this") ||
        				args[0].equalsIgnoreCase("hand") ||
        				args[0].equalsIgnoreCase("all") ||
        				args[0].matches("[0-9]+")
    			) {
    		    	if (suspendAllAuctions) {
    			    	messageManager.sendPlayerMessage(new CArrayList<String>("suspension-global"), playerName, (AuctionScope) null);
    		    		return true;
    		    	}
    		    	if (player != null && suspendedUsers.contains(playerName.toLowerCase())) {
    		    		messageManager.sendPlayerMessage(new CArrayList<String>("suspension-user"), playerName, (AuctionScope) null);
    					return true;
    		    	}

    				// Start new auction!
    	    		if (player == null) 
    	    		{
    	    			messageManager.sendPlayerMessage(new CArrayList<String>("auction-fail-console"), playerName, (AuctionScope) null);
    	    			return true;
    	    		}
    	    		if (!AuctionConfig.getBoolean("allow-gamemode-creative", userScope) && player.getGameMode() == GameMode.CREATIVE) {
    	    			messageManager.sendPlayerMessage(new CArrayList<String>("auction-fail-gamemode-creative"), playerName, (AuctionScope) null);
    	    			return true;
    	    		}
    	    		
    	    		if (userScope == null) 
    	    		{
    	    			messageManager.sendPlayerMessage(new CArrayList<String>("auction-fail-no-scope"), playerName, (AuctionScope) null);
    	    			return true;
    	    		}
    	    			
    				if (!perms.has(player, "auction.start")) 
    				{
    					messageManager.sendPlayerMessage(new CArrayList<String>("auction-fail-permissions"), playerName, (AuctionScope) null);
    	    			return true;
    				}
    				
    				if (!AuctionConfig.getBoolean("allow-sealed-auctions", userScope) && !AuctionConfig.getBoolean("allow-unsealed-auctions", userScope)) 
    				{
    					messageManager.sendPlayerMessage(new CArrayList<String>("auction-fail-no-auctions-allowed"), playerName, (AuctionScope) null);
    					return true;
    				}
    				
    				if(player.getInventory().getItemInHand() == null || player.getInventory().getItemInHand().getAmount() == 0) 
    				{
    					messageManager.sendPlayerMessage(new CArrayList<String>("auction-fail-hand-is-empty"), playerName, (AuctionScope) null);
    					return true;
    				}
    				
    				if (cmd.getName().equalsIgnoreCase("sealedauction") || cmd.getName().equalsIgnoreCase("sauc")) 
    				{
    					if (AuctionConfig.getBoolean("allow-sealed-auctions", userScope)) 
    					{
    						userScope.queueAuction(new Auction(this, player, args, userScope, true, messageManager, player.getItemInHand().clone()));
    					} else 
    					{
    						messageManager.sendPlayerMessage(new CArrayList<String>("auction-fail-no-sealed-auctions"), playerName, (AuctionScope) null);
    					}
    				} else 
    				{
    					if (AuctionConfig.getBoolean("allow-unsealed-auctions", userScope)) 
    					{
    						userScope.queueAuction(new Auction(this, player, args, userScope, false, messageManager, player.getItemInHand().clone()));
    					} else 
    					{
    						userScope.queueAuction(new Auction(this, player, args, userScope, true, messageManager, player.getItemInHand().clone()));
    					}
    				}

					return true;
    			} else if (args[0].equalsIgnoreCase("prep") || args[0].equalsIgnoreCase("p")) 
    			{
    				// Save a users individual starting default values.
    	    		if (player == null) 
    	    		{
    	    			messageManager.sendPlayerMessage(new CArrayList<String>("auction-fail-console"), playerName, (AuctionScope) null);
    	    			return true;
    	    		}
    				if (!perms.has(player, "auction.start")) 
    				{
    					messageManager.sendPlayerMessage(new CArrayList<String>("auction-fail-permissions"), playerName, (AuctionScope) null);
    	    			return true;
    				}
    				
    				// The function returns null and sends error on failure.
    				String[] mergedArgs = Functions.mergeInputArgs(playerName, args, true);
    				
    				if (mergedArgs != null) 
    				{
						FloAuction.userSavedInputArgs.put(playerName, mergedArgs);
						FloAuction.saveObject(FloAuction.userSavedInputArgs, "userSavedInputArgs.ser");
						messageManager.sendPlayerMessage(new CArrayList<String>("prep-save-success"), playerName, (AuctionScope) null);
    				}

					return true;
    			} 
    			else if (args[0].equalsIgnoreCase("cancel") || args[0].equalsIgnoreCase("c")) 
    			{
    	    		if (userScope == null) 
    	    		{
    	    			messageManager.sendPlayerMessage(new CArrayList<String>("auction-fail-no-scope"), playerName, (AuctionScope) null);
    	    			return true;
    	    		}
    				if (userScope.getActiveAuction() == null && userScope.getAuctionQueueLength() == 0) 
    				{
    					messageManager.sendPlayerMessage(new CArrayList<String>("auction-fail-no-auction-exists"), playerName, (AuctionScope) null);
    					return true;
    				}
    				
    				ArrayList<Auction> auctionQueue = userScope.getAuctionQueue();
    				for(int i = 0; i < auctionQueue.size(); i++)
    				{
    					if (auctionQueue.get(i).getOwner().equalsIgnoreCase(playerName)) 
    					{
    						auctionQueue.remove(i);
    						messageManager.sendPlayerMessage(new CArrayList<String>("auction-cancel-queued"), playerName, (AuctionScope) null);
    						return true;
    					}
    				}
    				
    				if (auction == null) 
    				{
    					messageManager.sendPlayerMessage(new CArrayList<String>("auction-fail-no-auction-exists"), playerName, (AuctionScope) null);
    					return true;
    				}
    				
					if (player == null || player.getName().equalsIgnoreCase(auction.getOwner()) || perms.has(player, "auction.admin")) 
					{
						if (AuctionConfig.getInt("cancel-prevention-seconds", userScope) > auction.getRemainingTime() || AuctionConfig.getDouble("cancel-prevention-percent", userScope) > (double)auction.getRemainingTime() / (double)auction.getTotalTime() * 100D) {
							messageManager.sendPlayerMessage(new CArrayList<String>("auction-fail-cancel-prevention"), playerName, (AuctionScope) null);
						} 
						else 
						{
	    					auction.cancel();
						}
					} else 
					{
						messageManager.sendPlayerMessage(new CArrayList<String>("auction-fail-not-owner-cancel"), playerName, (AuctionScope) null);
					}
    				return true;
    			} 
    			else if (args[0].equalsIgnoreCase("confiscate") || args[0].equalsIgnoreCase("impound")) 
    			{
    				if (auction == null) 
    				{
    					messageManager.sendPlayerMessage(new CArrayList<String>("auction-fail-no-auction-exists"), playerName, (AuctionScope) null);
    					return true;
    				}
    				
    				if (player == null) 
    				{
    					messageManager.sendPlayerMessage(new CArrayList<String>("confiscate-fail-console"), playerName, (AuctionScope) null);
    					return true;
    				}
					if (!perms.has(player, "auction.admin")) 
					{
						messageManager.sendPlayerMessage(new CArrayList<String>("confiscate-fail-permissions"), playerName, (AuctionScope) null);
    					return true;
					}
					if (playerName.equalsIgnoreCase(auction.getOwner())) 
					{
						messageManager.sendPlayerMessage(new CArrayList<String>("confiscate-fail-self"), playerName, (AuctionScope) null);
    					return true;
					}
					auction.confiscate(player);
    				return true;
    			} 
    			else if (args[0].equalsIgnoreCase("end") || args[0].equalsIgnoreCase("e")) 
    			{
    				if (auction == null) 
    				{
    					messageManager.sendPlayerMessage(new CArrayList<String>("auction-fail-no-auction-exists"), playerName, (AuctionScope) null);
        				return true;
    				}
    				if (!AuctionConfig.getBoolean("allow-early-end", userScope)) 
    				{
    					messageManager.sendPlayerMessage(new CArrayList<String>("auction-fail-no-early-end"), playerName, (AuctionScope) null);
        				return true;
    				}
					if (player.getName().equalsIgnoreCase(auction.getOwner())) 
					{
    					auction.end();
					} 
					else 
					{
						messageManager.sendPlayerMessage(new CArrayList<String>("auction-fail-not-owner-end"), playerName, (AuctionScope) null);
					}
    				return true;
    			} else if (
    					args[0].equalsIgnoreCase("stfu") ||
    					args[0].equalsIgnoreCase("ignore") ||
        				args[0].equalsIgnoreCase("quiet") ||
        				args[0].equalsIgnoreCase("off") ||
        				args[0].equalsIgnoreCase("silent") ||
        				args[0].equalsIgnoreCase("silence")
    			) {
    				if (getVoluntarilyDisabledUsers().indexOf(playerName) == -1) 
    				{
    					messageManager.sendPlayerMessage(new CArrayList<String>("auction-disabled"), playerName, (AuctionScope) null);
    					getVoluntarilyDisabledUsers().add(playerName);
    					saveObject(getVoluntarilyDisabledUsers(), "voluntarilyDisabledUsers.ser");
    				}
    				return true;
    			}
    			else if (args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("i")) 
    			{
    				if (auction == null) 
    				{
    					messageManager.sendPlayerMessage(new CArrayList<String>("auction-info-no-auction"), playerName, (AuctionScope) null);
    					return true;
    				}
					auction.info(sender, false);
    				return true;
    			} 
    			else if (args[0].equalsIgnoreCase("queue") || args[0].equalsIgnoreCase("q")) 
    			{
    				ArrayList<Auction> auctionQueue = userScope.getAuctionQueue();
    				if (auctionQueue.isEmpty()) 
    				{
    					messageManager.sendPlayerMessage(new CArrayList<String>("auction-queue-status-not-in-queue"), playerName, (AuctionScope) null);
    					return true;
    				}
    				Inventory inv = Bukkit.createInventory(null, 18, FloAuction.guiQueueName);
    				for(int i = 0; i < auctionQueue.size(); i++)
    				{
    					if(i == inv.getSize())
    						break;
    					inv.setItem(i, auctionQueue.get(i).getGuiItem());
    				}
    				player.openInventory(inv);
    				return true;
    			}
    		}
    		messageManager.sendPlayerMessage(new CArrayList<String>("auction-help"), playerName, (AuctionScope) null);
    		return true;
    	} 
    	else if (cmd.getName().equalsIgnoreCase("bid")) 
    	{
        	if (suspendAllAuctions) 
        	{
        		messageManager.sendPlayerMessage(new CArrayList<String>("suspension-global"), playerName, (AuctionScope) null);
        		return true;
        	}
        	else if (player != null && suspendedUsers.contains(playerName.toLowerCase())) 
        	{
        		messageManager.sendPlayerMessage(new CArrayList<String>("suspension-user"), playerName, (AuctionScope) null);
    			return true;
        	}
        	else if (player == null) 
    		{
    			messageManager.sendPlayerMessage(new CArrayList<String>("bid-fail-console"), playerName, (AuctionScope) null);
    			return true;
    		} 
        	else if (!AuctionConfig.getBoolean("allow-gamemode-creative", userScope) && player.getGameMode().equals(GameMode.CREATIVE)) 
    		{
    			messageManager.sendPlayerMessage(new CArrayList<String>("bid-fail-gamemode-creative"), playerName, (AuctionScope) null);
    			return true;
    		}
        	else if (!perms.has(player, "auction.bid")) 
			{
				messageManager.sendPlayerMessage(new CArrayList<String>("bid-fail-permissions"), playerName, (AuctionScope) null);
    			return true;
			}
        	else if (auction == null) 
    		{
    			messageManager.sendPlayerMessage(new CArrayList<String>("bid-fail-no-auction"), playerName, (AuctionScope) null);
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
     * @param sender who is initiating the logged event
     * @param message message to save
     */
    static void log(String playerName, String message, AuctionScope auctionScope) 
    {
    	if (AuctionConfig.getBoolean("log-auctions", auctionScope)) {
    		String scopeId = null;
    		
			BufferedWriter out = null;
			try 
			{
		    	if (auctionLog == null || !auctionLog.exists()) 
		    	{
					auctionLog.createNewFile();
					auctionLog.setWritable(true);
		    	}
		    	
				out = new BufferedWriter(new FileWriter(auctionLog.getAbsolutePath(), true));

				if (auctionScope == null) 
				{
					scopeId = "NOSCOPE";
				} 
				else 
				{
					scopeId = auctionScope.getScopeId();
				}
				
				out.append((new Date()).toString() + " (" + playerName + ", " + scopeId + "): " + ChatColor.stripColor(message) + "\n");
				out.close();

			} catch (IOException e) {
				
			}
    	}
	}

    /**
     * Setup Vault economy.
     * 
     * @return success level
     */
    private boolean setupEconomy() 
    {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) 
        {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) 
        {
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
    private boolean setupPermissions() 
    {
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
	public static Auction getPlayerAuction(String playerName) 
	{
		if (playerName == null) return null;
		return getPlayerAuction(Bukkit.getPlayer(playerName));
	}

	/**
	 * Gets the active auction instance from the scope where the player is.
	 * 
	 * @param player player in reference
	 * @return auction instance
	 */
	public static Auction getPlayerAuction(Player player) 
	{
		if (player == null) return null;
		AuctionScope auctionScope = AuctionScope.getPlayerScope(player);
		if (auctionScope == null) return null;
		return auctionScope.getActiveAuction();
	}

	public static ArrayList<String> getVoluntarilyDisabledUsers() 
	{
		return voluntarilyDisabledUsers;
	}
    
    /**
     * Prepares chat, prepending prefix and removing colors.
     * 
	 * @param message message to prepare
	 * @param auctionScope the scope of the destination
	 * @return prepared message
     */
    private static String chatPrepClean(String message, AuctionScope auctionScope) 
    {
    	message = AuctionConfig.getLanguageString("chat-prefix", auctionScope) + message;
    	message = ChatColor.translateAlternateColorCodes('&', message);
    	message = ChatColor.stripColor(message);
    	return message;
    }
    
    public static MessageManager getMessageManager() 
    {
    	return messageManager;
    }

    private void logToBukkit(String key, Level level) 
    {
    	List<String> messageList = AuctionConfig.getLanguageStringList(key, null);
    	
    	String originalMessage = null;
    	if (messageList == null || messageList.size() == 0) 
    	{
    		originalMessage = AuctionConfig.getLanguageString(key, null);
    		
    		if (originalMessage != null && originalMessage.length() != 0) 
    		{
        		messageList = Arrays.asList(originalMessage.split("(\r?\n|\r)"));
    		}
    	}
    	for (Iterator<String> i = messageList.iterator(); i.hasNext(); ) 
    	{
    		String messageListItem = i.next();
    		log.log(level, chatPrepClean(messageListItem, null));
    	}
    }
}