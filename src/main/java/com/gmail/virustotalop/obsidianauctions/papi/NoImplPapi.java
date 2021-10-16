package com.gmail.virustotalop.obsidianauctions.papi;

import org.bukkit.entity.Player;

public class NoImplPapi implements PlaceholderAPI {

    @Override
    public String setPlaceHolders(Player player, String message) {
        return message;
    }
}
