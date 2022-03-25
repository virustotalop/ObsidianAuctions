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

package com.github.virustotalop.obsidianauctions.test.util;

import com.gmail.virustotalop.obsidianauctions.util.EnumUtil;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;

public class EnumUtilTest {

    @Test
    public void testFormatNameWithEnum() {
        Material diamondSword = Material.DIAMOND_SWORD;
        String formatted = EnumUtil.formatName(diamondSword);
        assertEquals("Diamond Sword", formatted);
    }

    @Test
    public void testFormatNameWithString() {
        Material diamondSword = Material.DIAMOND_SWORD;
        String formatted = EnumUtil.formatName(diamondSword.name());
        assertEquals("Diamond Sword", formatted);
    }

    @Test
    public void testFormatNoUnderscores() {
        String formatted = EnumUtil.formatName("LUCK");
        assertEquals("Luck", formatted);
    }
}