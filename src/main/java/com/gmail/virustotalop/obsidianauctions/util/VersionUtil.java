package com.gmail.virustotalop.obsidianauctions.util;

import org.bukkit.Bukkit;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public final class VersionUtil {

    private static final String VERSION;

    static {
        if (Bukkit.getServer() == null) {
            VERSION = null;
        } else {
            String name = Bukkit.getServer().getClass().getPackage().getName();
            VERSION = name.substring(name.lastIndexOf('.') + 1);
        }
    }

    public synchronized static String getVersion() {
        return VERSION;
    }

    private VersionUtil() {
    }
}