package com.kanven.leech.bulk;


import com.kanven.leech.config.Configuration;
import com.kanven.leech.extension.Scope;
import com.kanven.leech.extension.Spi;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;

import static com.kanven.leech.config.Configuration.LEECH_BULK_FETCH_HISTORY;

@Spi(scope = Scope.PROTOTYPE)
public abstract class BulkReader implements Closeable {

    protected final File file;

    final RandomAccessFile raf;

    final Charset charset;

    long offset = 0;

    long size = -1;

    public BulkReader(File file, String charset) throws Exception {
        this.file = file;
        this.raf = new RandomAccessFile(file, "r");
        this.charset = Charset.forName(charset);
        this.size = this.raf.length();
        boolean history = Configuration.getBoolean(LEECH_BULK_FETCH_HISTORY, false);
        if (!history) {
            //TODO 丢失第一次修改内容
            offset = size;
        }
    }


    public long delta() throws Exception {
        return this.raf.length() - offset;
    }

    public void close() throws IOException {
        this.raf.close();
    }

    public abstract void read(Listener listener) throws Exception;

}
