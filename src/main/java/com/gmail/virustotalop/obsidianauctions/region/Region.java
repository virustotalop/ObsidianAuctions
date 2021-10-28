package com.gmail.virustotalop.obsidianauctions.region;

import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.Objects;

@ApiStatus.Internal
public abstract class Region {

    private final String name;
    private final WeakReference<World> world;

    public Region(@NotNull String name, @NotNull  World world) {
        this.name = name;
        this.world = new WeakReference<>(world);
    }

    public String getName() {
        return this.name;
    }

    public World getWorld() {
        return this.world.get();
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) {
            return true;
        } else if(!(o instanceof Region)) {
            return false;
        }
        Region that = (Region) o;
        if(this.world.get() == null) {
            if(that.world.get() == null && Objects.equals(this.name, that.name)) {
                return true;
            }
            return false;
        }
        return Objects.equals(this.name, that.name) &&
                Objects.equals(this.world.get().getName(), that.world.get().getName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, world);
    }

    public abstract boolean isWithin(Location location);

}