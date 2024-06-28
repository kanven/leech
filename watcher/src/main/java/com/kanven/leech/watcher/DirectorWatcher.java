package com.kanven.leech.watcher;

import com.kanven.leech.extension.Scope;
import com.kanven.leech.extension.Spi;
import lombok.extern.slf4j.Slf4j;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Spi(scope = Scope.PROTOTYPE)
public abstract class DirectorWatcher implements Closeable {

    private final AtomicInteger state = new AtomicInteger(State.INITED.state);

    private final ReentrantLock lock = new ReentrantLock();

    private final Condition condition = lock.newCondition();

    protected final File file;

    protected final boolean recursion;

    public DirectorWatcher(File file, Boolean recursion) {
        this.file = file;
        this.recursion = recursion;
        if (!file.exists()) {
            throw new RuntimeException(file.getName() + " 不存在");
        }
        if (!file.isDirectory()) {
            throw new RuntimeException(file.getName() + " 不是目录");
        }
    }

    public void start() {
        lock.lock();
        try {
            if (state.get() >= State.STARTING.state) {
                return;
            }
            state.compareAndSet(State.INITED.state, State.STARTING.state);
        } finally {
            lock.unlock();
        }
        this.onStart();
        lock.lock();
        try {
            state.compareAndSet(State.STARTING.state, State.STARTED.state);
            condition.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public void listen(Watcher watcher) {
        if (state.get() <= State.STARTED.state) {
            onListen(watcher);
        }
    }

    @Override
    public void close() throws IOException {
        lock.lock();
        try {
            if (state.get() >= State.STOPPING.state) {
                return;
            }
            if (state.get() == State.STARTING.state) {
                try {
                    condition.await();
                } catch (Exception e) {
                    log.error("lock wait occur an exception", e);
                }
            }
            state.compareAndSet(State.STARTED.state, State.STOPPING.state);
        } finally {
            lock.unlock();
        }
        onClose();
        state.compareAndSet(State.STOPPING.state, State.STOPPED.state);
    }

    protected abstract void onStart();

    protected abstract void onListen(Watcher watcher);

    protected abstract void onClose();

    enum State {
        INITED(0),
        STARTING(1),
        STARTED(2),
        STOPPING(3),
        STOPPED(4);

        private int state;

        State(int state) {
            this.state = state;
        }

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DirectorWatcher that = (DirectorWatcher) o;
        return Objects.equals(file.getPath(), that.file.getPath());
    }

    @Override
    public int hashCode() {
        return Objects.hash(file.getPath());
    }
}
