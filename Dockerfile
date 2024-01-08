FROM eclipse-temurin:21-jammy as builder

ARG GH_USERNAME
ARG GH_TOKEN
ENV GH_USERNAME=$GH_USERNAME
ENV GH_TOKEN=$GH_TOKEN
COPY . /opt/app
RUN /opt/app/gradlew -p /opt/app/ assemble

FROM eclipse-temurin:21-jammy
RUN useradd -rm -d /home/app -s /bin/bash -g root -G sudo -u 1001 app

WORKDIR /home/app
COPY --from=builder /opt/app/tools/run.sh /home/app
COPY --from=builder /opt/app/build/libs/code-inventory-system-*-plain.jar /home/app/app-plain.jar
COPY --from=builder /opt/app/build/libs/code-inventory-system-*.jar /home/app/app.jar
RUN chown app:root /home/app/*.jar \
    && chmod 755 /home/app/*.jar

ENV WAIT_VERSION 2.7.2
ADD https://github.com/ufoscout/docker-compose-wait/releases/download/$WAIT_VERSION/wait /home/app/wait
RUN chown app:root /home/app/wait \
    && chmod 755 /home/app/wait

# switch to non-root user
USER app
EXPOSE 8080
ENTRYPOINT /home/app/wait && /home/app/run.sh
