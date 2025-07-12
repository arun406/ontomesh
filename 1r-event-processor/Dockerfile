FROM amazoncorretto:21
WORKDIR /app
#VOLUME /tmp
#ARG DEPENDENCY=target/dependency
#COPY ${DEPENDENCY}/BOOT-INF/lib /app/lib
#COPY ${DEPENDENCY}/META-INF /app/META-INF
#COPY ${DEPENDENCY}/BOOT-INF/classes /app
#ENTRYPOINT ["java","-cp","app:app/lib/*","io.ontomesh.onerecord.server.OneRecordServerApplication"]

# Copy certificate initialization script
COPY ./init-certs.sh /app/init-certs.sh
RUN chmod +x /app/init-certs.sh

# Copy the application jar
COPY target/*.jar app.jar

# Set JVM memory settings for local deployment
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:+UseContainerSupport"

# Create entrypoint script that uses JAVA_OPTS
RUN echo '#!/bin/bash' > /app/entrypoint.sh && \
    echo 'set -e' >> /app/entrypoint.sh && \
    echo 'echo "Starting application..."' >> /app/entrypoint.sh && \
    echo 'exec java $JAVA_OPTS -jar /app/app.jar' >> /app/entrypoint.sh && \
    chmod +x /app/entrypoint.sh

ENTRYPOINT ["/app/entrypoint.sh"]
#                -javaagent:/app/opentelemetry-javaagent.jar \
#                -Dotel.traces.exporter=otlp \
#                -Dotel.metrics.exporter=otlp \
#                -Dotel.logs.exporter=otlp \
#                -Dotel.exporter.otlp.endpoint=http://10.118.71.208:4318 \
#                -Dotel.resource.attributes="service.name=gateway,service.version=0.0.1-SNAPSHOT,environment=development" \
#                -jar /app/app.jar