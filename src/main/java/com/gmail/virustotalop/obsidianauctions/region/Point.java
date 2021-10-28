package com.gmail.virustotalop.obsidianauctions.region;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class Point {

    public static Point create(String coords) {
        if(coords == null) {
            return null;
        } else if(!coords.contains(",")) {
            return null;
        }
        String[] split = coords.split(",");
        if(split.length != 3) {
            return null;
        }
        try {
            int x = Integer.parseInt(split[0]);
            int y = Integer.parseInt(split[1]);
            int z = Integer.parseInt(split[2]);
            return new Point(x, y, z);
        } catch(NumberFormatException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private final int x;
    private final int y;
    private final int z;

    public Point(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public int getZ() {
        return this.z;
    }
}