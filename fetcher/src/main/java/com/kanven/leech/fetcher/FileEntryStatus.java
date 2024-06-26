package com.kanven.leech.fetcher;

import lombok.extern.slf4j.Slf4j;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

@Slf4j
class FileEntryStatus {

    private volatile int state = SUSPEND;

    boolean suspend() {
        return compareAndSetState(RUNNING, SUSPEND);
    }

    boolean pending() {
        boolean result = compareAndSetState(SUSPEND, PENDING);
        if (!result) {
            result = compareAndSetState(RUNNING, PENDING);
        }
        return result;
    }

    boolean running() {
        return compareAndSetState(PENDING, RUNNING);
    }

    boolean isSuspend() {
        return state == SUSPEND;
    }

    boolean isPending() {
        return state == PENDING;
    }

    public boolean isRunning() {
        return state == RUNNING;
    }

    private boolean compareAndSetState(int expect, int update) {
        return unsafe.compareAndSwapInt(this, stateOffset, expect, update);
    }

    private static final Unsafe unsafe;

    private static final long stateOffset;

    static {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            unsafe = (Unsafe) field.get(null);
            stateOffset = unsafe.objectFieldOffset
                    (FileEntryStatus.class.getDeclaredField("state"));
        } catch (Exception e) {
            log.error("handler unsafe occur an error", e);
            throw new Error(e);
        }
    }

    /**
     * 挂起态
     */
    private static final int SUSPEND = -1;

    /**
     * 等待态
     */
    private static final int PENDING = 0;

    /**
     * 运行态
     */
    private static final int RUNNING = 1;

    public static void main(String[] args) {
        FileEntryStatus status = new FileEntryStatus();
        System.out.print(status.isSuspend());
    }

}
