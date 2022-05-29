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
