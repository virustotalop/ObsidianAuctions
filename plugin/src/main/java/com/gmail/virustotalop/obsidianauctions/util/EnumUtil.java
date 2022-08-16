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

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
@ApiStatus.NonExtendable
public final class EnumUtil {

    public static String formatName(Enum<?> en) {
        return formatName(en.name());
    }

    public static String formatName(String name) {
        char[] chars = name.toCharArray();
        chars[0] = Character.toUpperCase(chars[0]);
        for (int i = 1; i < chars.length; i++) {
            if (chars[i] == '_') {
                chars[i] = ' ';
                if (i + 1 <= chars.length - 1) { //Even though this shouldn't occur it doesn't hurt to check so we don't go out of bounds
                    chars[i + 1] = Character.toUpperCase(chars[i + 1]);
                }
            } else if (chars[i - 1] != ' ') { //Check to make sure the character before is not a space to make upper case to lower case
                chars[i] = Character.toLowerCase(chars[i]);
            }
        }
        return new String(chars);
    }

    private EnumUtil() {
    }
}