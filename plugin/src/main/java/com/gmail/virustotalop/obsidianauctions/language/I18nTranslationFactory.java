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

package com.gmail.virustotalop.obsidianauctions.language;

import com.clubobsidian.wrappy.Configuration;
import com.gmail.virustotalop.obsidianauctions.ObsidianAuctions;
import com.gmail.virustotalop.obsidianauctions.inject.annotation.I18nItemConfig;
import com.gmail.virustotalop.obsidianauctions.util.EnumUtil;
import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.NBTCompound;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.ApiStatus;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

@ApiStatus.Internal
public class I18nTranslationFactory implements TranslationFactory {

    private final Map<Material, Collection<LanguageItem>> items;

    @Inject
    private I18nTranslationFactory(@I18nItemConfig Configuration config) {
        this.items = this.loadItems(config);
    }

    @Override
    public String getTranslation(ItemStack itemStack) {
        if (itemStack == null) {
            return null;
        }
        Material type = itemStack.getType();
        Collection<LanguageItem> items = this.items.get(type);
        if (items != null) {
            for (LanguageItem item : items) {
                if (item.matches(itemStack)) {
                    return item.getTranslation();
                }
            }
        }
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (!itemMeta.hasLocalizedName()) {
            return EnumUtil.formatName(type.name());
        }
        return itemMeta.getLocalizedName();
    }

    private Map<Material, Collection<LanguageItem>> loadItems(Configuration config) {
        Map<Material, Collection<LanguageItem>> map = new HashMap<>();
        for (String key : config.getKeys()) {
            String translation = config.getString(key);
            LanguageItem item = this.parseItem(key, translation);
            if (item != null) {
                Material material = item.getType();
                Collection<LanguageItem> items = map.computeIfAbsent(material, (col) -> new ArrayList<>());
                items.add(item);
            }
        }
        return map;
    }

    private LanguageItem parseItem(String key, String translation) {
        boolean hasSeparator = key.contains("<sep>");
        Material material;
        short durability = 0;
        NBTCompound compound = null;
        try {
            if (hasSeparator) {
                String[] split = key.split("<sep>");
                if (split.length != 2) {
                    ObsidianAuctions.get().getLogger().log(Level.SEVERE, "Invalid length for: " + key);
                    return null;
                }

                String first = split[0];
                String second = split[1];
                if (this.invalidMaterial(first)) {
                    return null;
                }
                material = Material.getMaterial(first);
                if (this.isShort(second)) {
                    durability = Short.parseShort(second);
                } else {
                    try {
                        compound = (NBTCompound) NBT.parseNBT(second);
                    } catch (Exception ex) {
                        ObsidianAuctions.get().getLogger().log(Level.SEVERE, "Invalid nbt: " + second);
                        ex.printStackTrace();
                    }
                }
            } else {
                if (this.invalidMaterial(key)) {
                    return null;
                }
                material = Material.valueOf(key);
            }
            if (material == null) {
                ObsidianAuctions.get().getLogger().log(Level.SEVERE, "No material found for: " + key);
                return null;
            }
            return new LanguageItem(material, durability, compound, translation);
        } catch (Exception ex) {
            ObsidianAuctions.get().getLogger().log(Level.SEVERE, "Invalid item: " + key);
            ex.printStackTrace();
            return null;
        }
    }

    private boolean invalidMaterial(String material) {
        return Material.getMaterial(material) == null;
    }

    private boolean isShort(String parse) {
        try {
            Short.parseShort(parse);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }
}