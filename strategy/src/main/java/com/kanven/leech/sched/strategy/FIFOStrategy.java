package com.kanven.leech.sched.strategy;

import com.kanven.leech.extension.SpiMate;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
@SpiMate(name = "FIFO")
public class FIFOStrategy<E> implements Strategy<E> {

    private final BlockingQueue<E> pending = new LinkedBlockingQueue<>();

    @Override
    public void add(E entry) {
        try {
            pending.put(entry);
        } catch (Exception e) {
            log.error("the entry put to the queue occur an error", e);
        }
    }

    @Override
    public E pickOut() {
        try {
            return pending.take();
        } catch (Exception e) {
            log.error("", e);
            return null;
        }
    }

}
