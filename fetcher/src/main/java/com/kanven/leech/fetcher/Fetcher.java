package com.kanven.leech.fetcher;

import com.kanven.leech.config.Configuration;
import com.kanven.leech.bulk.*;
import com.kanven.leech.extension.basic.DefaultExtensionLoader;
import com.kanven.leech.watcher.DirectorWatcher;
import com.kanven.leech.watcher.Event;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.kanven.leech.config.Configuration.*;

@Slf4j
public class Fetcher implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger("LOG_WATCHER_FILE");

    private final CopyOnWriteArrayList<FileEntry> entries = new CopyOnWriteArrayList<>();

    private final List<DirectorWatcher> watchers = new ArrayList<>(0);

    private volatile boolean init = false;

    private final Checkpoint checkpoint = new Checkpoint();

    private final ScheduledExecutorService scheduled = Executors.newScheduledThreadPool(1, r -> new Thread(r, "Checkpoint-Scheduled-Thread"));

    public Fetcher() {
    }

    public synchronized void start() {
        if (!init) {
            Set<Pair<File, Pattern>> pairs = parsePaths();
            for (Pair<File, Pattern> pair : pairs) {
                createWatch(pair);
            }
            scheduled.scheduleAtFixedRate(() -> checkpoint.refresh(entries), 0, Configuration.getInteger(LEECH_CHECKPOINT_FLUSH_INTERVAL, 10000), TimeUnit.MILLISECONDS);
            init = true;
        }
    }

    private void log(Event event, long start) {
        long cost = System.currentTimeMillis() - start;
        logger.info(event.getParent().toString() + File.separator + event.getChild().toString() + " " + event.getType() + " " + cost);
    }

    private void createWatch(Pair<File, Pattern> pair) {
        DirectorWatcher watcher = DefaultExtensionLoader.load(DirectorWatcher.class).getExtension(Configuration.getString(LEECH_DIR_WATCHER_NAME, "default"), new ArrayList<Object>() {{
            add(pair.left);
            add(false);
        }});
        watcher.listen(event -> {
            long start = System.currentTimeMillis();
            boolean matched = pair.right.matcher(event.getChild().toString()).matches();
            if (event.getType() == Event.EventType.RENAME) {
                if (matched) {
                    //the new file's name be matched
                    if (!pair.right.matcher(event.getOld().toString()).matches()) {
                        //add an watch file entry
                        addNewFileEntry(event);
                        log(event, start);
                        return;
                    }
                } else {
                    //the new file's name not be matched but the old file's name be matched
                    if (pair.right.matcher(event.getOld().toString()).matches()) {
                        removeFileEntry(event.getParent().toString(), event.getOld().toString());
                        log(event, start);
                        return;
                    }
                }
            }
            if (!matched) {
                return;
            }
            switch (event.getType()) {
                case NEW:
                    addNewFileEntry(event);
                    break;
                case RENAME:
                    List<FileEntry> fes = filterEntries(event.getParent().toString(), event.getOld().toString());
                    fes.forEach(entry -> {
                        entry.setName(event.getChild().toString());
                    });
                    break;
                case MODIFY:
                    String path = event.getParent().toString() + File.separator + event.getChild();
                    File file = new File(path);
                    if (file.isFile()) {
                        fes = filterEntries(event);
                        if (fes.isEmpty()) {
                            fes.add(addNewFileEntry(event));
                        }
                        fes.forEach(entry -> entry.increment());
                    }
                    break;
                case DELETED:
                    removeFileEntry(event.getParent().toString(), event.getChild().toString());
                    break;
            }
            log(event, start);
        });
        watcher.start();
        this.watchers.add(watcher);
    }

    private FileEntry addNewFileEntry(Event event) {
        String dir = event.getParent().toString();
        if (!dir.endsWith(File.separator)) {
            dir += File.separator;
        }
        String name = event.getChild().toString();
        if (name.startsWith(File.separator)) {
            name = name.substring(1);
        }
        String path = dir + name;
        File file = new File(path);
        if (file.isFile()) {
            FileEntry entry = new FileEntry(event.getParent().toString(), event.getChild().toString(), createBulkReader(file));
            if (entries.contains(entry)) {
                // close the old file entry
                int index = entries.indexOf(entry);
                if (index > 0) {
                    FileEntry old = entries.remove(index);
                    old.close();
                }
            }
            entries.add(entry);
            return entry;
        }
        return null;
    }

    private void removeFileEntry(String dir, String name) {
        filterEntries(dir, name).forEach(entry -> {
            entries.remove(entry);
            entry.close();
        });
    }

    private static class Pair<L, R> {

        private L left;

        private R right;

        public Pair(L left, R right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Pair<?, ?> pair = (Pair<?, ?>) o;
            return Objects.equals(left, pair.left) &&
                    Objects.equals(right, pair.right);
        }

        @Override
        public int hashCode() {
            return Objects.hash(left, right);
        }
    }

    private Set<Pair<File, Pattern>> parsePaths() {
        String paths = Configuration.getString(LEECH_WATCHER_PATH);
        if (StringUtils.isBlank(paths)) {
            throw new IllegalArgumentException("the leech watcher path is null");
        }
        Set<Pair<File, Pattern>> pairs = new HashSet<>(0);
        String[] parts = paths.split(",");
        Map<Integer, Long> offsets = this.checkpoint.read();
        for (String part : parts) {
            part = StringUtils.trim(part);
            if (StringUtils.isBlank(part)) {
                continue;
            }
            int pos = part.lastIndexOf(File.separator);
            String dir = part.substring(0, pos);
            String name = part.substring(pos + 1);
            if (name.contains("*")) {
                int point = name.indexOf(".");
                int star = name.indexOf("*");
                if (name.contains(".") && star < point) {
                    String prefix = name.substring(0, point);
                    prefix = prefix.replace("*", ".+");
                    String reg = prefix + "\\." + name.substring(point + 1);
                    Pattern pattern = Pattern.compile(reg);
                    File parent = new File(dir);
                    pairs.add(new Pair<>(parent, pattern));
                    File[] children = parent.listFiles();
                    if (children != null && children.length > 0) {
                        for (File child : children) {
                            if (child.isDirectory()) {
                                continue;
                            }
                            Matcher matcher = pattern.matcher(child.getName());
                            if (matcher.matches()) {
                                long offset = -1;
                                try {
                                    int fid = child.getCanonicalFile().hashCode();
                                    offset = offsets.getOrDefault(fid, offset);
                                } catch (Exception e) {
                                    log.error("get file id occur an error:" + child.getPath(), e);
                                }
                                FileEntry entry = new FileEntry(parent.getPath(), child.getName(), createBulkReader(child, offset));
                                entries.add(entry);
                            }
                        }
                    }
                }
            } else {
                File file = new File(part);
                if (file.isDirectory()) {
                    pairs.add(new Pair<>(file, Pattern.compile(".+")));
                    File[] children = file.listFiles();
                    if (children != null && children.length > 0) {
                        for (File child : children) {
                            if (child.isFile()) {
                                long offset = -1;
                                try {
                                    int fid = child.getCanonicalFile().hashCode();
                                    offset = offsets.getOrDefault(fid, offset);
                                } catch (Exception e) {
                                    log.error("get file id occur an error:" + child.getPath(), e);
                                }
                                FileEntry entry = new FileEntry(file.getPath(), child.getName(), createBulkReader(child, offset));
                                entries.add(entry);
                            }
                        }
                    }
                } else {
                    long offset = -1;
                    try {
                        int fid = file.getCanonicalFile().hashCode();
                        offset = offsets.getOrDefault(fid, offset);
                    } catch (Exception e) {
                        log.error("get file id occur an error:" + file.getPath(), e);
                    }
                    FileEntry entry = new FileEntry(file.getParent(), file.getName(), createBulkReader(file, offset));
                    entries.add(entry);
                    int point = name.indexOf(".");
                    String reg = name.substring(0, point) + "\\." + name.substring(point + 1);
                    pairs.add(new Pair<>(new File(file.getParent()), Pattern.compile(reg)));
                }
            }
        }
        return pairs;
    }


    private List<FileEntry> filterEntries(Event event) {
        return entries.stream().filter(entry -> entry.getName().equals(event.getChild().toString())
                && entry.getDir().equals(event.getParent().toString())).collect(Collectors.toList());
    }

    private List<FileEntry> filterEntries(String dir, String name) {
        return entries.stream().filter(entry -> entry.getDir().equals(name)
                && entry.getDir().equals(dir)).collect(Collectors.toList());
    }

    private BulkReader createBulkReader(File file) {
        return createBulkReader(file, -1);
    }

    private BulkReader createBulkReader(File file, long offset) {
        return DefaultExtensionLoader.load(BulkReader.class).getExtension(Configuration.getString(LEECH_BULK_READER_NAME, "random"), new ArrayList<Object>() {{
            add(file);
            add(Configuration.getString(LEECH_CHARSET, "UTF-8"));
            add(offset);
        }});
    }

    @Override
    public synchronized void close() throws IOException {
        if (init) {
            watchers.forEach(watcher -> {
                try {
                    watcher.close();
                } catch (Exception e) {
                    log.error("watcher close occur an error", e);
                }
            });
            entries.clear();
            if (!scheduled.isShutdown()) {
                scheduled.shutdown();
            }
            init = false;
        }
    }

}
