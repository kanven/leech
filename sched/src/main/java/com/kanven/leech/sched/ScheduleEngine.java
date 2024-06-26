package com.kanven.leech.sched;

import com.kanven.leech.config.Configuration;
import com.kanven.leech.extension.basic.DefaultExtensionLoader;
import com.kanven.leech.extension.Scope;
import com.kanven.leech.extension.Spi;
import com.kanven.leech.extension.SpiMate;
import com.kanven.leech.sched.executor.Executor;
import com.kanven.leech.sched.strategy.Strategy;
import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.*;

import static com.kanven.leech.config.Configuration.*;

@Slf4j
@Spi(scope = Scope.SINGLETON)
@SpiMate(name = "default")
public class ScheduleEngine<E> implements Closeable {

    private ExecutorService scheduler = Executors.newSingleThreadExecutor(r -> new Thread(r, "Schedule-Engine-Scheduler"));

    private ThreadPoolExecutor pool = createThreadPoolExecutor();

    private Executor<E> executor = executor();

    private volatile boolean init = false;

    public synchronized void start() {
        if (init) {
            return;
        }
        scheduler.submit(() -> {
            while (true) {
                E entry = strategy().pickOut();
                if (entry != null) {
                    this.pool.submit(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                executor.execute(entry);
                            } catch (Exception e) {
                                log.error("the executor has an error", e);
                            }
                        }
                    });
                }
            }
        });
        init = true;
    }

    @Override
    public synchronized void close() throws IOException {
        if (init) {
            scheduler.shutdown();
            pool.shutdown();
            executor.close();
            init = false;
        }
    }

    private ThreadPoolExecutor createThreadPoolExecutor() {
        return new ThreadPoolExecutor(Configuration.getInteger(LEECH_SCHED_EXECUTOR_THREAD_CORE, 2),
                Configuration.getInteger(LEECH_SCHED_EXECUTOR_THREAD_MAX, 2), 0, TimeUnit.MICROSECONDS, new LinkedBlockingQueue<>(20), r -> new Thread(r, "Schedule-Engine-Executor"));

    }

    private Strategy<E> strategy() {
        return (Strategy<E>) DefaultExtensionLoader.load(Strategy.class).getExtension(Configuration.getString(LEECH_SCHED_STRATEGY_NAME, "FIFO"));
    }

    private Executor<E> executor() {
        return (Executor<E>) DefaultExtensionLoader.load(Executor.class).getExtension(Configuration.getString(LEECH_EXECUTOR_MODE, "log"));
    }

}
