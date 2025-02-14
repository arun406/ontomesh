FROM amazoncorretto:21
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8761

ENTRYPOINT java -jar /app/app.jar
#                -javaagent:/app/opentelemetry-javaagent.jar \
#                -Dotel.traces.exporter=otlp \
#                -Dotel.metrics.exporter=otlp \
#                -Dotel.logs.exporter=otlp \
#                -Dotel.exporter.otlp.endpoint=http://10.118.71.208:4318 \
#                -Dotel.resource.attributes="service.name=service-discovery,service.version=0.0.1-SNAPSHOT,environment=development" \
#                -jar /app/app.jar