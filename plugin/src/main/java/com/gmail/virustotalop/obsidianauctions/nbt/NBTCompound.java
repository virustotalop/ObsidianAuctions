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

import com.gmail.virustotalop.obsidianauctions.util.ReflectionUtil;
import com.gmail.virustotalop.obsidianauctions.util.VersionUtil;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;

public class NBTCompound {

    private static final String VERSION = VersionUtil.getVersion();
    private static final Class<?> NMS_ITEM_STACK_CLASS = ReflectionUtil.getClassIfExists(
            "net.minecraft.world.item.ItemStack",
            "net.minecraft.server." + VERSION + ".ItemStack"
    );
    private static final Class<?> CRAFT_ITEM_STACK_CLASS = ReflectionUtil.getClassIfExists(
            "org.bukkit.craftbukkit." + VERSION + ".inventory.CraftItemStack"
    );
    private static final Class<?> COMPOUND_CLASS = ReflectionUtil.getClassIfExists(
            "net.minecraft.nbt.NBTTagCompound",
            "net.minecraft.server." + VERSION + ".NBTTagCompound",
            "net.minecraft.nbt.CompoundTag"
    );
    private static final Class<?> PARSER_CLASS = ReflectionUtil.getClassIfExists(
            "net.minecraft.nbt.MojangsonParser",
            "net.minecraft.server." + VERSION + ".MojangsonParser",
            "net.minecraft.nbt.TagParser"
    );
    private static final Class<?> TAG_CLASS = ReflectionUtil.getClassIfExists(
            "net.minecraft.nbt.NBTBase",
            "net.minecraft.server." + VERSION + ".NBTBase",
            "net.minecraft.nbt.Tag"
    );

    private static final Method PARSE = ReflectionUtil.getStaticMethod(PARSER_CLASS, COMPOUND_CLASS);
    private static final Method GET_KEYS = ReflectionUtil.getMethodByReturnType(COMPOUND_CLASS, Set.class);
    private static final Method GET = ReflectionUtil.getMethodByReturnType(COMPOUND_CLASS, TAG_CLASS);
    private static final Method AS_CRAFT_COPY = ReflectionUtil.getStaticMethod(
            CRAFT_ITEM_STACK_CLASS,
            CRAFT_ITEM_STACK_CLASS,
            ItemStack.class
    );
    private static final Method AS_NMS_COPY = ReflectionUtil.getStaticMethod(
            CRAFT_ITEM_STACK_CLASS,
            NMS_ITEM_STACK_CLASS,
            ItemStack.class
    );
    private static final Method GET_COMPOUND_TAG = ReflectionUtil.getMethodByReturnType(
            NMS_ITEM_STACK_CLASS,
            COMPOUND_CLASS,
            new Class<?>[0]
    );

    public static boolean isCompound(Object compound) {
        if (compound == null) {
            return false;
        }
        return compound.getClass().equals(COMPOUND_CLASS);
    }

    public static boolean fuzzyMatches(NBTCompound compare, NBTCompound compound) {
        for (String key : compare.getKeys()) {
            Object get = compound.get(key);
            if (get == null) {
                return false;
            } else if (isCompound(get)) {
                Object compareCompound = compare.get(key);
                if (!isCompound(compareCompound)) {
                    return false;
                }
                return fuzzyMatches(new NBTCompound(compareCompound), new NBTCompound(get));
            } else if (!compare.get(key).equals(compound.get(key))) {
                return false;
            }
        }
        return true;
    }

    private final Object inner;

    public NBTCompound(Object compound) {
        this.inner = isCompound(compound) ? compound : null;
    }

    public NBTCompound(String json) throws Exception {
        this.inner = this.parseNBTCompoundFromJson(json);
    }

    public NBTCompound(ItemStack itemStack) {
        this.inner = this.retrieveNBTCompoundFromItem(itemStack);
    }

    public Object getNMSCompound() {
        return this.inner;
    }

    public Set<String> getKeys() {
        try {
            return (Set<String>) GET_KEYS.invoke(this.inner);
        } catch (InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean hasKey(String key) {
        return this.getKeys().contains(key);
    }

    public Object get(String key) {
        try {
            if (!this.hasKey(key)) {
                return null;
            }
            return GET.invoke(this.inner, key);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Object parseNBTCompoundFromJson(String json) throws Exception {
        return PARSE.invoke(null, json);
    }

    private Object retrieveNBTCompoundFromItem(ItemStack itemStack) {
        try {
            Object craftCopy = AS_CRAFT_COPY.invoke(null, itemStack);
            Object nmsStack = AS_NMS_COPY.invoke(null, craftCopy);
            return GET_COMPOUND_TAG.invoke(nmsStack);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String toString() {
        return "NBTCompound{" +
                "inner=" + this.inner +
                '}';
    }
}