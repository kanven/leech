package com.kanven.leech.bulk;

import lombok.Getter;

import java.io.File;

public interface Listener {

    void listen(Content content);

    @Getter
    public class Content {

        private final File file;

        private final String line;

        private final long start;

        private final long end;


        public Content(File file, String line, long start, long end) {
            this.file = file;
            this.line = line;
            this.start = start;
            this.end = end;
        }

    }

}
