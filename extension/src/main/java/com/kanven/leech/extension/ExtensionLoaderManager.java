package com.kanven.leech.extension;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class ExtensionLoaderManager {

    private static final ConcurrentMap<Class<?>, ExtensionLoader<?>> loaders = new ConcurrentHashMap<>();

    public static <T> void put(Class<T> type, ExtensionLoader<T> loader) {
        loaders.putIfAbsent(type, loader);
    }

    public static <T> ExtensionLoader<T> get(Class<T> type) {
        return (ExtensionLoader<T>) loaders.get(type);
    }

}
