package com.gmail.virustotalop.obsidianauctions.placeholder;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

public class PapiPlaceholderImpl implements Placeholder {

    private final MethodHandle setPlaceHolders;

    @Inject
    private PapiPlaceholderImpl() {
        this.setPlaceHolders = this.lookupSetPlaceHolders();
    }

    public String replace(Player player, String message) {
        try {
            return (String) this.setPlaceHolders.invoke(player, message);
        } catch(Throwable throwable) {
            throwable.printStackTrace();
        }
        return null;
    }

    private MethodHandle lookupSetPlaceHolders() {
        try {
            Class<?> placeholderAPI= Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            Method reflect = placeholderAPI.getDeclaredMethod("setPlaceholders",
                    OfflinePlayer.class, String.class);
            return MethodHandles.lookup().unreflect(reflect);
        } catch(ClassNotFoundException | NoSuchMethodException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }
}