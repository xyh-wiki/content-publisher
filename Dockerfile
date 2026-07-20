FROM eclipse-temurin:17-jre
RUN useradd --system --uid 10001 --home-dir /nonexistent --shell /usr/sbin/nologin publisher
WORKDIR /data/services/content-publisher
COPY publisher-web/target/publisher-web-*.jar app.jar
RUN chown publisher:publisher app.jar
USER publisher
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-Djava.io.tmpdir=/data/tmp/content-publisher", "-jar", "app.jar"]
