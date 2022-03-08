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