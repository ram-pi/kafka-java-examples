FROM rampi88/kafka-multitool:latest
COPY target/producer-1.0-SNAPSHOT.jar app.jar
CMD ["java", "-cp", "app.jar", "com.github.prametta.producer.MyBeerProducer"]