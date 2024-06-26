package com.kanven.leech.sched.executor;

import com.kanven.leech.extension.Scope;
import com.kanven.leech.extension.Spi;

import java.io.Closeable;

@Spi(scope = Scope.SINGLETON)
public interface Executor<E> extends Closeable {

    void execute(E entry);

}
