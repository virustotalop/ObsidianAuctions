package com.gmail.virustotalop.obsidianauctions.arena.region;

import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class GlobalRegion extends Region {

    public GlobalRegion(String name, World world) {
        super(name, world);
    }

    @Override
    public boolean isWithin(Location location) {
        return location.getWorld().getName().equals(this.getWorld().getName());
    }
}