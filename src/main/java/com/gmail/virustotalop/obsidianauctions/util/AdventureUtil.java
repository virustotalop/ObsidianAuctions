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