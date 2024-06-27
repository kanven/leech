package com.kanven.leech.bulk;

import com.kanven.leech.extension.SpiMate;

import java.io.ByteArrayOutputStream;
import java.io.File;

@SpiMate(name = "random")
public class FileRandomBulkReader extends BulkReader {

    public FileRandomBulkReader(File file, String charset, Long offset) throws Exception {
        super(file, charset, offset);
    }

    @Override
    public void read(Listener listener) throws Exception {
        this.size = this.raf.length();//size大小不准
        if (this.offset + 1 == this.size) {
            return;
        }
        this.raf.seek(this.offset);
        //注意未成line时文件offset处理
        try (ByteArrayOutputStream line = new ByteArrayOutputStream()) {
            while (this.offset < this.size) {
                int len;
                byte[] buffer = new byte[1024];
                while ((len = this.raf.read(buffer)) > 0) {
                    boolean seenCR = false;
                    for (int i = 0; i < len; i++) {
                        byte b = buffer[i];
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
                                }
                                line.write(b);
                                break;
                        }
                    }
                }
            }
        }
    }

}
