FROM rampi88/kafka-multitool:latest
COPY target/streams-1.0-SNAPSHOT.jar app.jar
CMD ["java", "-cp", "app.jar", "com.github.prametta.streams.MyKafkaBeerStreamApp"]