FROM amazoncorretto:16-alpine

RUN mkdir /opt/sama && \
    addgroup -S sama && \
    adduser -S sama -G sama

RUN mkdir -p /var/log/sama/sama-service

WORKDIR /home/sama

COPY target/service-0.0.1-SNAPSHOT.jar app.jar
RUN chown -R sama:sama /home/sama && \
    chown -R sama:sama /var/log/sama

USER sama

CMD ["sh", "-c", "java $X_JAVA_OPTS -jar app.jar"]

EXPOSE 8080