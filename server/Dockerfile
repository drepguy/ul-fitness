# Multi-stage: baut :server via Gradle Wrapper (braucht shared + root)
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /build
COPY gradle ./gradle
COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle
COPY shared ./shared
COPY server ./server
COPY app ./app
RUN chmod +x gradlew && ./gradlew :server:installDist --no-daemon -x test

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /build/server/build/install/server /app
EXPOSE 8080
ENV PORT=8080
ENTRYPOINT ["/app/bin/server"]
