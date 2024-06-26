package com.kanven.leech.sink;

import com.kanven.leech.extension.Scope;
import com.kanven.leech.extension.Spi;

import java.io.Closeable;

@Spi(scope = Scope.SINGLETON)
public interface Sinker extends Closeable {

    void sink(SinkData data);

}
