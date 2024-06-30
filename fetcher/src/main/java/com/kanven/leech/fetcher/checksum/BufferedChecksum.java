package com.kanven.leech.fetcher.checksum;

import java.util.zip.Checksum;

public class BufferedChecksum implements Checksum {

    private final byte[] buffer;

    private final Checksum checksum;

    private int pos;

    private static final int DEFAULT_BUFFERSIZE = 1 << 8;

    public BufferedChecksum(Checksum checksum) {
        this(checksum, DEFAULT_BUFFERSIZE);
    }

    public BufferedChecksum(Checksum checksum, int bufferSize) {
        this.checksum = checksum;
        this.buffer = new byte[bufferSize];
    }

    public void update(int b) {
        if (pos >= buffer.length) {
            flush();
        }
        buffer[pos++] = (byte) b;
    }

    public void update(byte[] b, int off, int len) {
        if (len > buffer.length) {
            flush();
            checksum.update(b, off, len);
        } else {
            if (len + pos > buffer.length) {
                flush();
            }
            System.arraycopy(b, off, buffer, pos, len);
            pos += len;
        }
    }

    public long getValue() {
        flush();
        return checksum.getValue();
    }

    public void reset() {
        checksum.reset();
        pos = 0;
    }

    private void flush() {
        if (pos > 0) {
            checksum.update(buffer, 0, pos);
        }
        pos = 0;
    }


}
