package com.gmail.virustotalop.obsidianauctions.nbt;

import com.gmail.virustotalop.obsidianauctions.util.VersionUtil;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;

public class NBTCompound {

    private static final String version;

    private static Class<?> compoundClass;

    private static Method parse;
    private static Method getKeys;
    private static Method get;

    static {
        version = VersionUtil.getVersion();
        try {
            Class<?> parser = Class.forName("net.minecraft.server." + version + ".MojangsonParser");
            parse = parser.getDeclaredMethod("parse", String.class);
            compoundClass = Class.forName("net.minecraft.server." + version + ".NBTTagCompound");
            for(Method method : compoundClass.getDeclaredMethods()) {
                if(method.getReturnType().equals(Set.class)) {
                    getKeys = method;
                    break;
                }
            }
            get = compoundClass.getDeclaredMethod("get", String.class);
        } catch(ClassNotFoundException | NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    public static boolean isCompound(Object compound) {
        if(compound == null) {
            return false;
        }
        return compound.getClass().equals(compoundClass);
    }

    private final Object inner;

    public NBTCompound(Object compound) {
        this.inner = compound;
    }

    public NBTCompound(String json) throws Exception {
        this.inner = this.parseNBTCompoundFromJson(json);
    }

    public NBTCompound(ItemStack itemStack) {
        this.inner = this.retrieveNBTCompoundFromItem(itemStack);
    }

    public Object getInner() {
        return this.inner;
    }

    public Set<String> getKeys() {
        try {
            return (Set<String>) getKeys.invoke(this.inner);
        } catch(InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean hasKey(String key) {
        return this.getKeys().contains(key);
    }

    public Object get(String key) {
        try {
            if(!this.hasKey(key)) {
                return null;
            }
            return get.invoke(this.inner, key);
        } catch(IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean fuzzyMatches(NBTCompound compound) {
        for(String key : this.getKeys()) {
            Object get = this.get(key);
            if(get == null) {
                return false;
            } else if(isCompound(get)) {
                return fuzzyMatches(new NBTCompound(get));
            } else if(!this.get(key).equals(compound.get(key))) {
                return false;
            }
        }
        return true;
    }

    private Object parseNBTCompoundFromJson(String json) throws Exception {
        return parse.invoke(null, json);
    }

    private Object retrieveNBTCompoundFromItem(ItemStack itemStack) {
        try {
            Class<?> craftItemStack = Class.forName("org.bukkit.craftbukkit." + version + ".inventory.CraftItemStack");
            Method asCraftCopy = craftItemStack.getMethod("asCraftCopy", ItemStack.class);
            Method asNMSCopy = craftItemStack.getMethod("asNMSCopy", ItemStack.class);
            Object craftCopy = asCraftCopy.invoke(null, itemStack);
            Object nmsStack = asNMSCopy.invoke(null, craftCopy);
            Method tagField = nmsStack.getClass().getMethod("getTag");
            return tagField.invoke(nmsStack);
        } catch(Exception e) {
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