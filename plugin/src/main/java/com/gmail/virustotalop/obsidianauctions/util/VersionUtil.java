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

package com.gmail.virustotalop.obsidianauctions.util;

import org.bukkit.Bukkit;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public final class VersionUtil {

    private static final String VERSION;

    static {
        if (Bukkit.getServer() == null) {
            VERSION = null;
        } else {
            String name = Bukkit.getServer().getClass().getPackage().getName();
            VERSION = name.substring(name.lastIndexOf('.') + 1);
        }
    }

    public synchronized static String getVersion() {
        return VERSION;
    }

    private VersionUtil() {
    }
}