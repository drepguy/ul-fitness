# P0: built via ./gradlew :server:installDist; Docker copies distribution
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY build/install/server /app
EXPOSE 8080
ENV PORT=8080
ENTRYPOINT ["/app/bin/server"]
