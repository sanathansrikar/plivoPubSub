# -------- BUILD STAGE --------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /build

# copy project sources and any libs you placed in libs/
COPY . .

# compile all java sources except tests, using jars from libs/ on the classpath
RUN mkdir -p out \
 && find . -name '*.java' -not -path './tests/*' -print > /tmp/sources.txt \
 && javac -cp "libs/*" -d out @/tmp/sources.txt

# -------- RUNTIME STAGE --------
FROM eclipse-temurin:21-jre
WORKDIR /app

# copy compiled classes and libs
COPY --from=build /build/out .
COPY --from=build /build/libs ./libs

EXPOSE 8000 8080

# run the server; classpath includes compiled classes and all jars in libs/
CMD ["java", "-cp", ":libs/*", "pubsub.PubSubServer"]