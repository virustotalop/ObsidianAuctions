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

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class AdventureUtil {

    private static final char SECTION_CODE = '\u00A7';
    private static final LegacyComponentSerializer SECTION = LegacyComponentSerializer.builder()
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .character(SECTION_CODE)
            .build();

    public static String miniToLegacy(String message) {
        Component component = MiniMessage.miniMessage().deserialize(message);
        return SECTION.serialize(component);
    }

    private AdventureUtil() {
    }
}