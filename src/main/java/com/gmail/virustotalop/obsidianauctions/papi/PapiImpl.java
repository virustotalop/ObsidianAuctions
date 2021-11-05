package com.gmail.virustotalop.obsidianauctions.papi;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

public class PapiImpl implements PlaceholderAPI {

    private final MethodHandle setPlaceHolders;

    @Inject
    private PapiImpl() {
        this.setPlaceHolders = this.lookupSetPlaceHolders();
    }

    public String setPlaceHolders(Player player, String message) {
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