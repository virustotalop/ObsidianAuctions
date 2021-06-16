package com.gmail.virustotalop.obsidianauctions.nbt;

import com.gmail.virustotalop.obsidianauctions.util.VersionUtil;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;

public class NBTCompound {

    private static final String version;

    private static Method parse;
    private static Method getKeys;
    private static Method get;

    static {
        version = VersionUtil.getVersion();
        try {
            Class<?> parser = Class.forName("net.minecraft.server." + version + ".MojangsonParser");
            parse = parser.getDeclaredMethod("parse", String.class);

            Class<?> compound = Class.forName("net.minecraft.server." + version + ".NBTTagCompound");
            for(Method method : compound.getDeclaredMethods()) {
                if(method.getReturnType().equals(Set.class)) {
                    getKeys = method;
                    break;
                }
            }

            get = compound.getDeclaredMethod("get", String.class);

        } catch(ClassNotFoundException | NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    private final Object inner;

    public NBTCompound(String json) throws Exception {
        this.inner = this.parseFromJson(json);
    }

    public NBTCompound(ItemStack itemStack) {
        this.inner = this.retrieveFromItem(itemStack);
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
            return get.invoke(this.inner, key);
        } catch(IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Object parseFromJson(String json) throws Exception {
        return parse.invoke(null, json);
    }

    private Object retrieveFromItem(ItemStack itemStack) {
        Class<?> craftItemStack = null;
        try {
            craftItemStack = Class.forName("org.bukkit.craftbukkit." + version + ".inventory.CraftItemStack");
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
}