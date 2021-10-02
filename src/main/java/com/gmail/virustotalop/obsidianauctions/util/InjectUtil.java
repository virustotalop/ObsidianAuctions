package com.gmail.virustotalop.obsidianauctions.util;

import com.google.inject.Injector;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.Collection;

@ApiStatus.Internal
@ApiStatus.NonExtendable
public final class InjectUtil {

    public static <T> Collection<T> collect(Class<T> superClazz, Injector injector) {
        Collection<T> bindings = new ArrayList<>();
        injector.getAllBindings().values().forEach(binding -> {
            Class<?> bindingClazz = binding.getKey().getTypeLiteral().getRawType();
            if(superClazz.isAssignableFrom(bindingClazz)) {
                bindings.add((T) binding.getProvider().get());
            }
        });
        return bindings;
    }

    private InjectUtil() {
    }
}
