package com.kanven.leech.sched.executor;

import com.kanven.leech.bulk.Listener;
import com.kanven.leech.extension.SpiMate;
import com.kanven.leech.extension.basic.DefaultExtensionLoader;
import com.kanven.leech.fetcher.FileEntry;
import com.kanven.leech.sink.SinkData;
import com.kanven.leech.sink.Sinker;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
@SpiMate(name = "log")
public class DefaultExecutor implements Executor<FileEntry>, Listener {

    private final Sinker sinker = DefaultExtensionLoader.load(Sinker.class).getExtension("kafka");

    @Override
    public void execute(FileEntry entry) {
        entry.read(this);
    }

    @Override
    public void listen(Content content) {
        SinkData.SinkDataBuilder builder = SinkData.SinkDataBuilder.getInstance();
        builder.path(content.getFile().getPath());
        builder.data(content.getLine());
        builder.offset(content.getEnd());
        sinker.sink(builder.build());
    }

    @Override
    public void close() throws IOException {
        if (sinker != null) {
            sinker.close();
        }
    }
}
