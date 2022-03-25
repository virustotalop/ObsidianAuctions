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

import com.gmail.virustotalop.obsidianauctions.nbt.NBTCompound;
import com.gmail.virustotalop.obsidianauctions.util.LegacyUtil;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
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
        if (this.type != itemStack.getType()) {
            return false;
        } else if (this.durability != LegacyUtil.getDurability(itemStack)) {
            return false;
        }
        if (this.compound != null) {
            NBTCompound stackComp = new NBTCompound(itemStack);
            return NBTCompound.fuzzyMatches(this.compound, stackComp);
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
