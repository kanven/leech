package com.kanven.leech.watcher.apache;

import com.kanven.leech.watcher.Event;
import com.kanven.leech.watcher.Watcher;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationObserver;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
public class DirectorListener implements FileAlterationListener {

    private List<Watcher> watchers = new CopyOnWriteArrayList<>();

    public void addWatcher(Watcher watcher) {
        if (watcher != null) {
            this.watchers.add(watcher);
        }
    }

    public void clear() {
        if (watchers.size() > 0) {
            watchers.clear();
        }
    }

    @Override
    public void onStart(FileAlterationObserver observer) {

    }

    @Override
    public void onDirectoryCreate(File directory) {
        notify(directory, Event.EventType.NEW);
    }

    @Override
    public void onDirectoryChange(File directory) {
        notify(directory, Event.EventType.MODIFY);
    }

    @Override
    public void onDirectoryDelete(File directory) {
        notify(directory, Event.EventType.DELETED);
    }

    @Override
    public void onFileCreate(File file) {
        notify(file, Event.EventType.NEW);
    }

    @Override
    public void onFileChange(File file) {
        notify(file, Event.EventType.MODIFY);
    }

    @Override
    public void onFileDelete(File file) {
        notify(file, Event.EventType.DELETED);
    }


    private void notify(File file, Event.EventType type) {
        File parent = file.getParentFile();
        String p = parent.getPath();
        String c = file.getPath().replaceAll(p + File.separator, "");
        Event event = new Event(Event.EventType.DELETED, Paths.get(p), Paths.get(c));
        watchers.forEach(watcher -> {
            try {
                watcher.watcher(event);
            } catch (Exception e) {
                log.error("event watcher occur an error!", e);
            }
        });
    }

    @Override
    public void onStop(FileAlterationObserver observer) {
    }

}
