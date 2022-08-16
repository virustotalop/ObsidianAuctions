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

import com.clubobsidian.wrappy.ConfigurationSection;
import com.gmail.virustotalop.obsidianauctions.auction.AuctionScope;
import com.gmail.virustotalop.obsidianauctions.util.Functions;
import org.jetbrains.annotations.ApiStatus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Class for handling configuration and language values for floAuction using the AuctionScope preference.
 *
 * @author Joshua "flobi" Hatfield
 */
@ApiStatus.Internal
public class AuctionConfig {
    /**
     * Gets a double from the config converted to floAuction's proprietary "safe money."
     *
     * @param key         the location in the config of the value
     * @param auctionScope the preferred AuctionScope for retrieval
     * @return "safe money" from config
     */
    public static long getSafeMoneyFromDouble(Key key, AuctionScope auctionScope) {
        return Functions.getSafeMoney(getDouble(key, auctionScope));
    }

    /**
     * Gets a double value from the config.
     *
     * @param key         the location in the config of the value
     * @param auctionScope the preferred AuctionScope for retrieval
     * @return double from the config
     */
    public static double getDouble(Key key, AuctionScope auctionScope) {
        String path = key.toString();
        Double result = null;
        if (auctionScope != null && auctionScope.getConfig() != null && auctionScope.getConfig().hasKey(path)) {
            result = auctionScope.getConfig().getDouble(path);
        }

        if (result == null) {
            result = ObsidianAuctions.config.getDouble(path);
        }
        return result;
    }

    /**
     * Gets an integer value from the config.
     *
     * @param key         the location in the config of the value
     * @param auctionScope the preferred AuctionScope for retrieval
     * @return integer from the config
     */
    public static int getInt(Key key, AuctionScope auctionScope) {
        String path = key.toString();
        Integer result = null;
        if (auctionScope != null && auctionScope.getConfig() != null && auctionScope.getConfig().hasKey(path)) {
            result = auctionScope.getConfig().getInteger(path);
        }

        if (result == null) {
            result = ObsidianAuctions.config.getInteger(path);
        }
        return result;
    }

    /**
     * Gets a boolean value from the config.
     *
     * @param key         the location in the config of the value
     * @param auctionScope the preferred AuctionScope for retrieval
     * @return boolean from the config
     */
    public static boolean getBoolean(Key key, AuctionScope auctionScope) {
        Boolean result = null;
        String path = key.toString();
        if (auctionScope != null && auctionScope.getConfig() != null && auctionScope.getConfig().hasKey(path)) {
            result = auctionScope.getConfig().getBoolean(path);
        }

        if (result == null) {
            result = ObsidianAuctions.config.getBoolean(path);
        }
        return result;
    }

    /**
     * Gets a string list from the config.
     *
     * @param key         the location in the config of the value
     * @param auctionScope the preferred AuctionScope for retrieval
     * @return string list from the config
     */
    public static List<String> getStringList(Key key, AuctionScope auctionScope) {
        String path = key.toString();
        List<String> result = null;
        if (auctionScope != null && auctionScope.getConfig() != null && auctionScope.getConfig().hasKey(path)) {
            result = auctionScope.getConfig().getStringList(path);
        }

        if (result == null) {
            result = ObsidianAuctions.config.getStringList(path);
        }
        return result;
    }

    /**
     * Gets a string value from the config.
     *
     * @param key         the location in the config of the value
     * @param auctionScope the preferred AuctionScope for retrieval
     * @return string from the config
     */
    public static String getString(Key key, AuctionScope auctionScope) {
        String path = key.toString();
        String result = null;
        if (auctionScope != null && auctionScope.getConfig() != null && auctionScope.getConfig().hasKey(path)) {
            result = auctionScope.getConfig().getString(path);
        }

        if (result == null) {
            result = ObsidianAuctions.config.getString(path);
        }
        return result;
    }

    /**
     * Gets a uuid value from the config.
     *
     * @param key         the location in the config of the value
     * @param auctionScope the preferred AuctionScope for retrieval
     * @return uuid from the config
     */
    public static UUID getUUID(Key key, AuctionScope auctionScope) {
        String path = key.toString();
        UUID result = null;
        if (auctionScope != null && auctionScope.getConfig() != null && auctionScope.getConfig().hasKey(path)) {
            result = auctionScope.getConfig().getUUID(path);
        }
        if (result == null) {
            result = ObsidianAuctions.config.getUUID(path);
        }
        return result;
    }

    /**
     * Gets a string to string map from the config.
     *
     * @param key         the location in the config of the value
     * @param auctionScope the preferred AuctionScope for retrieval
     * @return string to string map from the config
     */
    public static Map<String, String> getStringStringMap(Key key, AuctionScope auctionScope) {
        String path = key.toString();
        Map<String, String> result = new HashMap<>();
        ConfigurationSection section = null;
        if (auctionScope != null && auctionScope.getConfig() != null && auctionScope.getConfig().hasKey(path)) {
            section = auctionScope.getConfig().getConfigurationSection(path);
        }

        if (section == null) {
            section = ObsidianAuctions.config.getConfigurationSection(path);
        }

        if (section != null) {
            for (String itemCode : section.getKeys()) {
                result.put(itemCode, section.getString(itemCode));
            }
        }
        return result;
    }

    /**
     * Gets a string from the language file.
     *
     * @param path         the location in the config of the value
     * @param auctionScope the preferred AuctionScope for retrieval
     * @return string from language file
     */
    public static String getLanguageString(Key key, AuctionScope auctionScope) {
        String path = key.toString();
        String result = null;
        if (auctionScope != null && auctionScope.getTextConfig() != null && auctionScope.getTextConfig().hasKey(path)) {
            result = auctionScope.getTextConfig().getString(path);
        }

        if (result == null) {
            result = ObsidianAuctions.textConfig.getString(path);
        }
        return result;
    }

    /**
     * Gets a string list from the language file.
     *
     * @param key         the location in the config of the value
     * @param auctionScope the preferred AuctionScope for retrieval
     * @return string list from language file
     */
    public static List<String> getLanguageStringList(Key key, AuctionScope auctionScope) {
        String path = key.toString();
        List<String> result = null;
        if (auctionScope != null && auctionScope.getTextConfig() != null && auctionScope.getTextConfig().hasKey(path)) {
            result = auctionScope.getTextConfig().getStringList(path);
        }

        if (result == null) {
            result = ObsidianAuctions.textConfig.getStringList(path);
        }
        return result;
    }
}