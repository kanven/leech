package com.kanven.leech.watcher.apache;

import com.kanven.leech.extension.SpiMate;
import com.kanven.leech.watcher.DirectorWatcher;
import com.kanven.leech.watcher.Watcher;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;

import java.io.File;

@Slf4j
@SpiMate(name = "default")
public class ApacheDirectorWatcher extends DirectorWatcher {

    private final FileAlterationMonitor monitor;

    private final DirectorListener listener = new DirectorListener();

    public ApacheDirectorWatcher(String path) {
        this(path, false);
    }

    public ApacheDirectorWatcher(String path, boolean recursion) {
        super(path, recursion);
        this.monitor = new FileAlterationMonitor(1000L);
        FileAlterationObserver observer = new FileAlterationObserver(new File(path));
        observer.addListener(listener);
        this.monitor.addObserver(observer);
    }

    @Override
    protected void onStart() {
        try {
            this.monitor.start();
        } catch (Exception e) {
            log.error("the director watcher start fail", e);
        }
    }

    @Override
    protected void onClose() {
        try {
            this.monitor.stop();
        } catch (Exception e) {
            log.error("the director watcher stop fail", e);
        }
    }

    @Override
    protected void onListen(Watcher watcher) {
        this.listen(watcher);
    }

}
