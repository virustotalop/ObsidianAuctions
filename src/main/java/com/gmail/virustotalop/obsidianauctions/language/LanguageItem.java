package com.gmail.virustotalop.obsidianauctions.language;

import com.gmail.virustotalop.obsidianauctions.nbt.NBTCompound;
import com.gmail.virustotalop.obsidianauctions.util.LegacyUtil;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class LanguageItem {

    private final Material type;
    private final short durability;
    private final NBTCompound compound;
    private final String translation;

    public LanguageItem(Material type, short durability, NBTCompound compound, String translation) {
        this.type = type;
        this.durability = durability;
        this.compound = compound;
        this.translation = translation;
    }

    public Material getType() {
        return this.type;
    }

    public String getTranslation() {
        return this.translation;
    }

    public boolean matches(ItemStack itemStack) {
        if(this.type != itemStack.getType()) {
            return false;
        } else if(this.durability != LegacyUtil.getDurability(itemStack)) {
            return false;
        }
        if(this.compound != null) {
            NBTCompound stackComp = new NBTCompound(itemStack);
            for(String key : this.compound.getKeys()) {
                Object tag = stackComp.get(key);
                if(tag == null) {
                    return false;
                }
                if(!tag.equals(this.compound.get(key))) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return "LanguageItem{" +
                "type=" + this.type +
                ", durability=" + this.durability +
                ", compound=" + this.compound +
                ", translation='" + this.translation + '\'' +
                '}';
    }
}
