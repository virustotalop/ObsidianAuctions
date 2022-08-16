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

package com.github.virustotalop.obsidianauctions.test;

import com.gmail.virustotalop.obsidianauctions.Key;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;


public class KeyTest {

    @Test
    public void duplicateValueTest() throws IllegalAccessException {
        Set<String> values = new HashSet<>();
        for (Field field : Key.class.getDeclaredFields()) {
            if (field.getType().equals(Key.class) && Modifier.isStatic(field.getModifiers())) {
                Key key = (Key) field.get(null);
                assertTrue(values.add(key.toString()), () -> "Duplicate key: " + field.getName());
            }
        }
    }
}