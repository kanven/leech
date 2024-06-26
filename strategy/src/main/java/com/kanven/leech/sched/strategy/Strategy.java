package com.kanven.leech.sched.strategy;

import com.kanven.leech.extension.Scope;
import com.kanven.leech.extension.Spi;

@Spi(scope = Scope.SINGLETON)
public interface Strategy<E> {

    void add(E entry);

    E pickOut();

}
