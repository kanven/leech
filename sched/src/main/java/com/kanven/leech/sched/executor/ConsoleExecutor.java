package com.kanven.leech.sched.executor;

import com.kanven.leech.bulk.Listener;
import com.kanven.leech.extension.SpiMate;
import com.kanven.leech.fetcher.FileEntry;

import java.io.IOException;

@SpiMate(name = "console")
public class ConsoleExecutor implements Executor<FileEntry>, Listener {

    @Override
    public void listen(Content content) {
        System.out.println(content.getLine());
    }

    @Override
    public void execute(FileEntry entry) {
        entry.read(this);
    }

    @Override
    public void close() throws IOException {

    }

}
