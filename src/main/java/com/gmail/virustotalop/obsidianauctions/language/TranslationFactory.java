package com.gmail.virustotalop.obsidianauctions.language;

import com.clubobsidian.wrappy.Configuration;
import com.gmail.virustotalop.obsidianauctions.ObsidianAuctions;
import com.gmail.virustotalop.obsidianauctions.inject.annotation.I18nItemConfig;
import com.gmail.virustotalop.obsidianauctions.nbt.NBTCompound;
import com.gmail.virustotalop.obsidianauctions.util.MaterialUtil;
import com.google.inject.Inject;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class TranslationFactory {

    private final Map<Material, Collection<LanguageItem>> items;

    @Inject
    private TranslationFactory(@I18nItemConfig Configuration config) {
        this.items = this.loadItems(config);
    }

    public String getTranslation(ItemStack itemStack) {
        if(itemStack == null) {
            return null;
        }
        Material type = itemStack.getType();
        Collection<LanguageItem> items = this.items.get(type);
        if(items != null) {
            for(LanguageItem item : items) {
                if(item.matches(itemStack)) {
                    return item.getTranslation();
                }
            }
        }
        ItemMeta itemMeta = itemStack.getItemMeta();
        if(!itemMeta.hasLocalizedName()) {
            return MaterialUtil.formatName(type.name());
        }
        return itemMeta.getLocalizedName();
    }

    private Map<Material, Collection<LanguageItem>> loadItems(Configuration config) {
        Map<Material, Collection<LanguageItem>> map = new HashMap<>();
        for(String key : config.getKeys()) {
            String translation = config.getString(key);
            boolean hasSeperator = key.contains("<sep>");
            Material material = null;
            short durability = 0;
            NBTCompound compound = null;
            try {
                if(hasSeperator) {
                    String[] split = key.split("<sep>");
                    if(split.length != 2) {
                        ObsidianAuctions.get().getLogger().log(Level.SEVERE, "Invalid length for: " + key);
                        continue;
                    }

                    String first = split[0];
                    String second = split[1];
                    if(!this.isMaterial(first)) {
                        continue; //Skip and ignore
                    }
                    material = Material.getMaterial(first);
                    if(this.isShort(second)) {
                        durability = Short.parseShort(second);
                    } else {
                        try {
                            compound = new NBTCompound(second);
                        } catch(Exception ex) {
                            ObsidianAuctions.get().getLogger().log(Level.SEVERE, "Invalid nbt: " + second);
                            ex.printStackTrace();
                        }
                    }
                } else {
                    if(!this.isMaterial(key)) {
                        continue; //Skip and ignore
                    }
                    material = Material.valueOf(key);
                }
                if(material == null) {
                    ObsidianAuctions.get().getLogger().log(Level.SEVERE, "No material found for: " + key);
                    continue;
                }
                Collection<LanguageItem> items = map.computeIfAbsent(material, (col) -> new ArrayList<>());
                items.add(new LanguageItem(material, durability, compound, translation));
            }
            catch(Exception ex) {
                ObsidianAuctions.get().getLogger().log(Level.SEVERE, "Invalid item: " + key);
                ex.printStackTrace();
            }
        }
        return map;
    }

    private boolean isMaterial(String material) {
        try {
            Material.valueOf(material);
            return true;
        } catch(IllegalArgumentException ex) {
            return false;
        }
    }

    private boolean isShort(String parse) {
        try {
            Short.parseShort(parse);
            return true;
        } catch(NumberFormatException ex) {
            return false;
        }
    }
}