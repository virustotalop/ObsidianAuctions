/*
 *     ObsidianAuctions
 *     Copyright (C) 2012-2022 flobi and contributors
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.gmail.virustotalop.obsidianauctions.region;

import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;

@ApiStatus.Internal
public class Point {

    public static Point create(String coords) {
        if (coords == null) {
            return null;
        } else if (!coords.contains(",")) {
            return null;
        }
        String[] split = coords.split(",");
        if (split.length != 3) {
            return null;
        }
        try {
            int x = Integer.parseInt(split[0]);
            int y = Integer.parseInt(split[1]);
            int z = Integer.parseInt(split[2]);
            return new Point(x, y, z);
        } catch (NumberFormatException ex) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof Point)) {
            return false;
        }
        Point that = (Point) o;
        return this.x == that.x && this.y == that.y && this.z == that.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.x, this.y, this.z);
    }
}