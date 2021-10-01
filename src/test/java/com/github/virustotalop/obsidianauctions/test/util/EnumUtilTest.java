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