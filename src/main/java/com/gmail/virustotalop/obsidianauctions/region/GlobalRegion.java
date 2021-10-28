package com.gmail.virustotalop.obsidianauctions.region;

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
        World world = this.getWorld();
        if(world == null) {
            return false;
        }
        return location.getWorld().getName().equals(world.getName());
    }
}