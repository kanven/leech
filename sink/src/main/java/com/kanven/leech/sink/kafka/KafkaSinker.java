package com.kanven.leech.sink.kafka;

import com.alibaba.fastjson2.JSON;
import com.kanven.leech.extension.SpiMate;
import com.kanven.leech.sink.AbstractSinker;
import com.kanven.leech.sink.SinkData;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.producer.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@SpiMate(name = "kafka")
public class KafkaSinker extends AbstractSinker {

    private static final String KAFKA_PROPERTIES = "kafka.properties";

    private static final String KAFKA_TOPIC = "kafka.topic";

    private static Properties properties;

    private Producer<String, String> producer;

    static {
        Class<KafkaSinker> clazz = KafkaSinker.class;
        ClassLoader loader = clazz.getClassLoader();
        InputStream input = ClassLoader.getSystemClassLoader().getResourceAsStream(KAFKA_PROPERTIES);
        if (input == null) {
            input = loader.getResourceAsStream(KAFKA_PROPERTIES);
        }
        if (input == null) {
            input = clazz.getResourceAsStream(KAFKA_PROPERTIES);
        }
        Properties properties = new Properties();
        try {
            properties.load(input);
        } catch (Exception e) {
            throw new Error("the kafka.properties load fail!");
        }
        if (StringUtils.isBlank(properties.getProperty(ProducerConfig.ACKS_CONFIG))) {
            properties.setProperty(ProducerConfig.ACKS_CONFIG, "1");
        }
        properties.setProperty(ProducerConfig.PARTITIONER_CLASS_CONFIG, KeyPartitioner.class.getName());
        KafkaSinker.properties = properties;
    }

    public KafkaSinker() {
        this.producer = new KafkaProducer<>(properties);
    }

    @Override
    public void sink(SinkData data) {
        ProducerRecord<String, String> record = new ProducerRecord<>(properties.getProperty(KAFKA_TOPIC), data.getPath(), JSON.toJSONString(data));
        this.producer.send(record, new Callback() {
            @Override
            public void onCompletion(RecordMetadata meta, Exception e) {
                if (e != null) {
                    sink(data);
                } else {
                    //记录文件offset
                    mark(data);
                }
            }
        });
    }

    @Override
    public void close() throws IOException {
        producer.close();
    }

}
