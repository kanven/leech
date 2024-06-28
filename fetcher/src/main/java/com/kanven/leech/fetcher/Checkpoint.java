package com.kanven.leech.fetcher;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class Checkpoint {

    private static final Logger logger = LoggerFactory.getLogger("CHECKPOINT");

    private static final int CURRENT_VERSION = 1;

    private static final String CHECKPOINT_DIR;

    static {
        CHECKPOINT_DIR = System.getProperty("user.dir") + File.separator + "checkpoint";
        File dir = new File(CHECKPOINT_DIR);
        if (!dir.exists()) {
            if (!dir.mkdir()) {
                throw new Error("the checkpoint dir create fail:" + CHECKPOINT_DIR);
            }
        }
    }

    private static final Comparator<String> comparator = (first, second) -> {
        long f = Long.parseLong(first);
        long s = Long.parseLong(second);
        return (int) (s - f);
    };

    public Map<Integer, Long> read() {
        List<String> names = getFiles();
        if (names.size() > 0) {
            //按文件名升序
            names.sort(comparator);
            Map<Integer, Long> checkpoints = parseContent(names);
            if (checkpoints == null || checkpoints.isEmpty()) {
                log.warn("not find an effective checkpoint file");
            } else {
                return checkpoints;
            }
        }
        return new HashMap<>(0);
    }

    private Map<Integer, Long> parseContent(List<String> names) {
        for (String name : names) {
            Map<Integer, Long> checkpoints = new HashMap<>(0);
            File file = new File(CHECKPOINT_DIR + File.separator + name);
            try {
                InputStream input = new FileInputStream(file);
                try (DataInputStream dis = new DataInputStream(input)) {
                    AtomicInteger offset = new AtomicInteger(0);
                    int version = readInt(dis, offset);
                    if (version != CURRENT_VERSION) {
                        continue;
                    }
                    int len = readInt(dis, offset);
                    if (len <= 0) {
                        continue;
                    }
                    readCheckpoint(dis, offset, checkpoints);
                    if (checkpoints.size() == len) {
                        return checkpoints;
                    }
                }
            } catch (Exception e) {
                log.error("", e);
            }
        }
        return new HashMap<>(0);
    }

    private int readInt(DataInputStream dis, AtomicInteger offset) throws IOException {
        int bytes = dis.available();
        if (bytes > offset.get() && (bytes - offset.get()) >= Integer.BYTES) {
            int data = dis.readInt();
            offset.getAndAdd(Integer.BYTES);
            return data;
        }
        return -1;
    }

    private void readCheckpoint(DataInputStream dis, AtomicInteger offset, Map<Integer, Long> checkpoints) throws IOException {
        int len = Integer.BYTES + Long.BYTES;
        while (dis.available() >= len) {
            checkpoints.put(dis.readInt(), dis.readLong());
        }
    }

    /**
     * CopyOnWriteArrayList add、remove 变更操作采用的都是写时复制（cow），所以get、iterator时是其中的一个快照（snapshot）
     *
     * @param entries
     */
    public void refresh(CopyOnWriteArrayList<FileEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return;
        }
        int len = entries.size();
        long timestamp = System.currentTimeMillis();
        String path = CHECKPOINT_DIR + File.separator + timestamp;
        File file = new File(path);
        try {
            if (file.createNewFile()) {
                OutputStream output = new FileOutputStream(file);
                try (DataOutputStream dos = new DataOutputStream(output)) {
                    dos.writeInt(CURRENT_VERSION);
                    dos.writeInt(len);
                    for (FileEntry entry : entries) {
                        dos.writeInt(entry.fid());
                        dos.writeLong(entry.offset());
                    }
                    dos.flush();
                    deleteFiles(file.getName());
                }
            } else {
                log.error("create checkpoint file fail:" + path);
            }
        } catch (Exception e) {
            log.error("write data to checkpoint has an error:" + path, e);
        }
        logger.info("Checkpoint refresh cost:" + (System.currentTimeMillis() - timestamp) + " ms,the entry size is:" + len);
    }

    private List<String> getFiles() {
        List<String> names = new ArrayList<>(0);
        File dir = new File(CHECKPOINT_DIR);
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null && files.length > 0) {
                for (File file : files) {
                    if (file.isFile()) {
                        String name = file.getName();
                        try {
                            Long.parseLong(name);
                            names.add(name);
                        } catch (Exception e) {
                        }
                    }
                }
            }
        }
        return names;
    }

    private void deleteFiles(String exclude) {
        List<String> names = getFiles();
        if (names.size() > 0) {
            names.sort(comparator);
            int index = names.indexOf(exclude);
            for (int i = index + 1; i < names.size(); i++) {
                String name = names.get(i);
                File file = new File(CHECKPOINT_DIR + File.separator + name);
                file.deleteOnExit();
            }
        }
    }

}
