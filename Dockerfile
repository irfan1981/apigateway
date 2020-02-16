FROM openjdk:8-jre-alpine
ENV VERTICLE_FILE apiGateway-1.0-SNAPSHOT.jar
ENV VERTICLE_HOME /usr/verticles
ENV VERTX_OPTIONS "-cluster -Dvertx.cacheDirBase=/tmp"
ENV HAZELCAST_OPTIONS -Dhazelcast.max.no.heartbeat.seconds=15 -Dhazelcast.merge.first.run.delay.seconds=15 -Dhazelcast.merge.next.run.delay.seconds=15 -Dhazelcast.max.join.merge.target.seconds=15
EXPOSE 8082
COPY target/$VERTICLE_FILE $VERTICLE_HOME/
WORKDIR $VERTICLE_HOME
ENTRYPOINT ["sh", "-c"]
CMD ["exec java $HAZELCAST_OPTIONS -jar $VERTICLE_FILE $VERTX_OPTIONS"]