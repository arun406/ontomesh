# Use Amazon Corretto JDK 21 (LTS)
FROM amazoncorretto:21-alpine

# Set working directory
WORKDIR /app

# Copy the Spring Boot fat JAR
COPY target/*.jar app.jar

# Set default JVM options (can be overridden via JAVA_OPTS)
#ENV JAVA_OPTS="-Xms512m -Xmx1g -XX:+UseG1GC -XX:+UseContainerSupport"

# Expose the application port
EXPOSE 8080

# Use exec form to ensure proper signal handling (graceful shutdown)
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]