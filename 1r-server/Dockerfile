FROM amazoncorretto:21
WORKDIR /app
#VOLUME /tmp
#ARG DEPENDENCY=target/dependency
#COPY ${DEPENDENCY}/BOOT-INF/lib /app/lib
#COPY ${DEPENDENCY}/META-INF /app/META-INF
#COPY ${DEPENDENCY}/BOOT-INF/classes /app
#ENTRYPOINT ["java","-cp","app:app/lib/*","io.ontomesh.onerecord.server.OneRecordServerApplication"]

COPY ./tls.crt /usr/local/share/ca-certificates/keycloak.crt

# Import the certificate into the JVM's truststore
RUN keytool -importcert \
    -file /usr/local/share/ca-certificates/keycloak.crt \
    -keystore /usr/lib/jvm/java-21-amazon-corretto/lib/security/cacerts \
    -storepass changeit \
    -noprompt

COPY target/*.jar app.jar

ENTRYPOINT  java -jar /app/app.jar
#                -javaagent:/app/opentelemetry-javaagent.jar \
#                -Dotel.traces.exporter=otlp \
#                -Dotel.metrics.exporter=otlp \
#                -Dotel.logs.exporter=otlp \
#                -Dotel.exporter.otlp.endpoint=http://10.118.71.208:4318 \
#                -Dotel.resource.attributes="service.name=gateway,service.version=0.0.1-SNAPSHOT,environment=development" \
#                -jar /app/app.jar



#keytool -importcert \
#    -file /Users/arun/source/ontomesh/1r-server/tls.crt \
#    -keystore /Users/arun/Library/Java/JavaVirtualMachines/openjdk-21.0.1/Contents/Home/lib/security \
#    -storepass changeit \
#    -noprompt