package com.gmail.virustotalop.obsidianauctions.region;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@ApiStatus.Internal
public abstract class Region {

    private final String name;
    private final String worldName;

    public Region(@NotNull String name, @NotNull  World world) {
        this.name = name;
        this.worldName = world.getName();
    }

    public String getName() {
        return this.name;
    }

    public String getWorldName() {
        return this.worldName;
    }

    public World getWorld() {
        return Bukkit.getServer().getWorld(this.name);
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(!(o instanceof Region)) return false;
        Region region = (Region) o;
        return Objects.equals(this.name, region.name) &&
                Objects.equals(this.worldName, region.worldName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.name, this.worldName);
    }

    public abstract boolean isWithin(Location location);

}