package com.github.prametta.producer;

import com.github.javafaker.Faker;
import com.github.jcustenborder.kafka.serialization.jackson.JacksonSerializer;
import io.confluent.common.utils.Utils;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.LongSerializer;

import java.util.Properties;
import java.util.concurrent.*;

@Log4j2
public class SimpleProducerJsonValue implements Callback, Runnable {

    public static Integer NUM_ITERATIONS = 1;

    public static void main(String[] args) {
//        new Thread(new SimpleProducer()).start();
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(new SimpleProducerJsonValue(), 0, 60, TimeUnit.SECONDS);
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
        log.info("Message sent to topic: {}, on partition: {}, with offset: {}. Serialized Key: {}", recordMetadata.topic(), recordMetadata.partition(), recordMetadata.offset(), recordMetadata.serializedKeySize());
    }

    @Override
    @SneakyThrows
    public void run() {
        log.info("SimpleProducer Running!");
        String topic = "users";
        Faker faker = new Faker();

        // set properties
        Properties props = Utils.loadProps("client.properties");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class.getName());
        props.setProperty(ProducerConfig.LINGER_MS_CONFIG, String.valueOf(0));
        props.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonSerializer.class.getName());

        // print properties
        log.info("Properties: {}", props);

        // create the producer
        KafkaProducer<Long, User> producer = new KafkaProducer<>(props);

        // send data - asynchronous
        for (int i = 0; i < NUM_ITERATIONS; i++) {
            Long key = faker.number().numberBetween(1L, 100L);
            // JSON
            User user = new User();
            user.setName(faker.funnyName().name());

            log.info("Sending message: {}, with Key: {}", user.toString(), key);
            producer.send(new ProducerRecord<>(topic, key, user), this);
        }

        // flush data - synchronous
        producer.flush();

        // flush and close producer
        producer.close();
    }

    @Getter
    @Setter
    private class User {
        public String name;
    }
}