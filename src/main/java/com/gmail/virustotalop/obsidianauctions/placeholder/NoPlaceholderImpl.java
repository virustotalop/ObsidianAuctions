package com.gmail.virustotalop.obsidianauctions.placeholder;

import org.bukkit.entity.Player;

public class NoPlaceholderImpl implements Placeholder {

    @Override
    public String replace(Player player, String message) {
        return message;
    }
}
