FROM amazoncorretto:21
WORKDIR /app
# Create a config-repo directory
RUN mkdir -p /app/config-repo
COPY target/*.jar app.jar
EXPOSE 8888

# Set JVM memory settings for local deployment
ENV JAVA_OPTS="-Xms128m -Xmx256m -XX:+UseG1GC -XX:+UseContainerSupport"

ENTRYPOINT java $JAVA_OPTS -jar /app/app.jar
#                -javaagent:/app/opentelemetry-javaagent.jar \
#                -Dotel.traces.exporter=otlp \
#                -Dotel.metrics.exporter=otlp \
#                -Dotel.logs.exporter=otlp \
#                -Dotel.exporter.otlp.endpoint=http://10.118.71.208:4318 \
#                -Dotel.resource.attributes="service.name=configserver,service.version=0.0.1-SNAPSHOT,environment=development" \
#                -jar /app/app.jar
