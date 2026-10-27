# ---- build stage: compile the uberjar ----
FROM clojure:temurin-17-lein AS build
WORKDIR /build
# copy project.clj first so dependencies are cached until it changes
COPY project.clj .
RUN lein deps
COPY src ./src
COPY resources ./resources
RUN lein uberjar

# ---- runtime stage: small JRE image ----
FROM eclipse-temurin:17-jre
RUN useradd --system --create-home app
WORKDIR /app
COPY --from=build /build/target/shopping-cart-demo.jar app.jar
# the app writes bulk-upload logs to ./logs
RUN mkdir -p /app/logs && chown -R app:app /app
USER app
ENV PORT=3030
EXPOSE 3030
CMD ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
