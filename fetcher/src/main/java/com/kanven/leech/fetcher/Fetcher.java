package com.kanven.leech.fetcher;

import com.kanven.leech.config.Configuration;
import com.kanven.leech.bulk.*;
import com.kanven.leech.extension.basic.DefaultExtensionLoader;
import com.kanven.leech.watcher.DirectorWatcher;
import com.kanven.leech.watcher.Event;
import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import static com.kanven.leech.config.Configuration.*;

@Slf4j
public class Fetcher implements Closeable {

    private final CopyOnWriteArrayList<FileEntry> entries = new CopyOnWriteArrayList<>();

    private final DirectorWatcher watcher;

    public Fetcher() {
        this.watcher = DefaultExtensionLoader.load(DirectorWatcher.class).getExtension(Configuration.getString(LEECH_DIR_WATCHER_NAME, "default"), new ArrayList<Object>() {{
            add(Configuration.getString(LEECH_DIR));
            add(Configuration.getBoolean(LEECH_DIR_WATCHER_RECURSION, false));
        }});
        this.init();
    }

    private void init() {
        watcher.listen(event -> {
            switch (event.getType()) {
                case NEW:
                    String path = event.getParent().toString() + File.separator + event.getChild();
                    File file = new File(path);
                    if (file.isFile()) {
                        entries.add(new FileEntry(event.getParent().toString(), event.getChild().toString(), createBulkReader(file)));
                    }
                    break;
                case RENAME:
                    List<FileEntry> fes = filterEntries(event.getParent().toString(), event.getOld().toString(), entries);
                    fes.forEach(entry -> {
                        entry.setName(event.getChild().toString());
                    });
                    break;
                case MODIFY:
                    path = event.getParent().toString() + File.separator + event.getChild();
                    file = new File(path);
                    if (file.isFile()) {
                        fes = filterEntries(event, entries);
                        if (fes.isEmpty()) {
                            FileEntry entry = new FileEntry(event.getParent().toString(), event.getChild().toString(), createBulkReader(file));
                            entries.add(entry);
                            fes.add(entry);
                        }
                        fes.forEach(entry -> entry.increment());
                    }
                    break;
                case DELETED:
                    filterEntries(event, entries).forEach(entry -> {
                        entries.remove(entry);
                        entry.close();
                    });
                    break;
            }
        });
        watcher.start();
    }


    private List<FileEntry> filterEntries(Event event, List<FileEntry> entries) {
        return entries.stream().filter(entry -> entry.getName().equals(event.getChild().toString())
                && entry.getDir().equals(event.getParent().toString())).collect(Collectors.toList());
    }

    private List<FileEntry> filterEntries(String dir, String name, List<FileEntry> entries) {
        return entries.stream().filter(entry -> entry.getDir().equals(name)
                && entry.getDir().equals(dir)).collect(Collectors.toList());
    }

    private BulkReader createBulkReader(File file) {
        return DefaultExtensionLoader.load(BulkReader.class).getExtension(Configuration.getString(LEECH_BULK_READER_NAME, "random"), new ArrayList<Object>() {{
            add(file);
            add(Configuration.getString(LEECH_CHARSET, "UTF-8"));
        }});
    }

    @Override
    public void close() throws IOException {
        if (watcher != null) {
            watcher.close();
        }
    }
}
