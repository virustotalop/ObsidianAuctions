package com.gmail.virustotalop.obsidianauctions.arena.region;

import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class CuboidRegion extends Region {
    
    private int minX;
    private int minY;
    private int minZ;
    private int maxX;
    private int maxY;
    private int maxZ;

    public CuboidRegion(String name, World world, Point min, Point max) {
        super(name, world);
        this.maxX = Math.max(min.getX(), max.getX());
        this.maxY = Math.max(min.getY(), max.getY());
        this.maxZ = Math.max(min.getZ(), max.getZ());
        this.minX = Math.min(min.getX(), max.getX());
        this.minY = Math.min(min.getY(), max.getY());
        this.minZ = Math.min(min.getZ(), max.getZ());
    }

    @Override
    public boolean isWithin(Location location) {
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();

        if(x > this.maxX || x < this.minX) {
            return false;
        } else if(y > this.maxY || y < this.minY) {
            return false;
        } else if(z > this.maxZ || z < this.minZ) {
            return false;
        } else return location.getWorld().getName().equals(this.getWorld().getName());
    }
}