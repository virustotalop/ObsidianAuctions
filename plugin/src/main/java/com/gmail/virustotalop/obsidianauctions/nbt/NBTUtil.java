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

package com.gmail.virustotalop.obsidianauctions.nbt;

import de.tr7zw.changeme.nbtapi.NBTCompound;
import de.tr7zw.changeme.nbtapi.NBTReflectionUtil;
import de.tr7zw.changeme.nbtapi.NBTType;

import java.util.Arrays;

public final class NBTUtil {

    public static boolean fuzzyMatches(NBTCompound compare, NBTCompound compound) {
        for (String key : compare.getKeys()) {
            boolean hasKey = compound.hasTag(key);
            if (!hasKey) {
                return false;
            }
            if (compound.getType(key).equals(NBTType.NBTTagCompound)) {
                NBTCompound compareCompound = compare.getCompound(key);
                if (compareCompound == null) {
                    return false;
                }
                NBTCompound get = compound.getCompound(key);
                return fuzzyMatches(compareCompound, get);
            }
            NBTType compareType = compare.getType(key);
            NBTType getType = compound.getType(key);
            if (!compareType.equals(getType)) {
                return false;
            }
            Object compareVal = NBTReflectionUtil.getEntry(compare, key);
            Object getVal = NBTReflectionUtil.getEntry(compound, key);
            if (getVal == null || (arraysNotEquals(compareVal, getVal)) || !compareVal.equals(getVal)) {
                return false;
            }
        }
        return true;
    }

    private static boolean arraysNotEquals(Object compareKey, Object getKey) {
        return getKey.getClass().isArray() && !Arrays.equals((Object[]) compareKey, (Object[]) getKey);
    }

    private NBTUtil() {
        throw new UnsupportedOperationException("Cannot instantiate utility class");
    }
}
