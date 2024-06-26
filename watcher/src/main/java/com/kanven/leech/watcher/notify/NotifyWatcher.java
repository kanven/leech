package com.kanven.leech.watcher.notify;

import com.kanven.leech.extension.SpiMate;
import com.kanven.leech.watcher.DirectorWatcher;
import com.kanven.leech.watcher.Event;
import com.kanven.leech.watcher.Watcher;
import lombok.extern.slf4j.Slf4j;
import net.contentobjects.jnotify.JNotify;
import net.contentobjects.jnotify.JNotifyListener;

import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 借助jnotify实时监听文件变化，jnofigy 是java版本对操作系统inofiy机制的应用
 * <ul> jnotify 使用教程
 * <li>1、官网地址 -> https://jnotify.sourceforge.net/</li>
 * <li>2、配置流程</li>
 * <li>2.1 官网下载jnotify包并解压解压，可以看到libjnotify.dll(win)、libjnotify.so(linux)、libjnotify.jnilib（mac）三个动态库</li>
 * <li>2.2 在系统启动参数配置-Djava.library.path=动态库所在目录</li>
 * </ul>
 */
@Slf4j
@SpiMate(name = "notify")
public class NotifyWatcher extends DirectorWatcher {

    private List<Watcher> watchers = new CopyOnWriteArrayList<>();

    private int watchId = -1;

    public NotifyWatcher(String path, Boolean recursion) {
        super(path, recursion);
    }

    @Override
    protected void onStart() {
        try {
            watchId = JNotify.addWatch(path, JNotify.FILE_ANY, recursion, new JNotifyListener() {

                @Override
                public void fileCreated(int watchId, String rootPath, String name) {
                    NotifyWatcher.this.notify(new Event(Event.EventType.NEW, Paths.get(rootPath), Paths.get(name)));
                }

                @Override
                public void fileDeleted(int watchId, String rootPath, String name) {
                    NotifyWatcher.this.notify(new Event(Event.EventType.DELETED, Paths.get(rootPath), Paths.get(name)));
                }

                @Override
                public void fileModified(int watchId, String rootPath, String name) {
                    NotifyWatcher.this.notify(new Event(Event.EventType.MODIFY, Paths.get(rootPath), Paths.get(name)));
                }

                @Override
                public void fileRenamed(int watchId, String rootPath, String oldName, String newName) {
                    NotifyWatcher.this.notify(new Event(Event.EventType.RENAME, Paths.get(rootPath), Paths.get(newName), Paths.get(oldName)));
                }
            });
        } catch (Exception e) {
            log.error("jnotify add watcher has an error!", e);
        }
    }

    private void notify(Event event) {
        watchers.forEach(watcher -> {
            watcher.watcher(event);
        });
    }

    @Override
    protected void onListen(Watcher watcher) {
        if (watcher != null) {
            watchers.add(watcher);
        }
    }

    @Override
    protected void onClose() {
        if (watchId > 0) {
            try {
                JNotify.removeWatch(this.watchId);
            } catch (Exception e) {
                log.error("jnotify remove watcher has an error!", e);
            }
            watchers.clear();
            watchId = -1;
        }
    }

}
