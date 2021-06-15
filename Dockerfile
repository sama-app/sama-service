FROM amazoncorretto:16-alpine

ENV USER=sama
ENV UID=1001
ENV GID=1001

RUN mkdir /opt/sama && \
    addgroup --gid $GID -S $USER && \
    adduser --uid $UID -S $USER -G $USER

RUN mkdir -p /var/log/sama/sama-service

WORKDIR /home/sama

COPY app/target/service-0.0.1-SNAPSHOT.jar app.jar
RUN chown -R sama:sama /home/sama && \
    chown -R sama:sama /var/log/sama

USER sama

CMD ["sh", "-c", "java $X_JAVA_OPTS -jar app.jar"]

EXPOSE 8080