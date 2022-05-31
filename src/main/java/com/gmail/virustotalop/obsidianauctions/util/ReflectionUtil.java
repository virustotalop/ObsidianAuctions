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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

public final class ReflectionUtil {

    public static Class<?> getClassIfExists(String... clazzes) {
        for (String className : clazzes) {
            try {
                return Class.forName(className);
            } catch (ClassNotFoundException e) {
            }
        }
        return null;
    }

    public static Method getMethodByReturnType(Class<?> searchIn, Class<?> returnType) {
        for (Method m : searchIn.getDeclaredMethods()) {
            if (m.getReturnType().equals(returnType)) {
                m.setAccessible(true);
                return m;
            }
        }
        return null;
    }

    public static Method getMethodByReturnType(Class<?> searchIn, Class<?> returnType, Class<?>... params) {
        for (Method m : searchIn.getDeclaredMethods()) {
            if (m.getReturnType().equals(returnType) && Arrays.equals(m.getParameterTypes(), params)) {
                m.setAccessible(true);
                return m;
            }
        }
        return null;
    }

    public static Method getStaticMethod(Class<?> searchIn, Class<?> returnType) {
        for (Method m : searchIn.getDeclaredMethods()) {
            if (Modifier.isStatic(m.getModifiers()) && m.getReturnType().equals(returnType)) {
                m.setAccessible(true);
                return m;
            }
        }
        return null;
    }

    public static Method getStaticMethod(Class<?> searchIn, Class<?> returnType, Class<?>... params) {
        for (Method m : searchIn.getDeclaredMethods()) {
            if (Modifier.isStatic(m.getModifiers())
                    && m.getReturnType().equals(returnType)
                    && Arrays.equals(m.getParameterTypes(), params)) {
                m.setAccessible(true);
                return m;
            }
        }
        return null;
    }

    private ReflectionUtil() {
    }
}
