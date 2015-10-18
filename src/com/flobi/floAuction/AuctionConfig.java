package com.flobi.floAuction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.configuration.ConfigurationSection;

import com.flobi.floAuction.utilities.Functions;

/**
 * Class for handling configuration and language values for floAuction using the AuctionScope preference.
 * 
 * @author Joshua "flobi" Hatfield
 */
public class AuctionConfig {
	/**
	 * Gets a double from the config converted to floAuction's proprietary "safe money."
	 * 
	 * @param path the location in the config of the value
	 * @param auctionScope the preferred AuctionScope for retrieval
	 * @return "safe money" from config
	 */
	public static long getSafeMoneyFromDouble(String path, AuctionScope auctionScope) {
		return Functions.getSafeMoney(getDouble(path, auctionScope));
	}
	
	/**
	 * Gets a double value from the config.
	 * 
	 * @param path the location in the config of the value
	 * @param auctionScope the preferred AuctionScope for retrieval
	 * @return double from the config
	 */
	public static double getDouble(String path, AuctionScope auctionScope) {
		Double result = null; 
		if (auctionScope != null && auctionScope.getConfig() != null && auctionScope.getConfig().contains(path)) {
			result = auctionScope.getConfig().getDouble(path);
		}
		if (result == null) result = floAuction.config.getDouble(path);
		return result;
	}
	
	/**
	 * Gets an integer value from the config.
	 * 
	 * @param path the location in the config of the value
	 * @param auctionScope the preferred AuctionScope for retrieval
	 * @return integer from the config
	 */
	public static int getInt(String path, AuctionScope auctionScope) {
		Integer result = null; 
		if (auctionScope != null && auctionScope.getConfig() != null && auctionScope.getConfig().contains(path)) {
			result = auctionScope.getConfig().getInt(path);
		}
		if (result == null) result = floAuction.config.getInt(path);
		return result;
	}
	
	/**
	 * Gets a boolean value from the config.
	 * 
	 * @param path the location in the config of the value
	 * @param auctionScope the preferred AuctionScope for retrieval
	 * @return boolean from the config
	 */
	public static boolean getBoolean(String path, AuctionScope auctionScope) {
		Boolean result = null; 
		if (auctionScope != null && auctionScope.getConfig() != null && auctionScope.getConfig().contains(path)) {
			result = auctionScope.getConfig().getBoolean(path);
		}
		if (result == null) result = floAuction.config.getBoolean(path);
		return result;
	}
	
	/**
	 * Gets a string list from the config.
	 * 
	 * @param path the location in the config of the value
	 * @param auctionScope the preferred AuctionScope for retrieval
	 * @return string list from the config
	 */
	public static List<String> getStringList(String path, AuctionScope auctionScope) {
		List<String> result = null; 
		if (auctionScope != null && auctionScope.getConfig() != null && auctionScope.getConfig().contains(path)) {
			result = auctionScope.getConfig().getStringList(path);
		}
		if (result == null) result = floAuction.config.getStringList(path);
		return result;
	}
	
	/**
	 * Gets a string value from the config.
	 * 
	 * @param path the location in the config of the value
	 * @param auctionScope the preferred AuctionScope for retrieval
	 * @return string from the config
	 */
	public static String getString(String path, AuctionScope auctionScope) {
		String result = null; 
		if (auctionScope != null && auctionScope.getConfig() != null && auctionScope.getConfig().contains(path)) {
			result = auctionScope.getConfig().getString(path);
		}
		if (result == null) result = floAuction.config.getString(path);
		return result;
	}
	
	/**
	 * Gets a string to string map from the config.
	 * 
	 * @param path the location in the config of the value
	 * @param auctionScope the preferred AuctionScope for retrieval
	 * @return string to string map from the config
	 */
	public static Map<String, String> getStringStringMap(String path, AuctionScope auctionScope) {
		Map<String, String> result = new HashMap<String, String>();

		ConfigurationSection section = null;
		if (auctionScope != null && auctionScope.getConfig() != null && auctionScope.getConfig().contains(path)) {
			section = auctionScope.getConfig().getConfigurationSection(path);
		}
		if (section == null) section = floAuction.config.getConfigurationSection(path);
		
		
		result = new HashMap<String, String>();
		if (section != null) {
			for (String itemCode : section.getKeys(false)) {
				result.put(itemCode, section.getString(itemCode));
			}
		}
		return result;
	}
	
	/**
	 * Gets a string from the language file.
	 * 
	 * @param path the location in the config of the value
	 * @param auctionScope the preferred AuctionScope for retrieval
	 * @return string from language file
	 */
	public static String getLanguageString(String path, AuctionScope auctionScope) {
		String result = null; 
		if (auctionScope != null && auctionScope.getTextConfig() != null && auctionScope.getTextConfig().contains(path)) {
			result = auctionScope.getTextConfig().getString(path);
		}
		if (result == null) result = floAuction.textConfig.getString(path);
		return result;
	}
	
	/**
	 * Gets a string list from the language file.
	 * 
	 * @param path the location in the config of the value
	 * @param auctionScope the preferred AuctionScope for retrieval
	 * @return string list from language file
	 */
	public static List<String> getLanguageStringList(String path, AuctionScope auctionScope) {
		List<String> result = null; 
		if (auctionScope != null && auctionScope.getTextConfig() != null && auctionScope.getTextConfig().contains(path)) {
			result = auctionScope.getTextConfig().getStringList(path);
		}
		if (result == null) result = floAuction.textConfig.getStringList(path);
		return result;
	}
}
