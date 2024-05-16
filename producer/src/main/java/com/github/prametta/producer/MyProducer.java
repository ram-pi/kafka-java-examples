package com.github.prametta.producer;

import com.github.javafaker.Faker;
import io.confluent.common.utils.Utils;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.producer.*;

import java.util.Properties;
import java.util.concurrent.*;

@Log4j2
public class MyProducer implements Callback, Runnable {

    public static void main(String[] args) {
//        new Thread(new MyProducer()).start();
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(new MyProducer(), 0, 60, TimeUnit.SECONDS);
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
        log.info("Message sent to topic: {}, on partition: {}, with offset: {}", recordMetadata.topic(), recordMetadata.partition(), recordMetadata.offset());
    }

    @Override
    @SneakyThrows
    public void run() {
        log.info("MyProducer Running!");
        String topic = "quotes";
        Faker faker = new Faker();

        // set properties
        Properties props = Utils.loadProps("client.properties");
        props.setProperty(ProducerConfig.LINGER_MS_CONFIG, String.valueOf(0));

        // print properties
        log.info("Properties: {}", props);

        // create the producer
        KafkaProducer<String, String> producer = new KafkaProducer<>(props);

        // send data - asynchronous
        for (int i = 0; i < 100; i++) {
            producer.send(new ProducerRecord<>(topic, faker.gameOfThrones().house(), faker.gameOfThrones().quote()), this).get(30, TimeUnit.SECONDS);
        }

        // flush data - synchronous
        producer.flush();

        // flush and close producer
        producer.close();
    }
}
