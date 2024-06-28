package com.kanven.leech.sched.executor;

import com.kanven.leech.bulk.Listener;
import com.kanven.leech.config.Configuration;
import com.kanven.leech.extension.SpiMate;
import com.kanven.leech.extension.basic.DefaultExtensionLoader;
import com.kanven.leech.fetcher.FileEntry;
import com.kanven.leech.sink.SinkData;
import com.kanven.leech.sink.Sinker;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.kanven.leech.config.Configuration.*;

import java.io.IOException;

@Slf4j
@SpiMate(name = "default")
public class DefaultExecutor implements Executor<FileEntry>, Listener {

    private static final Logger logger = LoggerFactory.getLogger("LOG_EXECUTOR_FILE");

    private final Sinker sinker = DefaultExtensionLoader.load(Sinker.class).getExtension(Configuration.getString(LEECH_SINKER_NAME));

    @Override
    public void execute(FileEntry entry) {
        entry.read(this);
    }

    @Override
    public void listen(Content content) {
        long start = System.currentTimeMillis();
        SinkData.SinkDataBuilder builder = SinkData.SinkDataBuilder.getInstance();
        builder.path(content.getFile().getPath());
        builder.data(content.getLine());
        builder.offset(content.getEnd());
        sinker.sink(builder.build());
        long cost = System.currentTimeMillis() - start;
        logger.info(content.getFile().getPath() + " " + content.getStart() + " " + content.getEnd() + " " + cost);
    }

    @Override
    public void close() throws IOException {
        if (sinker != null) {
            sinker.close();
        }
    }
}
