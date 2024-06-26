package com.kanven.leech.watcher.jdk;


import com.kanven.leech.extension.SpiMate;
import com.kanven.leech.watcher.DirectorWatcher;
import com.kanven.leech.watcher.Event;
import com.kanven.leech.watcher.Watcher;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * <ul>jdk原生WatchService存在一下问题：
 * <li>1、延迟问题，底层PollingWatchService实现采用的是定时任务获取事件;</li>
 * <li>2、不监听子目录下事件</li>
 * </ul>
 *
 * @author kanven
 */
@Slf4j
@SpiMate(name = "native")
public class NativeDirectorWatcher extends DirectorWatcher {

    private final List<Path> paths = new ArrayList<>(0);

    private final Map<Path, WatchService> watchers = new HashMap<>(0);

    private final ExecutorService executor = Executors.newSingleThreadExecutor((r) -> {
        Thread t = new Thread(r, "Thread-Director-Watcher");
        t.setDaemon(true);
        return t;
    });

    private final List<Watcher> handlers = new ArrayList<>(0);


    public NativeDirectorWatcher(String path, boolean recursion) {
        super(path, recursion);
        File file = new File(path);
        this.paths.add(Paths.get(this.path));
        if (recursion) {
            this.paths.addAll(fetch(new ArrayList<File>() {{
                add(file);
            }}));
        }
    }

    private List<Path> fetch(List<File> dirs) {
        if (dirs == null || dirs.isEmpty()) {
            return new ArrayList<>(0);
        }
        List<Path> paths = new ArrayList<>(0);
        List<File> second = new ArrayList<>(0);
        dirs.forEach(file -> {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    if (child.isDirectory()) {
                        paths.add(Paths.get(child.getPath()));
                        second.add(child);
                    }
                }
            }
        });
        paths.addAll(fetch(second));
        return paths;
    }

    @Override
    protected void onStart() {
        paths.forEach(path -> createWatchService(path));
        executor.submit(() -> {
            Iterator<Map.Entry<Path, WatchService>> iterator = this.watchers.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Path, WatchService> entry = iterator.next();
                WatchService watcher = entry.getValue();
                Path path = entry.getKey();
                WatchKey key = watcher.poll();
                if (key != null) {
                    List<WatchEvent<?>> events = key.pollEvents();
                    events.forEach(event -> {
                        if (event.kind() != StandardWatchEventKinds.OVERFLOW) {
                            Event en = new Event();
                            Path child = (Path) event.context();
                            if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                                en.setType(Event.EventType.NEW);
                                Path np = Paths.get(path.toString() + File.separator + child.toString());
                                if (recursion) {
                                    List<Path> paths = fetch(new ArrayList<File>() {{
                                        add(path.toFile());
                                    }});
                                    paths.add(np);
                                    paths.forEach(p -> createWatchService(p));
                                }
                            } else if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
                                en.setType(Event.EventType.DELETED);
                                paths.remove(path);
                                iterator.remove();
                            } else {
                                en.setType(Event.EventType.MODIFY);
                            }
                            en.setParent(path);
                            en.setChild(child);
                            Iterator<Watcher> itr = handlers.iterator();
                            while (itr.hasNext()) {
                                Watcher w = itr.next();
                                try {
                                    w.watcher(en);
                                } catch (Exception e) {
                                    log.error("the handler throw an exception!", e);
                                }
                            }
                        }
                    });
                    if (!key.reset()) {
                        createWatchService(path);
                    }
                }
            }
        });
    }

    @Override
    protected void onListen(Watcher watcher) {
        this.handlers.add(watcher);
    }

    @Override
    protected void onClose() {
        this.watchers.forEach((path, watcher) -> {
            try {
                watcher.close();
            } catch (Exception e) {
                log.error(path.toString() + "'s watcher close action occur an exception!", e);
            }
        });
        watchers.clear();
        paths.clear();
        if (!executor.isShutdown()) {
            executor.shutdown();
        }
    }

    private void createWatchService(Path path) {
        try {
            WatchService watcher = FileSystems.getDefault().newWatchService();
            path.register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);
            watchers.put(path, watcher);
        } catch (IOException e) {
            log.error(path.toString() + " create watcher occur an exception!", e);
        }
    }

}
