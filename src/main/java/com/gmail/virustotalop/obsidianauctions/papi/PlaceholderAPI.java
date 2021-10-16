package com.gmail.virustotalop.obsidianauctions.papi;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public interface PlaceholderAPI {

    String setPlaceHolders(Player player, String message);

}