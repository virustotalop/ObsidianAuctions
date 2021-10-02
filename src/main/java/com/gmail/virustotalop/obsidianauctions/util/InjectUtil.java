package com.gmail.virustotalop.obsidianauctions.util;

import com.google.inject.Injector;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@ApiStatus.Internal
@ApiStatus.NonExtendable
public final class InjectUtil {

    public static <T> List<T> collect(@NotNull Class<T> superClazz, @NotNull Injector injector) {
        List<T> bindings = new ArrayList<>();
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
