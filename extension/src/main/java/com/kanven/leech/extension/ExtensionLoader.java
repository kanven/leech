package com.kanven.leech.extension;

import java.util.List;

public interface ExtensionLoader<T> {

    T getExtension(String name);

    T getExtension(String name, List<Object> params);

}
