package com.kanven.leech.bulk;

import com.kanven.leech.extension.SpiMate;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

@SpiMate(name = "mmp")
public class FileMMPBulkReader extends BulkReader {

    private final FileChannel channel;

    public FileMMPBulkReader(File file, String charset) throws Exception {
        super(file, charset);
        this.channel = this.raf.getChannel();
    }

    public void read(Listener listener) throws Exception {
        this.size = this.channel.size();
        if (offset == this.size) {
            return;
        }
        long delta = offset == 0 ? this.size : this.size - offset;
        int page = (int) (delta / Integer.MAX_VALUE);
        if (delta % Integer.MAX_VALUE != 0) {
            page += 1;
        }
        try (ByteArrayOutputStream line = new ByteArrayOutputStream()) {
            for (long p = 1; p <= page; p++) {
                long pageSize = p * Integer.MAX_VALUE > delta ? delta - (p - 1) * Integer.MAX_VALUE : Integer.MAX_VALUE;
                MappedByteBuffer buffer = this.channel.map(FileChannel.MapMode.READ_ONLY, offset, pageSize);
                int cap = buffer.capacity();
                boolean seenCR = false;
                for (int i = 0; i < cap; i++) {
                    byte b = buffer.get(i);
                    switch (b) {
                        case '\n':
                            seenCR = false;
                            String str = new String(line.toByteArray(), charset);
                            long end = this.offset + line.size();
                            line.reset();
                            listener.listen(new Listener.Content(file, str, this.offset + 1, end));
                            this.offset = end;
                            break;
                        case '\r':
                            if (seenCR) {
                                line.write('\r');
                            }
                            seenCR = true;
                            break;
                        default:
                            if (seenCR) {
                                seenCR = false;
                                str = new String(line.toByteArray(), charset);
                                end = this.offset + line.size();
                                line.reset();
                                listener.listen(new Listener.Content(file, str, this.offset + 1, end));
                                this.offset = end;
                            }
                            line.write(b);
                            break;
                    }
                }
            }
        }
        this.offset += 1;
    }

}
