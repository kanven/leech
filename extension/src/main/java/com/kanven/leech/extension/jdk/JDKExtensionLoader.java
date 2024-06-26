package com.kanven.leech.extension.jdk;


import com.kanven.leech.extension.AbstractExtensionLoader;
import com.kanven.leech.extension.ExtensionLoader;
import com.kanven.leech.extension.ExtensionLoaderManager;

import java.util.List;
import java.util.ServiceLoader;

public class JDKExtensionLoader<T> extends AbstractExtensionLoader<T> {

    private ServiceLoader<T> loader;

    private JDKExtensionLoader(Class<T> type, ClassLoader loader) {
        super(type, loader);
        this.loader = ServiceLoader.load(type);
    }

    @Override
    protected T doInit(String name, List<?> params) {
        loader.forEach(instance -> {
            String metaName = com.kanven.leech.extension.basic.ServiceLoader.getSpiMeta(instance.getClass());
            if (name.equals(metaName)) {
                extensions.computeIfAbsent(name, key -> instance);
            }
        });
        return extensions.get(name);
    }

    public static <T> JDKExtensionLoader<T> load(Class<T> clazz) {
        ExtensionLoader<T> loader = ExtensionLoaderManager.get(clazz);
        if (loader == null) {
            loader = initExtensionLoader(clazz);
        }
        return (JDKExtensionLoader<T>) loader;
    }

    private static synchronized <T> ExtensionLoader initExtensionLoader(Class<T> type) {
        ExtensionLoader<T> loader = ExtensionLoaderManager.get(type);
        if (loader == null) {
            loader = new JDKExtensionLoader<>(type, Thread.currentThread().getContextClassLoader());
            ExtensionLoaderManager.put(type, loader);
        }
        return loader;
    }

}
