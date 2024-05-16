FROM rampi88/kafka-multitool:latest
COPY target/consumer-1.0-SNAPSHOT.jar app.jar
CMD ["java", "-cp", "app.jar", "com.github.prametta.consumer.MyBeerConsumer"]