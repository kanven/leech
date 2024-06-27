package com.kanven.leech.bulk;


import com.kanven.leech.config.Configuration;
import com.kanven.leech.extension.Scope;
import com.kanven.leech.extension.Spi;

import java.io.*;
import java.nio.charset.Charset;

import static com.kanven.leech.config.Configuration.LEECH_BULK_FETCH_HISTORY;

@Spi(scope = Scope.PROTOTYPE)
public abstract class BulkReader implements Closeable {

    final File file;

    final int fid;

    final RandomAccessFile raf;

    final Charset charset;

    volatile long offset = 0;

    long size = -1;

    public BulkReader(File file, String charset, Long offset) throws Exception {
        this.file = file;
        this.fid = file.getCanonicalFile().hashCode();
        this.raf = new RandomAccessFile(file, "r");
        this.charset = Charset.forName(charset);
        this.size = this.raf.length();
        if (offset > 0) {
            this.offset = Math.min(offset, size);
        } else {
            boolean history = Configuration.getBoolean(LEECH_BULK_FETCH_HISTORY, false);
            if (!history) {
                this.offset = size;
            }
        }

    }

    public int fid() {
        return this.fid;
    }

    public long offset() {
        return this.offset;
    }

    public long delta() throws Exception {
        return this.raf.length() - offset;
    }

    public void close() throws IOException {
        this.raf.close();
    }

    public abstract void read(Listener listener) throws Exception;

}
