package com.kanven.leech.sink;

import com.alibaba.fastjson2.JSON;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

@Getter
public class SinkData {

    private String host;

    private String app;

    private long time = System.currentTimeMillis();

    private String path;

    private long offset = -1;

    private String data;

    private SinkData() {

    }

    public static class SinkDataBuilder {

        private SinkData sinkData = new SinkData();

        public static SinkDataBuilder getInstance() {
            return new SinkDataBuilder();
        }

        public void host(String host) {
            sinkData.host = host;
        }

        public void app(String app) {
            sinkData.app = app;
        }

        public void path(String path) {
            sinkData.path = path;
        }

        public void offset(long offset) {
            sinkData.offset = offset;
        }

        public void data(String data) {
            sinkData.data = data;
        }

        public SinkData build() {
            if (StringUtils.isBlank(sinkData.path)) {
                throw new IllegalArgumentException("the path should not be null");
            }
            if (StringUtils.isBlank(sinkData.data)) {
                throw new IllegalArgumentException("the data should not be null");
            }
            if (sinkData.offset < 0) {
                throw new IllegalArgumentException("the offset should not be null");
            }
            return sinkData;
        }

    }

    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }

}
