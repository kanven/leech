package com.kanven.leech.extension.basic;


import com.kanven.leech.extension.AbstractExtensionLoader;
import com.kanven.leech.extension.ExtensionLoader;
import com.kanven.leech.extension.ExtensionLoaderManager;
import com.kanven.leech.extension.Scope;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class DefaultExtensionLoader<T> extends AbstractExtensionLoader<T> {

    private final ServiceLoader<T> serviceLoader;

    private ReentrantLock lock = new ReentrantLock();

    private DefaultExtensionLoader(Class<T> type, ClassLoader loader) {
        super(type, loader);
        this.serviceLoader = new ServiceLoader<>(type);
        try {
            this.serviceLoader.loadExtension(loader);
        } catch (Exception e) {
            throw new Error("load " + type.getName() + " extension has an error", e);
        }
    }

    @Override
    protected T doInit(String name, List<?> params) {
        try {
            Scope scope = ServiceLoader.getScope(this.type);
            if (scope == Scope.PROTOTYPE) {
                return createInstance(name, params);
            }
            lock.lock(); // 防止单例被多次实例化
            T instance = extensions.get(name);
            if (instance != null) {
                return instance;
            }
            try {
                instance = createInstance(name, params);
                extensions.put(name, instance);
                return instance;
            } finally {
                lock.unlock();
            }
        } catch (Throwable e) {
            // TODO
            throw new RuntimeException(e);
        }
    }

    public T createInstance(String name, List<?> params) throws Exception {
        Class<T> clazz = serviceLoader.getClass(name);
        if (clazz == null) {
            throw new ClassNotFoundException(name);
        }
        if (params == null || params.isEmpty()) {
            return clazz.newInstance();
        } else {
            Class<?>[] types = new Class<?>[params.size()];
            Object[] pvs = new Object[params.size()];
            for (int i = 0, len = params.size(); i < len; i++) {
                types[i] = params.get(i).getClass();
                pvs[i] = params.get(i);
            }
            Constructor<T> constructor = clazz.getConstructor(types);
            return constructor.newInstance(pvs);
        }
    }


    public static <T> DefaultExtensionLoader<T> load(Class<T> clazz) {
        ExtensionLoader<T> loader = ExtensionLoaderManager.get(clazz);
        if (loader == null) {
            loader = initExtensionLoader(clazz);
        }
        return (DefaultExtensionLoader<T>) loader;
    }

    private static synchronized <T> ExtensionLoader initExtensionLoader(Class<T> type) {
        ExtensionLoader<T> loader = ExtensionLoaderManager.get(type);
        if (loader == null) {
            loader = new DefaultExtensionLoader<>(type, Thread.currentThread().getContextClassLoader());
            ExtensionLoaderManager.put(type, loader);
        }
        return loader;
    }

}
