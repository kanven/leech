package com.kanven.leech.fetcher;

import com.kanven.leech.config.Configuration;
import com.kanven.leech.bulk.BulkReader;
import com.kanven.leech.bulk.Listener;
import com.kanven.leech.extension.basic.DefaultExtensionLoader;
import com.kanven.leech.sched.strategy.Strategy;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import static com.kanven.leech.config.Configuration.*;

@Slf4j
public final class FileEntry {

    private String dir;

    private String name;

    private BulkReader reader;

    private FileEntryStatus status = new FileEntryStatus();

    private final AtomicInteger times = new AtomicInteger();

    FileEntry(String dir, String name, BulkReader reader) {
        this.dir = dir;
        this.name = name;
        this.reader = reader;
    }

    void increment() {
        this.times.incrementAndGet();
        if (status.isSuspend()) {
            if (status.pending()) {
                //加入待调度任务
                buildStrategy().add(this);
            }
        }
    }

    public void read(Listener listener) {
        if (this.times.get() == 0 || !status.running()) {
            return;
        }
        try {
            reader.read(listener);
        } catch (Exception e) {
            log.error(dir + File.separator + name + "'s content read occur an error!", e);
        } finally {
            int times = this.times.decrementAndGet();
            try {
                //增量读取到达文件临时末端，重置times，实现文件有效modify准确控制（存在提前读取其他modify数据的情况）
                if (reader.delta() == 0) {
                    boolean result = this.times.compareAndSet(times, 0);
                    while (!result) {
                        int nt = this.times.get();
                        result = this.times.compareAndSet(nt, nt - times);
                    }
                }
                if (this.times.get() == 0) {
                    status.suspend();
                }
                if (this.times.get() > 0 && status.pending()) {
                    //加入待调度任务
                    buildStrategy().add(this);
                }
            } catch (Exception e) {
                log.error("", e);
            }
        }
    }

    private Strategy<FileEntry> buildStrategy() {
        Strategy<FileEntry> strategy = (Strategy<FileEntry>) DefaultExtensionLoader.load(Strategy.class).getExtension(Configuration.getString(LEECH_SCHED_STRATEGY_NAME, "FIFO"));
        return strategy;
    }

    public void close() {
        if (reader != null) {
            try {
                reader.close();
            } catch (Exception e) {
                log.error("the reader close occur an error", e);
            }
        }
    }

    public int times() {
        return times.get();
    }

    FileEntryStatus status() {
        return this.status;
    }

    String getDir() {
        return dir;
    }

    public String getName() {
        return name;
    }

    public long offset() {
        return reader.offset();
    }

    public int fid() {
        return reader.fid();
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileEntry fileEntry = (FileEntry) o;
        return Objects.equals(dir, fileEntry.dir) &&
                Objects.equals(name, fileEntry.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dir, name);
    }

}
