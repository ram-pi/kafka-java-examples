package com.github.prametta.producer;

import com.github.javafaker.Faker;
import com.github.prametta.model.ZeldaOuterClass.Zelda;
import io.confluent.common.utils.Utils;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.producer.*;

import java.util.Properties;
import java.util.concurrent.*;

@Log4j2
public class MyZeldaProducer implements Callback, Runnable {

    public static void main(String[] args) {
        // new Thread(new MyZeldaProducer()).start();
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(new MyZeldaProducer(), 0, 60, TimeUnit.SECONDS);
        try {
            future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        } finally {
            scheduler.shutdown();
        }
    }

    @Override
    public void onCompletion(RecordMetadata recordMetadata, Exception e) {
        if (e != null) {
            log.error("Unable to send the message: {}", e.getMessage());
            return;
        }
        log.info("Message sent to topic: {}, on partition: {}, with offset: {}", recordMetadata.topic(),
                recordMetadata.partition(), recordMetadata.offset());
    }

    @Override
    @SneakyThrows
    public void run() {
        log.info("MyZeldaProducer Running!");
        String topic = "zelda";
        Faker faker = new Faker();

        // set properties
        Properties props = Utils.loadProps("client.properties");
        props.setProperty(ProducerConfig.LINGER_MS_CONFIG, String.valueOf(0));

        // define serializer
        props.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                "io.confluent.kafka.serializers.protobuf.KafkaProtobufSerializer");

        // print properties
        log.info("Properties: {}", props);

        // create the producer
        KafkaProducer<String, Zelda> producer = new KafkaProducer<>(props);

        // send data - asynchronous
        for (int i = 0; i < 100; i++) {
            Zelda z = Zelda.newBuilder()
                    .setCharacter(faker.zelda().character())
                    .setGame(faker.zelda().game())
                    .build();
            producer.send(new ProducerRecord<>(topic, z.getGame(), z), this);
        }

        // flush data - synchronous
        producer.flush();

        // flush and close producer
        producer.close();
    }
}