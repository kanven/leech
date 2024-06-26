package com.kanven.leech.extension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractExtensionLoader<T> implements ExtensionLoader<T> {

    protected Class<T> type;

    protected ClassLoader loader;

    protected Map<String, T> extensions = new HashMap<>();

    public AbstractExtensionLoader(Class<T> type, ClassLoader loader) {
        this.type = type;
        this.loader = loader;
    }

    @Override
    public T getExtension(String name) {
        return getExtension(name, null);
    }

    public T getExtension(String name, List<Object> params) {
        T extension = extensions.get(name);
        if (extension != null) {
            return extension;
        }
        return doInit(name, params);
    }

    protected abstract T doInit(String name, List<?> params) ;


}
