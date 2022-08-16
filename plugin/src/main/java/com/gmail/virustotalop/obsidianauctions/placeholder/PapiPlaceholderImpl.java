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

package com.gmail.virustotalop.obsidianauctions.placeholder;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

public class PapiPlaceholderImpl implements Placeholder {

    private final MethodHandle setPlaceHolders;

    @Inject
    private PapiPlaceholderImpl() {
        this.setPlaceHolders = this.lookupSetPlaceHolders();
    }

    public String replace(Player player, String message) {
        try {
            return (String) this.setPlaceHolders.invoke(player, message);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        return null;
    }

    private MethodHandle lookupSetPlaceHolders() {
        try {
            Class<?> placeholderAPI = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            Method reflect = placeholderAPI.getDeclaredMethod("setPlaceholders",
                    OfflinePlayer.class, String.class);
            return MethodHandles.lookup().unreflect(reflect);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }
}