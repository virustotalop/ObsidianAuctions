package com.gmail.virustotalop.obsidianauctions.region;

import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@ApiStatus.Internal
public abstract class Region {

    private final String name;
    private final World world;

    public Region(@NotNull String name, @NotNull  World world) {
        this.name = name;
        this.world = world;
    }

    public String getName() {
        return this.name;
    }

    public World getWorld() {
        return this.world;
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(!(o instanceof Region)) return false;
        Region that = (Region) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(world.getName(), that.world.getName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, world);
    }

    public abstract boolean isWithin(Location location);

}