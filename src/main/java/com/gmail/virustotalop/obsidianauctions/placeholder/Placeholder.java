package com.gmail.virustotalop.obsidianauctions.placeholder;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public interface Placeholder {

    String replace(Player player, String message);

}